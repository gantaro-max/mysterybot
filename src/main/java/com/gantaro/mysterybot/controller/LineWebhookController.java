package com.gantaro.mysterybot.controller;

import org.springframework.stereotype.Component;
import com.gantaro.mysterybot.dto.GameResult;
import com.gantaro.mysterybot.service.GameService;
import com.gantaro.mysterybot.util.FlexMessageHelper;
import com.linecorp.bot.messaging.model.Message;
import com.linecorp.bot.messaging.model.TextMessage;
import com.linecorp.bot.spring.boot.handler.annotation.EventMapping;
import com.linecorp.bot.spring.boot.handler.annotation.LineMessageHandler;
import com.linecorp.bot.webhook.model.MessageEvent;
import com.linecorp.bot.webhook.model.TextMessageContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@LineMessageHandler
@RequiredArgsConstructor
@Slf4j
public class LineWebhookController {

    private final GameService gameService;

    @EventMapping
    public Message handleTextMessageEvent(MessageEvent event) {
        if (!(event.message() instanceof TextMessageContent)) {
            return null;
        }

        TextMessageContent textContent = (TextMessageContent) event.message();
        String userId = event.source().userId();
        String text = textContent.text().trim(); // 空白除去

        log.info("受信メッセージ: userId={}, text={}", userId, text);

        try {

            // ヒント機能
            if (text.equals("ヒント") || text.equalsIgnoreCase("hint")) {
                return new TextMessage(gameService.getHint(userId));
            }
            // ▼▼▼ 1. リセット機能 ▼▼▼
            // リッチメニューの「リセット」ボタンや、手入力に対応
            if (text.equals("リセット") || text.equalsIgnoreCase("reset")) {
                String reply = gameService.resetGame(userId);
                return new TextMessage(reply);
            }

            // ▼▼▼ 2. 遊び方ヘルプ機能 (★新規追加) ▼▼▼
            // リッチメニューの「遊び方」ボタンに対応
            if (text.equals("遊び方") || text.equals("ヘルプ")) {
                String helpMsg = """
                        🔰 遊び方ガイド

                        【1. ゲームを始める】
                        イベント主催者から案内された
                        「開始 〇〇」
                        という合言葉を送信してください。

                        【2. 謎を解く】
                        謎が解けたら、その「答え」をメッセージで送信してください。

                        【3. 行き詰まったら？】
                        前のメッセージを見返したり、仲間と相談してみましょう。

                        ──────────
                        🔄 最初からやり直す場合は
                        メニューを出して「リセット」ボタンを押してください。
                        （メッセージで「リセット」と入力してメッセージで送信でもOK）
                        """;
                return new TextMessage(helpMsg);
            }

            // ▼▼▼ 3. 開始コマンド処理 ▼▼▼
            // QRコードまたは手入力（例: "開始 demo"）に対応
            if (text.startsWith("開始") || text.toLowerCase().startsWith("start")) {
                String[] parts = text.split("\\s+", 2); // 空白で分割
                if (parts.length == 2) {
                    GameResult result = gameService.joinGame(userId, parts[1]);

                    if (result.getStatus() == GameResult.Status.SUCCESS) {
                        // ヘルパーを呼ぶ
                        return FlexMessageHelper.createQuestionMessage(result.getMainText(),
                                result.getImageId());
                    } else {
                        return new TextMessage(result.getMainText());
                    }
                } else {
                    return new TextMessage("イベントIDを入力してください。\n例: 「開始 demo」");
                }
            }

            // ▼▼▼ 4. 通常のゲーム回答処理 (それ以外) ▼▼▼
            // 上記のどのコマンドにも当てはまらない場合は、回答や名前入力とみなす
            GameResult result = gameService.processAnswer(userId, text);

            if (result.getStatus() == GameResult.Status.SUCCESS) {
                // subText(次の問題)がある ＝ 「謎解き正解」のとき → 緑のカード
                if (result.getSubText() != null) {
                    return FlexMessageHelper.createCorrectMessage(result.getMainText(),
                            result.getSubText(), result.getImageId());
                }
                // subTextがない ＝ 「名前登録完了」のとき → 青のカード
                else {

                    return FlexMessageHelper.createQuestionMessage(result.getMainText(),
                            result.getImageId());
                }

            } else {
                // 不正解やエラーメッセージ
                return new TextMessage(result.getMainText());
            }

        } catch (Exception e) {
            log.error("ゲーム処理中にエラーが発生しました", e);
            return new TextMessage("エラーが発生しました: " + e.getMessage());
        }
    }
}
