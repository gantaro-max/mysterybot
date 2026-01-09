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
        // 門番チェック (前回実装済みと仮定、未実装ならここに追加)
        if (group.getStartedAt() == null) {
            return new GameResult(GameResult.Status.TEXT_ONLY, "⛔ 準備中 ⛔", null, null);
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
            // ★修正: 画像IDを渡す
            return new GameResult(GameResult.Status.SUCCESS, r.getQuestion(), null, r.getImageId());
        }
        return new GameResult(GameResult.Status.FAILURE, "エラー: 問題が見つかりません", null, null);
    }

    // 2. 回答処理
    @Transactional
    public GameResult processAnswer(String lineUserId, String userText) {
        Player player = playerRepository.findByLineUserId(lineUserId).orElseThrow(
                () -> new IllegalArgumentException("まだ参加していません 開始 [イベントID]」を入力し送信してください"));

        // 門番チェック (省略)

        if (player.getCurrentStage() == 0) {
            String name = userText.trim();
            playerRepository.updateNameAndStart(player.getId(), name, LocalDateTime.now());
            player.setCurrentStage(1);
            Optional<Riddle> firstRiddle = getNextRiddle(player, player.getGroupId());
            if (firstRiddle.isEmpty())
                return new GameResult(GameResult.Status.TEXT_ONLY, "問題がありません", null, null);

            playerRepository.updateCurrentRiddleId(player.getId(), firstRiddle.get().getId());
            // ★修正: 画像IDを渡す
            return new GameResult(GameResult.Status.SUCCESS, firstRiddle.get().getQuestion(), null,
                    firstRiddle.get().getImageId());
        }

        Riddle currentRiddle = getRiddle(player.getCurrentRiddleId());

        // ★修正: あいまい一致 (カンマ区切り対応)
        boolean isCorrect = false;
        String[] answers = currentRiddle.getAnswer().split("[,、]"); // カンマと読点に対応
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
                // ★修正: 次の問題の画像IDを渡す
                return new GameResult(GameResult.Status.SUCCESS, currentRiddle.getNextMsg(),
                        nextRiddle.get().getQuestion(), nextRiddle.get().getImageId());
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
