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
    // 制限時間（24時間）を定数定義
    private static final long TIME_LIMIT_MS = 24L * 60 * 60 * 1000;

    // イベントが終了しているか判定するメソッド
    private boolean isEventExpired(TeamGroup group) {
        if (group.getStartedAt() == null)
            return false;
        long elapsed = System.currentTimeMillis() - group.getStartedAt().getTime();
        return elapsed > TIME_LIMIT_MS;
    }

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

    // ヒント取得機能
    public String getHint(String lineUserId) {
        Player player = playerRepository.findByLineUserId(lineUserId).orElse(null);
        if (player == null || player.getCurrentRiddleId() == null) {
            return "ゲームに参加していないか、準備中です。";
        }
        Riddle currentRiddle = riddleRepository.findById(player.getCurrentRiddleId()).orElseThrow();
        String hint = currentRiddle.getHintMsg();
        if (hint == null || hint.isEmpty()) {
            return "この問題にヒントはありません。頑張って！";
        }
        return "💡 ヒント:\n" + hint;
    }

    // 1. 開始処理
    @Transactional
    public GameResult joinGame(String lineUserId, String groupId) {
        Optional<TeamGroup> findGroup = teamGroupRepository.findByGroupId(groupId);
        if (findGroup.isEmpty())
            return new GameResult(GameResult.Status.FAILURE, "イベントが見つかりません", null, null);

        TeamGroup group = findGroup.get();
        if (group.getStartedAt() == null) {
            return new GameResult(GameResult.Status.TEXT_ONLY, "⛔ 準備中 ⛔", null, null);
        }

        if (isEventExpired(group)) {
            return new GameResult(GameResult.Status.TEXT_ONLY,
                    "⛔ イベント終了 ⛔\n開催期間（24時間）が終了しました。\nご参加ありがとうございました！", null, null);
        }

        Player player = playerRepository.findByLineUserAndGroup(lineUserId, groupId).orElse(null);
        if (player == null) {
            player = new Player();
            player.setGroupId(groupId);
            player.setLineUserId(lineUserId);
            player.setCurrentStage(0);
            playerRepository.insert(player);
            return new GameResult(GameResult.Status.TEXT_ONLY, "参加ありがとうございます！\nチーム名を入力してください。",
                    null, null);
        }
        if (player.getCurrentStage() == 0) {
            return new GameResult(GameResult.Status.TEXT_ONLY, "チーム名を入力してください。", null, null);
        }
        if (player.getFinishedAt() != null) {
            return new GameResult(GameResult.Status.TEXT_ONLY, "既に全問クリアしています！", null, null);
        }
        if (player.getCurrentRiddleId() != null) {
            Riddle r = getRiddle(player.getCurrentRiddleId());
            // ★修正: getImageUuid() を使う
            return new GameResult(GameResult.Status.SUCCESS, r.getQuestion(), null,
                    r.getImageUuid());
        }
        return new GameResult(GameResult.Status.FAILURE, "エラー: 問題が見つかりません", null, null);
    }

    // 2. 回答処理
    @Transactional
    public GameResult processAnswer(String lineUserId, String userText) {
        Player player = playerRepository.findByLineUserId(lineUserId).orElseThrow(
                () -> new IllegalArgumentException("まだ参加していません 開始 [イベントID]」を入力し送信してください"));

        TeamGroup group = teamGroupRepository.findByGroupId(player.getGroupId()).orElseThrow();
        if (isEventExpired(group)) {
            return new GameResult(GameResult.Status.TEXT_ONLY,
                    "⛔ イベント終了 ⛔\n開催期間（24時間）が終了しました。\n回答の受付は締め切られました。", null, null);
        }

        if (player.getCurrentStage() == 0) {
            String name = userText.trim();
            playerRepository.updateNameAndStart(player.getId(), name, LocalDateTime.now());
            player.setCurrentStage(1);
            Optional<Riddle> firstRiddle = getNextRiddle(player, player.getGroupId());
            if (firstRiddle.isEmpty())
                return new GameResult(GameResult.Status.TEXT_ONLY, "問題がありません", null, null);

            playerRepository.updateCurrentRiddleId(player.getId(), firstRiddle.get().getId());

            // ★修正: getImageUuid() を使う
            return new GameResult(GameResult.Status.SUCCESS, firstRiddle.get().getQuestion(), null,
                    firstRiddle.get().getImageUuid());
        }

        Riddle currentRiddle = getRiddle(player.getCurrentRiddleId());

        boolean isCorrect = false;
        String[] answers = currentRiddle.getAnswer().split("[,、]");
        for (String ans : answers) {
            if (userText.trim().equalsIgnoreCase(ans.trim())) {
                isCorrect = true;
                break;
            }
        }

        if (isCorrect) {
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
                        "全問クリア！", null);
            } else {
                playerRepository.updateCurrentRiddleId(player.getId(), nextRiddle.get().getId());

                return new GameResult(GameResult.Status.SUCCESS, currentRiddle.getNextMsg(),
                        nextRiddle.get().getQuestion(), nextRiddle.get().getImageUuid());
            }
        } else {
            return new GameResult(GameResult.Status.FAILURE, "不正解...", null, null);
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
