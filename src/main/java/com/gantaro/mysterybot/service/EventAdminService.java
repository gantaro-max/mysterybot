package com.gantaro.mysterybot.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gantaro.mysterybot.entity.Player;
import com.gantaro.mysterybot.entity.Riddle;
import com.gantaro.mysterybot.entity.TeamGroup;
import com.gantaro.mysterybot.repository.PlayerRepository;
import com.gantaro.mysterybot.repository.RiddleRepository;
import com.gantaro.mysterybot.repository.TeamGroupRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventAdminService {

    private final TeamGroupRepository teamGroupRepository;
    private final RiddleRepository riddleRepository;
    private final PlayerRepository playerRepository;

    // ▼▼▼ ログイン・イベント作成 ▼▼▼

    // ログイン判定
    public boolean login(String groupId, String password) {
        Optional<TeamGroup> group = teamGroupRepository.findByGroupId(groupId);
        if (group.isEmpty())
            return false;

        String savedPass = group.get().getAdminPass();
        // ※本番環境ではパスワードのハッシュ化が推奨されますが、現在は平文比較します
        return savedPass != null && savedPass.equals(password);
    }

    // イベント新規作成
    @Transactional
    public void createEvent(String groupId, String groupName, String password) {
        if (teamGroupRepository.findByGroupId(groupId).isPresent()) {
            throw new IllegalArgumentException("そのイベントIDは既に使用されています");
        }
        TeamGroup newGroup = new TeamGroup();
        newGroup.setGroupId(groupId);
        newGroup.setGroupName(groupName);
        newGroup.setAdminPass(password); // パスワードも保存
        newGroup.setIsRandomOrder(false); // デフォルトは順番通り

        teamGroupRepository.insert(newGroup);
    }

    // ▼▼▼ データ取得系 ▼▼▼

    // イベント情報取得
    public TeamGroup getEvent(String groupId) {
        return teamGroupRepository.findByGroupId(groupId)
                .orElseThrow(() -> new IllegalArgumentException("グループが見つかりません: " + groupId));
    }

    // ランキング取得
    public List<Player> getRanking(String groupId) {
        return playerRepository.findRankingByGroup(groupId);
    }

    // 問題一覧取得
    public List<Riddle> getRiddles(String groupId) {
        return riddleRepository.findAllByGroup(groupId);
    }

    // 1件の問題を取得
    public Riddle getRiddle(Integer id) {
        return riddleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("謎が見つかりません:ID" + id));
    }

    // ▼▼▼ 更新・削除系 ▼▼▼

    // 謎の登録
    @Transactional
    public void registerRiddle(String groupId, String question, String answer, String nextMsg) {
        Integer nextStage = riddleRepository.countByGroup(groupId) + 1;
        Riddle newRiddle = new Riddle();
        newRiddle.setGroupId(groupId);
        newRiddle.setStageNo(nextStage);
        newRiddle.setQuestion(question);
        newRiddle.setAnswer(answer);
        newRiddle.setNextMsg(nextMsg);
        riddleRepository.insert(newRiddle);
    }

    // 謎の更新
    @Transactional
    public void updateRiddle(Integer id, String question, String answer, String nextMsg) {
        Riddle resultRiddle = getRiddle(id);
        resultRiddle.setQuestion(question);
        resultRiddle.setAnswer(answer);
        resultRiddle.setNextMsg(nextMsg);
        riddleRepository.update(resultRiddle);
    }

    // 謎の削除
    @Transactional
    public void deleteRiddle(Integer id) {
        riddleRepository.delete(id);
    }

    // 設定変更
    @Transactional
    public void updateEventSettings(String groupId, Boolean isRandom) {
        if (isRandom == null)
            isRandom = false;
        teamGroupRepository.updateRandomMode(groupId, isRandom);
    }

    // イベントを開始する（スイッチON）
    @Transactional
    public void startEvent(String groupId) {
        // 現在時刻をセット
        Timestamp now = new Timestamp(System.currentTimeMillis());
        teamGroupRepository.updateStartedAt(groupId, now);
    }
}
