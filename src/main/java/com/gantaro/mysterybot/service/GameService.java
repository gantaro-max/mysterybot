package com.gantaro.mysterybot.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gantaro.mysterybot.dto.GameResult;
import com.gantaro.mysterybot.entity.Player;
import com.gantaro.mysterybot.entity.Riddle;
import com.gantaro.mysterybot.entity.TeamGroup;
import com.gantaro.mysterybot.repository.PlayerRepository;
import com.gantaro.mysterybot.repository.RiddleRepository;
import com.gantaro.mysterybot.repository.SolvedHistoryRepository;
import com.gantaro.mysterybot.repository.TeamGroupRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GameService {
    private final RiddleRepository riddleRepository;
    private final PlayerRepository playerRepository;
    private final TeamGroupRepository teamGroupRepository;
    private final SolvedHistoryRepository solvedHistoryRepository;

    // ヘルパーメソッド: IDからRiddleを取得
    private Riddle getRiddle(Integer id) {
        return riddleRepository.findById(id).orElseThrow();
    }

    // 次の問題を選ぶ処理
    private Optional<Riddle> getNextRiddle(Player player, String groupId) {
        TeamGroup group = teamGroupRepository.findByGroupId(groupId).orElseThrow();

        if (Boolean.TRUE.equals(group.getIsRandomOrder())) {
            List<Riddle> allRiddles = riddleRepository.findAllByGroup(groupId);
            List<Integer> solvedIds = solvedHistoryRepository.findSolvedRiddleIds(player.getId());
            List<Riddle> remaining = allRiddles.stream().filter(r -> !solvedIds.contains(r.getId()))
                    .collect(Collectors.toList());

            if (remaining.isEmpty())
                return Optional.empty();
            Collections.shuffle(remaining);
            return Optional.of(remaining.get(0));
        } else {
            return riddleRepository.findByGroupIdAndStageNo(groupId, player.getCurrentStage());
        }
    }

    // 1. 開始処理
    @Transactional
    public GameResult joinGame(String lineUserId, String groupId) {
        Optional<TeamGroup> findGroup = teamGroupRepository.findByGroupId(groupId);
        if (findGroup.isEmpty())
            return new GameResult(GameResult.Status.FAILURE, "イベントが見つかりません", null);

        Player player = playerRepository.findByLineUserAndGroup(lineUserId, groupId).orElse(null);

        if (player == null) {
            player = new Player();
            player.setGroupId(groupId);
            player.setLineUserId(lineUserId);
            player.setCurrentStage(0);
            playerRepository.insert(player);
            return new GameResult(GameResult.Status.TEXT_ONLY,
                    "参加ありがとうございます！\nチーム名または個人名を入力してください。", null);
        }
        if (player.getCurrentStage() == 0) {
            return new GameResult(GameResult.Status.TEXT_ONLY, "チーム名または個人名を入力してください。", null);
        }
        if (player.getFinishedAt() != null) {
            return new GameResult(GameResult.Status.TEXT_ONLY,
                    "既に全問クリアしています！タイム: " + player.getClearTime(), null);
        }
        if (player.getCurrentRiddleId() != null) {
            Riddle r = getRiddle(player.getCurrentRiddleId());
            return new GameResult(GameResult.Status.SUCCESS, r.getQuestion(), null);
        }

        return new GameResult(GameResult.Status.FAILURE, "エラー: 問題が見つかりません", null);
    }

    // 2. 回答処理
    @Transactional
    public GameResult processAnswer(String lineUserId, String userText) {
        Player player = playerRepository.findByLineUserId(lineUserId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "まだ参加していません 開始 [イベントID]」を入力し送信するか、開始用のQRコードを読み込んでください"));

        if (player.getCurrentStage() == 0) {
            String name = userText.trim();
            if (name.isEmpty() || name.length() > 20) {
                return new GameResult(GameResult.Status.TEXT_ONLY, "名前は1〜20文字で入力してください", null);
            }
            playerRepository.updateNameAndStart(player.getId(), name, LocalDateTime.now());
            player.setCurrentStage(1);

            Optional<Riddle> firstRiddle = getNextRiddle(player, player.getGroupId());
            if (firstRiddle.isEmpty())
                return new GameResult(GameResult.Status.TEXT_ONLY, "問題がありません", null);

            playerRepository.updateCurrentRiddleId(player.getId(), firstRiddle.get().getId());
            return new GameResult(GameResult.Status.SUCCESS, firstRiddle.get().getQuestion(), null);
        }

        if (player.getCurrentRiddleId() == null) {
            return new GameResult(GameResult.Status.TEXT_ONLY, "既にクリア済みです", null);
        }
        Riddle currentRiddle = getRiddle(player.getCurrentRiddleId());

        if (userText.trim().equalsIgnoreCase(currentRiddle.getAnswer())) {
            solvedHistoryRepository.insert(player.getId(), currentRiddle.getId());
            player.setCurrentStage(player.getCurrentStage() + 1);
            playerRepository.updateProgress(player.getId(), player.getCurrentStage());

            Optional<Riddle> nextRiddle = getNextRiddle(player, player.getGroupId());

            if (nextRiddle.isEmpty()) {
                LocalDateTime now = LocalDateTime.now();
                playerRepository.updateFinishedAt(player.getId(), now);
                playerRepository.updateCurrentRiddleId(player.getId(), null);
                player.setFinishedAt(now);
                return new GameResult(GameResult.Status.SUCCESS, currentRiddle.getNextMsg(),
                        "全問クリア！タイム: " + player.getClearTime());
            } else {
                playerRepository.updateCurrentRiddleId(player.getId(), nextRiddle.get().getId());
                return new GameResult(GameResult.Status.SUCCESS, currentRiddle.getNextMsg(),
                        nextRiddle.get().getQuestion());
            }
        } else {
            return new GameResult(GameResult.Status.FAILURE, "不正解...", null);
        }
    }

    // 3. リセット機能
    @Transactional
    public String resetGame(String lineUserId) {
        Optional<Player> playerOpt = playerRepository.findByLineUserId(lineUserId);
        if (playerOpt.isEmpty())
            return "プレイデータが見つかりません。";

        Integer playerId = playerOpt.get().getId();
        solvedHistoryRepository.deleteByPlayerId(playerId);
        playerRepository.deleteById(playerId);
        return "データをリセットしました！\n「開始 [イベントID]」で最初から遊べます。";
    }
}
