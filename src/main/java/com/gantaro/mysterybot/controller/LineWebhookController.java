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
            // 1. 開始コマンドの処理（ここはStringのまま）
            if (text.startsWith("開始") || text.toLowerCase().startsWith("start")) {
                String[] parts = text.split("\\s+", 2);
                if (parts.length == 2) {
                    GameResult result = gameService.joinGame(userId, parts[1]);
                    if (result.getStatus() == GameResult.Status.SUCCESS) {
                        return FlexMessageHelper.createQuestionMessage(result.getMainText());
                    } else {
                        return new TextMessage(result.getMainText());
                    }
                } else {
                    return new TextMessage("イベントIDを入力してください。\n例: 「開始 demo」");
                }
            }
            // 2. 回答の処理
            else {
                GameResult result = gameService.processAnswer(userId, text);

                if (result.getStatus() == GameResult.Status.SUCCESS) {
                    // ★★★ 正解なら、さっき作ったカードを送る！ ★★★
                    return FlexMessageHelper.createCorrectMessage(result.getMainText(), // ストーリー
                            result.getSubText() // 次の問題
                    );
                } else {
                    // それ以外は今まで通り文字で返す
                    return new TextMessage(result.getMainText());
                }
            }
        } catch (Exception e) {
            log.error("ゲーム処理中にエラーが発生しました", e);
            return new TextMessage("エラーが発生しました: " + e.getMessage());
        }
    }
}
