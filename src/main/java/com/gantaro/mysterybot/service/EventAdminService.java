package com.gantaro.mysterybot.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.imageio.ImageIO;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.gantaro.mysterybot.entity.MasterRiddle;
import com.gantaro.mysterybot.entity.Player;
import com.gantaro.mysterybot.entity.Riddle;
import com.gantaro.mysterybot.entity.RiddleImage;
import com.gantaro.mysterybot.entity.TeamGroup;
import com.gantaro.mysterybot.repository.MasterRiddleRepository;
import com.gantaro.mysterybot.repository.PlayerRepository;
import com.gantaro.mysterybot.repository.RiddleImageRepository;
import com.gantaro.mysterybot.repository.RiddleRepository;
import com.gantaro.mysterybot.repository.SolvedHistoryRepository;
import com.gantaro.mysterybot.repository.TeamGroupRepository;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;

@Service
@RequiredArgsConstructor
public class EventAdminService {

    private final TeamGroupRepository teamGroupRepository;
    private final RiddleRepository riddleRepository;
    private final PlayerRepository playerRepository;
    private final MasterRiddleRepository masterRiddleRepository;
    private final RiddleImageRepository riddleImageRepository;
    private final SolvedHistoryRepository solvedHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Set<String> RESERVED_GROUP_IDS =
            Set.of("admin", "system", "root", "superadmin", "test");
    private static final long MAX_IMAGE_BYTES = 10L * 1024L * 1024L;
    private static final long MAX_IMAGE_PIXELS = 20_000_000L;
    private static final int MAX_IMAGE_DIMENSION = 10_000;

    // ▼▼▼ ログイン・イベント作成 ▼▼▼
    @Transactional
    public boolean login(String groupId, String password) {
        Optional<TeamGroup> group = teamGroupRepository.findByGroupId(groupId);
        if (group.isEmpty())
            return false;
        String savedPass = group.get().getAdminPass();
        if (savedPass == null)
            return false;
        if (isBCryptHash(savedPass)) {
            return passwordEncoder.matches(password, savedPass);
        }
        if (!savedPass.equals(password)) {
            return false;
        }
        teamGroupRepository.updateAdminPass(groupId, passwordEncoder.encode(password));
        return true;
    }

    @Transactional
    public void createEvent(String groupId, String groupName, String password) {
        if (RESERVED_GROUP_IDS.contains(groupId.toLowerCase())) {
            throw new IllegalArgumentException("そのイベントIDは使用できません");
        }
        if (!groupId.matches("^[a-zA-Z0-9_-]{3,30}$")) {
            throw new IllegalArgumentException("イベントIDは半角英数字・ハイフン・アンダーバーのみ、3〜30文字で入力してください");
        }
        if (teamGroupRepository.findByGroupId(groupId).isPresent()) {
            throw new IllegalArgumentException("そのイベントIDは既に使用されています");
        }
        TeamGroup newGroup = new TeamGroup();
        newGroup.setGroupId(groupId);
        newGroup.setGroupName(groupName);
        newGroup.setAdminPass(passwordEncoder.encode(password));
        newGroup.setIsRandomOrder(false);
        teamGroupRepository.insert(newGroup);
    }

    // ▼▼▼ データ取得系 ▼▼▼
    public TeamGroup getEvent(String groupId) {
        return teamGroupRepository.findByGroupId(groupId)
                .orElseThrow(() -> new IllegalArgumentException("グループが見つかりません: " + groupId));
    }

    public List<Player> getRanking(String groupId) {
        return playerRepository.findRankingByGroup(groupId);
    }

    public List<Riddle> getRiddles(String groupId) {
        return riddleRepository.findAllByGroup(groupId);
    }

