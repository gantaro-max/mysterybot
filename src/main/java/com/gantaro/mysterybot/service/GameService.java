package com.gantaro.mysterybot.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gantaro.mysterybot.dto.GameResult;
import com.gantaro.mysterybot.entity.Player;
import com.gantaro.mysterybot.entity.Riddle;
import com.gantaro.mysterybot.entity.TeamGroup;
import com.gantaro.mysterybot.repository.PlayerRepository;
import com.gantaro.mysterybot.repository.RiddleRepository;
import com.gantaro.mysterybot.repository.TeamGroupRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GameService {
    private final RiddleRepository riddleRepository;
    private final PlayerRepository playerRepository;
    private final TeamGroupRepository teamGroupRepository;

    @Transactional
    public GameResult joinGame(String lineUserId, String groupId) {
        Optional<TeamGroup> findGroup = teamGroupRepository.findByGroupId(groupId);
        if (findGroup.isEmpty()) {
            return new GameResult(GameResult.Status.FAILURE, "グループが見つかりません", null);
        }

        Optional<Player> searchPlayer =
                playerRepository.findByLineUserAndGroup(lineUserId, groupId);

        Integer stageNo = 1;

        if (searchPlayer.isEmpty()) {
            Player newPlayer = new Player();
            newPlayer.setGroupId(groupId);
            newPlayer.setLineUserId(lineUserId);
            playerRepository.insert(newPlayer);
        } else {
            stageNo = searchPlayer.get().getCurrentStage();
        }

        Optional<Riddle> resultRiddle = riddleRepository.findByGroupIdAndStageNo(groupId, stageNo);
        if (resultRiddle.isEmpty()) {
            return new GameResult(GameResult.Status.TEXT_ONLY, "すべてクリアしています", null);
        }

        return new GameResult(GameResult.Status.SUCCESS, resultRiddle.get().getQuestion(), null);
    }

    @Transactional
    public GameResult processAnswer(String lineUserId, String userText) {
        Optional<Player> resultPlayer = playerRepository.findByLineUserId(lineUserId);
        if (resultPlayer.isEmpty()) {
            return new GameResult(GameResult.Status.TEXT_ONLY, "まずは「開始 [イベントID]」と送って参加してください",
                    null);
        }
        Optional<Riddle> resultRiddle = riddleRepository.findByGroupIdAndStageNo(
                resultPlayer.get().getGroupId(), resultPlayer.get().getCurrentStage());
        if (resultRiddle.isEmpty()) {
            return new GameResult(GameResult.Status.TEXT_ONLY, "すべてクリアしています", null);
        }
        if (userText.trim().equalsIgnoreCase(resultRiddle.get().getAnswer())) {
            playerRepository.updateProgress(resultPlayer.get().getId(),
                    resultPlayer.get().getCurrentStage() + 1);
            Optional<Riddle> nextRiddle = riddleRepository.findByGroupIdAndStageNo(
                    resultPlayer.get().getGroupId(), resultPlayer.get().getCurrentStage() + 1);
            if (nextRiddle.isEmpty()) {
                return new GameResult(GameResult.Status.SUCCESS, resultRiddle.get().getNextMsg(),
                        "全問クリアおめでとう！");
            } else {
                return new GameResult(GameResult.Status.SUCCESS, resultRiddle.get().getNextMsg(),
                        nextRiddle.get().getQuestion());
            }
        } else {
            return new GameResult(GameResult.Status.FAILURE, "残念、不正解です...もう一度チャレンジ！", null);
        }

    }

    @Transactional
    public void createEvent(String eventId, String eventName) {
        // すでに同じIDがないかチェック
        if (teamGroupRepository.findByGroupId(eventId).isPresent()) {
            throw new IllegalArgumentException("そのイベントIDは既に使用されています");
        }

        TeamGroup newGroup = new TeamGroup();
        newGroup.setGroupId(eventId);
        newGroup.setGroupName(eventName); // キーワードではなくイベント名として保存します

        teamGroupRepository.insert(newGroup);
    }

    // イベント一覧を取得
    public List<TeamGroup> getAllEvents() {
        return teamGroupRepository.findAll();
    }

    // イベントの謎一覧を取得
    public List<Riddle> getRiddles(String groupId) {
        return riddleRepository.findAllByGroup(groupId);
    }

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

    // 1件の問題を取得
    public Riddle getRiddle(Integer id) {
        return riddleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("謎が見つかりません:ID" + id));

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



}
