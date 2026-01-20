package com.gantaro.mysterybot.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.bot.jackson.ModelObjectMapper;
import com.linecorp.bot.messaging.model.FlexMessage;
import com.linecorp.bot.messaging.model.Message;

@Component
public class FlexMessageHelper {

        @Value("${mysterybot.app-url}")
        private String appUrl;

        // LINE SDK 専用の Mapper を使用（これを使えば正確に変換されます）
        private static final ObjectMapper objectMapper = ModelObjectMapper.createNewObjectMapper();

        /**
         * 正解時のメッセージカードを作成
         */
        public Message createCorrectMessage(String storyText, String nextQuestionText,
                        String nextImageUuid) {
                try {
                        // 1. バブルの枠組み
                        Map<String, Object> bubble = new HashMap<>();
                        bubble.put("type", "bubble");

                        // 2. Hero (画像)
                        if (nextImageUuid != null) {
                                Map<String, Object> hero = new HashMap<>();
                                hero.put("type", "image");
                                hero.put("url", appUrl + "/public/image/" + nextImageUuid);
                                hero.put("size", "full");
                                hero.put("aspectRatio", "16:9");
                                hero.put("aspectMode", "cover");
                                bubble.put("hero", hero);
                        }

                        // 3. Header (緑色)
                        Map<String, Object> header = new HashMap<>();
                        header.put("type", "box");
                        header.put("layout", "vertical");
                        header.put("backgroundColor", "#2CBF4E"); // 緑色

                        Map<String, Object> headerText = new HashMap<>();
                        headerText.put("type", "text");
                        headerText.put("text", "🎉 STAGE CLEAR 🎉");
                        headerText.put("color", "#FFFFFF");
                        headerText.put("weight", "bold");
                        headerText.put("align", "center");

                        header.put("contents", List.of(headerText));
                        bubble.put("header", header);

                        // 4. Body (ストーリー ＋ 次の問題)
                        Map<String, Object> body = new HashMap<>();
                        body.put("type", "box");
                        body.put("layout", "vertical");

                        List<Map<String, Object>> bodyContents = new ArrayList<>();

                        // (A) ストーリー
                        Map<String, Object> story = new HashMap<>();
                        story.put("type", "text");
                        story.put("text", storyText);
                        story.put("wrap", true);
                        story.put("size", "md");
                        story.put("color", "#555555");
                        bodyContents.add(story);

                        // (B) 区切り線
                        Map<String, Object> separator = new HashMap<>();
                        separator.put("type", "separator");
                        separator.put("margin", "lg");
                        bodyContents.add(separator);

                        // (C) 「次の問題」ラベル
                        Map<String, Object> labelText = new HashMap<>();
                        labelText.put("type", "text");
                        labelText.put("text", "▼ 次の問題");
                        labelText.put("size", "xs");
                        labelText.put("color", "#aaaaaa");
                        labelText.put("align", "center");
                        labelText.put("margin", "lg");
                        bodyContents.add(labelText);

                        // (D) 次の問題文
                        Map<String, Object> nextQText = new HashMap<>();
                        nextQText.put("type", "text");
                        nextQText.put("text", nextQuestionText);
                        nextQText.put("wrap", true);
                        nextQText.put("size", "md");
                        nextQText.put("weight", "bold");
                        nextQText.put("align", "center");
                        nextQText.put("margin", "sm");
                        nextQText.put("color", "#333333");
                        bodyContents.add(nextQText);

                        body.put("contents", bodyContents);
                        bubble.put("body", body);

                        // 5. Footer (補足)
                        Map<String, Object> footer = new HashMap<>();
                        footer.put("type", "box");
                        footer.put("layout", "vertical");

                        Map<String, Object> footerText = new HashMap<>();
                        footerText.put("type", "text");
                        footerText.put("text", "※答えを入力 / 「ヒント」でヒント表示");
                        footerText.put("size", "xs");
                        footerText.put("color", "#aaaaaa");
                        footerText.put("align", "center");

                        footer.put("contents", List.of(footerText));
                        bubble.put("footer", footer);

                        // 6. FlexMessageコンテナに格納
                        Map<String, Object> flexContainer = new HashMap<>();
                        flexContainer.put("type", "flex");
                        flexContainer.put("altText", "正解！次の問題です");
                        flexContainer.put("contents", bubble);

                        // ★ここでMapをSDKのFlexMessageクラスに変換
                        return objectMapper.convertValue(flexContainer, FlexMessage.class);

                } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException("Flex Message作成エラー", e);
                }
        }

        /**
         * 出題時のメッセージカードを作成
         */
        public Message createQuestionMessage(String questionText, String imageUuid) {
                try {
                        Map<String, Object> bubble = new HashMap<>();
                        bubble.put("type", "bubble");

                        // Hero
                        if (imageUuid != null) {
                                Map<String, Object> hero = new HashMap<>();
                                hero.put("type", "image");
                                hero.put("url", appUrl + "/public/image/" + imageUuid);
                                hero.put("size", "full");
                                hero.put("aspectRatio", "16:9");
                                hero.put("aspectMode", "cover");
                                bubble.put("hero", hero);
                        }

                        // Header (青色)
                        Map<String, Object> header = new HashMap<>();
                        header.put("type", "box");
                        header.put("layout", "vertical");
                        header.put("backgroundColor", "#0055aa"); // 青色

                        Map<String, Object> headerText = new HashMap<>();
                        headerText.put("type", "text");
                        headerText.put("text", "📝 MISSION 📝");
                        headerText.put("color", "#FFFFFF");
                        headerText.put("weight", "bold");
                        headerText.put("align", "center");

                        header.put("contents", List.of(headerText));
                        bubble.put("header", header);

                        // Body
                        Map<String, Object> body = new HashMap<>();
                        body.put("type", "box");
                        body.put("layout", "vertical");

                        Map<String, Object> qText = new HashMap<>();
                        qText.put("type", "text");
                        qText.put("text", questionText);
                        qText.put("wrap", true);
                        qText.put("size", "md");
                        qText.put("color", "#333333");

                        body.put("contents", List.of(qText));
                        bubble.put("body", body);

                        // Footer
                        Map<String, Object> footer = new HashMap<>();
                        footer.put("type", "box");
                        footer.put("layout", "vertical");

                        Map<String, Object> fText = new HashMap<>();
                        fText.put("type", "text");
                        fText.put("text", "※答えを入力 / 「ヒント」でヒント表示");
                        fText.put("size", "xs");
                        fText.put("color", "#aaaaaa");
                        fText.put("align", "center");

                        footer.put("contents", List.of(fText));
                        bubble.put("footer", footer);

                        // FlexMessageへ
                        Map<String, Object> flexContainer = new HashMap<>();
                        flexContainer.put("type", "flex");
                        flexContainer.put("altText", "新しい問題です");
                        flexContainer.put("contents", bubble);

                        return objectMapper.convertValue(flexContainer, FlexMessage.class);

                } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException("Flex Message作成エラー", e);
                }
        }
}
