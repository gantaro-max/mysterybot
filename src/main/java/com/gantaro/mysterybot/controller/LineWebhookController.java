package com.gantaro.mysterybot.controller;

import org.springframework.stereotype.Component;
import com.gantaro.mysterybot.service.GameService;
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
        // テキストメッセージ以外（スタンプや画像）は無視する
        if (!(event.message() instanceof TextMessageContent)) {
            return null;
        }

        TextMessageContent textContent = (TextMessageContent) event.message();
        String userId = event.source().userId();
        String text = textContent.text().trim();

        log.info("受信メッセージ: userId={}, text={}", userId, text);

        String replyText;

        try {
            // ゲームの処理を実行
            if (text.startsWith("開始") || text.toLowerCase().startsWith("start")) {
                String[] parts = text.split("\\s+", 2);
                if (parts.length == 2) {
                    replyText = gameService.joinGame(userId, parts[1]);
                } else {
                    replyText = "イベントIDを入力してください。\n例: 「開始 demo」";
                }
            } else {
                replyText = gameService.processAnswer(userId, text);
            }
        } catch (Exception e) {
            log.error("ゲーム処理中にエラーが発生しました", e);
            replyText = "エラーが発生しました: " + e.getMessage();
        }

        // ▼ 戻り値としてMessageを返すと、SDKが自動で返信してくれます
        return new TextMessage(replyText);
    }
}
