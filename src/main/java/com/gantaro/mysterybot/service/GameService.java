package com.gantaro.mysterybot.service;

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
public class GameService {
    private final RiddleRepository riddleRepository;
    private final PlayerRepository playerRepository;
    private final TeamGroupRepository teamGroupRepository;

    @Transactional
    public String joinGame(String lineUserId, String groupId) {
        Optional<TeamGroup> findGroup = teamGroupRepository.findByGroupId(groupId);
        if (findGroup.isEmpty()) {
            return "グループが見つかりません";
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
            return "すべてクリアしています";
        }

        return resultRiddle.get().getQuestion();
    }

    @Transactional
    public String processAnswer(String lineUserId, String userText) {
        Optional<Player> resultPlayer = playerRepository.findByLineUserId(lineUserId);
        if (resultPlayer.isEmpty()) {
            return "まずは「開始[イベントID]」と送って参加してください";
        }
        Optional<Riddle> resultRiddle = riddleRepository.findByGroupIdAndStageNo(
                resultPlayer.get().getGroupId(), resultPlayer.get().getCurrentStage());
        if (resultRiddle.isEmpty()) {
            return "すべてクリアしています";
        }
        if (userText.trim().equalsIgnoreCase(resultRiddle.get().getAnswer())) {
            playerRepository.updateProgress(resultPlayer.get().getId(),
                    resultPlayer.get().getCurrentStage() + 1);
            Optional<Riddle> nextRiddle = riddleRepository.findByGroupIdAndStageNo(
                    resultPlayer.get().getGroupId(), resultPlayer.get().getCurrentStage() + 1);
            if (nextRiddle.isEmpty()) {
                return resultRiddle.get().getNextMsg() + "\n\n🎉全問クリアおめでとう！";
            } else {
                return resultRiddle.get().getNextMsg() + "\n\n" + "▼ 次の問題\n"
                        + nextRiddle.get().getQuestion();
            }
        } else {
            return "残念、不正解です...もう一度チャレンジ！";
        }

    }


}