    public Riddle getRiddle(Integer id) {
        return riddleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("謎が見つかりません:ID" + id));
    }

    public Riddle getRiddleOwnedBy(Integer id, String groupId) {
        Riddle riddle = getRiddle(id);
        if (!groupId.equals(riddle.getGroupId())) {
            throw new SecurityException("この謎問題へのアクセス権がありません");
        }
        return riddle;
    }

    // ▼▼▼ 更新・削除系 ▼▼▼

    // 謎の登録 (Controllerと引数を合わせました)
    @Transactional
    public void registerRiddle(String groupId, String question, String answer, String nextMsg,
            Integer imageId, String hintMsg) {
        Integer nextStage = riddleRepository.countByGroup(groupId) + 1;
        Riddle newRiddle = new Riddle();
        newRiddle.setGroupId(groupId);
        newRiddle.setStageNo(nextStage);
        newRiddle.setQuestion(question);
        newRiddle.setAnswer(answer);
        newRiddle.setNextMsg(nextMsg);
        newRiddle.setImageId(imageId);
        newRiddle.setHintMsg(hintMsg);
        riddleRepository.insert(newRiddle);
    }

    // 謎の更新（画像とヒントに対応）
    @Transactional
    public void updateRiddle(Integer id, String groupId, String question, String answer, String nextMsg,
            String hintMsg, Integer imageId) {
        Riddle resultRiddle = getRiddleOwnedBy(id, groupId);
        resultRiddle.setQuestion(question);
        resultRiddle.setAnswer(answer);
        resultRiddle.setNextMsg(nextMsg);
        resultRiddle.setHintMsg(hintMsg);

        // 今回は「Controller側で、画像がない場合は古いIDを渡す」ように制御します）
        if (imageId != null) {
            resultRiddle.setImageId(imageId);
        }

        riddleRepository.update(resultRiddle);
    }

    // 謎の削除
    @Transactional
    public void deleteRiddle(Integer id, String groupId) {
        getRiddleOwnedBy(id, groupId);

        // 1. まず、この問題に関連する「回答履歴」を削除する
        solvedHistoryRepository.deleteByRiddleId(id);

        // 2. その後、問題本体を削除する
        riddleRepository.delete(id);
    }

    @Transactional
    public void updateEventSettings(String groupId, Boolean isRandom) {
        if (isRandom == null)
            isRandom = false;
        teamGroupRepository.updateRandomMode(groupId, isRandom);
    }

    @Transactional
    public void startEvent(String groupId) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        teamGroupRepository.updateStartedAt(groupId, now);
    }

    // ▼▼▼ 画像・カタログ機能 ▼▼▼

    // 画像アップロード（自動リサイズ付き）
    @Transactional
    public Integer uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty())
            return null;
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("画像ファイルは10MB以下にしてください");
        }

        byte[] originalData = file.getBytes();
        if (!isAllowedImageBytes(originalData)) {
            throw new IllegalArgumentException("画像ファイル（JPEG/PNG/GIF）のみアップロードできます");
        }

        byte[] savedData;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(originalData)) {
            BufferedImage image = ImageIO.read(bis);
            validateImageDimensions(image);
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                Thumbnails.of(image).width(800).outputFormat("jpg")
                    .outputQuality(0.8).toOutputStream(bos);
                savedData = bos.toByteArray();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("画像ファイルを処理できませんでした");
        }

        // 3. データベースに保存
        RiddleImage img = new RiddleImage();
        img.setData(savedData);
        img.setMimeType("image/jpeg");
        img.setUuid(UUID.randomUUID().toString());
        riddleImageRepository.insert(img);

        return img.getId();
    }

    private boolean isAllowedImageBytes(byte[] data) {
        if (data.length < 4)
            return false;
        if (data[0] == (byte) 0xFF && data[1] == (byte) 0xD8 && data[2] == (byte) 0xFF)
            return true;
        if (data[0] == (byte) 0x89 && data[1] == (byte) 0x50
                && data[2] == (byte) 0x4E && data[3] == (byte) 0x47)
            return true;
        return data[0] == (byte) 0x47 && data[1] == (byte) 0x49
                && data[2] == (byte) 0x46 && data[3] == (byte) 0x38;
    }

    private boolean isBCryptHash(String value) {
        return value.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$");
    }

    private void validateImageDimensions(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("画像ファイルを処理できませんでした");
        }
        int width = image.getWidth();
        int height = image.getHeight();
        long pixels = (long) width * height;
        if (width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION
                || pixels > MAX_IMAGE_PIXELS) {
            throw new IllegalArgumentException("画像サイズが大きすぎます");
        }
    }

    // カタログ取得
    public List<MasterRiddle> getCatalog() {
        return masterRiddleRepository.findAll();
    }

    // カタログからインポート
    @Transactional
    public void importFromCatalog(String groupId, Integer masterRiddleId) {
        MasterRiddle m = masterRiddleRepository.findById(masterRiddleId);
        if (m == null)
            return;
        registerRiddle(groupId, m.getQuestion(), m.getAnswer(), m.getNextMsg(), m.getImageId(),
                m.getHintMsg());
    }

    // 管理者用マスター登録
    @Transactional
    public void registerMasterRiddle(String q, String a, String n, String h, Integer imgId,
            String cat) {
        MasterRiddle mr = new MasterRiddle();
        mr.setQuestion(q);
        mr.setAnswer(a);
        mr.setNextMsg(n);
        mr.setHintMsg(h);
        mr.setImageId(imgId);
        mr.setCategory(cat);
        masterRiddleRepository.insert(mr);
    }

    // ▼▼▼ スーパーAdmin用機能 ▼▼▼

    // 全イベントリスト取得
    public List<TeamGroup> getAllEvents() {
        return teamGroupRepository.findAll();
    }

    // イベントの強制削除（プレイヤー、謎、グループ本体をまとめて消す）
    @Transactional
    public void deleteEvent(String groupId) {
        // 1. プレイヤーを削除（履歴は外部キー制約で自動削除される想定、または履歴も消す必要あり）
        playerRepository.deleteByGroupId(groupId);

        // 2. 謎（シナリオ）を削除
        riddleRepository.deleteByGroupId(groupId);

        // 3. グループ本体を削除
        teamGroupRepository.delete(groupId);
    }

    // 開始時間をリセットして「準備中」に戻す
    @Transactional
    public void resetEventTime(String groupId) {
        // nullを渡すことで、DBの started_at を NULL に戻します
        teamGroupRepository.updateStartedAt(groupId, null);
    }

    // ▼▼▼カタログ問題の削除 ▼▼▼
    @Transactional
    public void deleteMasterRiddle(Integer id) {
        masterRiddleRepository.delete(id);
    }

    // 1件取得（編集画面用）
    public MasterRiddle getMasterRiddle(Integer id) {
        return masterRiddleRepository.findById(id);
    }

    // 更新処理
    @Transactional
    public void updateMasterRiddle(Integer id, String question, String answer, String nextMsg,
            String hintMsg, Integer imageId, String category) {

        MasterRiddle mr = masterRiddleRepository.findById(id);
        if (mr == null)
            return;

        mr.setQuestion(question);
        mr.setAnswer(answer);
        mr.setNextMsg(nextMsg);
        mr.setHintMsg(hintMsg);
        mr.setCategory(category);

        // 画像が新しくアップロードされた場合のみIDを更新
        if (imageId != null) {
            mr.setImageId(imageId);
        }

        masterRiddleRepository.update(mr);
    }
}
