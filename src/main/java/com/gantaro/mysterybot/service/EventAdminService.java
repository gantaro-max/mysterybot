package com.gantaro.mysterybot.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    // ▼▼▼ ログイン・イベント作成 ▼▼▼
    public boolean login(String groupId, String password) {
        Optional<TeamGroup> group = teamGroupRepository.findByGroupId(groupId);
        if (group.isEmpty())
            return false;
        String savedPass = group.get().getAdminPass();
        return savedPass != null && savedPass.equals(password);
    }

    @Transactional
    public void createEvent(String groupId, String groupName, String password) {
        if (teamGroupRepository.findByGroupId(groupId).isPresent()) {
            throw new IllegalArgumentException("そのイベントIDは既に使用されています");
        }
        TeamGroup newGroup = new TeamGroup();
        newGroup.setGroupId(groupId);
        newGroup.setGroupName(groupName);
        newGroup.setAdminPass(password);
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
    public void updateRiddle(Integer id, String question, String answer, String nextMsg,
            String hintMsg, Integer imageId) {
        Riddle resultRiddle = getRiddle(id);
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
    public void deleteRiddle(Integer id) {
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

        // 1. 元の画像データを取得
        byte[] originalData = file.getBytes();
        String contentType = file.getContentType();

        // 2. 画像データ格納用
        byte[] savedData;

        // 画像(jpg, png)の場合のみリサイズ処理を行う
        if (contentType != null && contentType.startsWith("image/")) {
            try (ByteArrayInputStream bis = new ByteArrayInputStream(originalData);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

                // 幅800pxに合わせて縮小（高さは自動）、画質は80%に圧縮
                Thumbnails.of(bis).width(800).outputFormat("jpg") // 強制的にjpgにして容量削減（透過PNGを使いたい場合は外す）
                        .outputQuality(0.8).toOutputStream(bos);

                savedData = bos.toByteArray();

                // 変換したのでMIMEタイプはjpegにする
                contentType = "image/jpeg";

            } catch (Exception e) {
                // 万が一変換に失敗したら、元のデータをそのまま使う保険
                savedData = originalData;
            }
        } else {
            // 画像以外ならそのまま
            savedData = originalData;
        }

        // 3. データベースに保存
        RiddleImage img = new RiddleImage();
        img.setData(savedData);
        img.setMimeType(contentType);
        img.setUuid(UUID.randomUUID().toString());
        riddleImageRepository.insert(img);

        return img.getId();
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
}
