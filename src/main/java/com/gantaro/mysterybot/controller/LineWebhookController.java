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
        String text = textContent.text().trim();

        log.info("受信メッセージ: userId={}, text={}", userId, text);

        try {
            // 0. リセットコマンド
            if (text.equals("リセット") || text.equalsIgnoreCase("reset")) {
                String reply = gameService.resetGame(userId);
                return new TextMessage(reply);
            }

            // 1. 開始コマンドの処理
            if (text.startsWith("開始") || text.toLowerCase().startsWith("start")) {
                String[] parts = text.split("\\s+", 2);
                if (parts.length == 2) {
                    GameResult result = gameService.joinGame(userId, parts[1]);

                    if (result.getStatus() == GameResult.Status.SUCCESS) {
                        // 開始成功（再開時など）は「出題カード」
                        return FlexMessageHelper.createQuestionMessage(result.getMainText());
                    } else {
                        return new TextMessage(result.getMainText());
                    }
                } else {
                    return new TextMessage("イベントIDを入力してください。\n例: 「開始 demo」");
                }
            }
            // 2. 回答（および名前入力）の処理
            else {
                GameResult result = gameService.processAnswer(userId, text);

                if (result.getStatus() == GameResult.Status.SUCCESS) {

                    // subText(次の問題)がある ＝ 「謎解き正解」のとき → 緑のカード
                    if (result.getSubText() != null) {
                        return FlexMessageHelper.createCorrectMessage(result.getMainText(), // ストーリー（正解メッセージ）
                                result.getSubText() // 次の問題
                        );
                    }
                    // subTextがない ＝ 「名前登録完了」のとき → 青のカード
                    else {
                        return FlexMessageHelper.createQuestionMessage(result.getMainText() // 第1問の問題文
                        );
                    }

                } else {
                    // 不正解やエラーメッセージはテキストで返す
                    return new TextMessage(result.getMainText());
                }
            }
        } catch (Exception e) {
            log.error("ゲーム処理中にエラーが発生しました", e);
            return new TextMessage("エラーが発生しました: " + e.getMessage());
        }
    }
}
