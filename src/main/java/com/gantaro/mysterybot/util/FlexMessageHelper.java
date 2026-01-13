package com.gantaro.mysterybot.util;

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

        // LINE SDKに含まれる「MapからFlexMessageクラスへ変換するツール」
        private static final ObjectMapper objectMapper = ModelObjectMapper.createNewObjectMapper();

        // 正解返信用のカード
        // ★修正: 第3引数を Integer から String (UUID) に変更
        public Message createCorrectMessage(String storyText, String nextQuestionText,
                        String nextImageUuid) {
                try {
                        Map<String, Object> bubble = new HashMap<>();
                        bubble.put("type", "bubble");

                        // -------------------------------------------------
                        // ★追加: 画像ブロック (Hero)
                        // -------------------------------------------------
                        // 次の問題の画像がある場合は、正解カードの上部に表示する
                        if (nextImageUuid != null) {
                                Map<String, Object> hero = new HashMap<>();
                                hero.put("type", "image");
                                // ★修正: UUIDを使った公開用URL
                                hero.put("url", appUrl + "/public/image/" + nextImageUuid);
                                hero.put("size", "full");
                                hero.put("aspectRatio", "16:9");
                                hero.put("aspectMode", "cover");
                                bubble.put("hero", hero);
                        }

                        // -------------------------------------------------
                        // 1. ヘッダー (STAGE CLEAR)
                        // -------------------------------------------------
                        Map<String, Object> header = new HashMap<>();
                        header.put("type", "box");
                        header.put("layout", "vertical");
                        header.put("backgroundColor", "#2CBF4E");

                        Map<String, Object> titleText = new HashMap<>();
                        titleText.put("type", "text");
                        titleText.put("text", "🎉 STAGE CLEAR 🎉");
                        titleText.put("color", "#FFFFFF");
                        titleText.put("weight", "bold");
                        titleText.put("align", "center");

                        header.put("contents", List.of(titleText));
                        bubble.put("header", header); // ★修正: 作成したheaderをbubbleに追加

                        // -------------------------------------------------
                        // 2. 本文 (ストーリー)
                        // -------------------------------------------------
                        Map<String, Object> body = new HashMap<>();
                        body.put("type", "box");
                        body.put("layout", "vertical");

                        Map<String, Object> story = new HashMap<>();
                        story.put("type", "text");
                        story.put("text", storyText);
                        story.put("wrap", true);
                        story.put("size", "md");
                        story.put("color", "#555555");

                        body.put("contents", List.of(story));
                        bubble.put("body", body); // ★修正: 作成したbodyをbubbleに追加

                        // -------------------------------------------------
                        // 3. フッター (次の問題)
                        // -------------------------------------------------
                        Map<String, Object> footer = new HashMap<>();
                        footer.put("type", "box");
                        footer.put("layout", "vertical");

                        Map<String, Object> labelText = new HashMap<>();
                        labelText.put("type", "text");
                        labelText.put("text", "▼ 次の問題");
                        labelText.put("size", "xs");
                        labelText.put("color", "#aaaaaa");
                        labelText.put("align", "center");

                        Map<String, Object> nextQText = new HashMap<>();
                        nextQText.put("type", "text");
                        nextQText.put("text", nextQuestionText);
                        nextQText.put("wrap", true);
                        nextQText.put("size", "sm");
                        nextQText.put("weight", "bold");
                        nextQText.put("align", "center");

                        footer.put("contents", List.of(labelText, nextQText));
                        bubble.put("footer", footer); // ★修正: 作成したfooterをbubbleに追加

                        // -------------------------------------------------
                        // 4. FlexMessageへ変換
                        // -------------------------------------------------
                        Map<String, Object> flexMessageMap = new HashMap<>();
                        flexMessageMap.put("type", "flex");
                        flexMessageMap.put("altText", "正解！");
                        flexMessageMap.put("contents", bubble);

                        return objectMapper.convertValue(flexMessageMap, FlexMessage.class);

                } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException("メッセージ作成エラー", e);
                }
        }

        // 出題用のカード
        public Message createQuestionMessage(String questionText, String imageUuid) {
                try {
                        Map<String, Object> bubble = new HashMap<>();
                        bubble.put("type", "bubble");

                        // -------------------------------------------------
                        // 画像ブロック (Hero)
                        // -------------------------------------------------
                        if (imageUuid != null) {
                                Map<String, Object> hero = new HashMap<>();
                                hero.put("type", "image");
                                // ★修正: UUIDを使った公開用URL
                                hero.put("url", appUrl + "/public/image/" + imageUuid);
                                hero.put("size", "full");
                                hero.put("aspectRatio", "16:9");
                                hero.put("aspectMode", "cover");
                                bubble.put("hero", hero);
                        }

                        // -------------------------------------------------
                        // 1. ヘッダー
                        // -------------------------------------------------
                        Map<String, Object> header = new HashMap<>();
                        header.put("type", "box");
                        header.put("layout", "vertical");
                        header.put("backgroundColor", "#0055aa");

                        Map<String, Object> titleText = new HashMap<>();
                        titleText.put("type", "text");
                        titleText.put("text", "📝 MISSION 📝");
                        titleText.put("color", "#FFFFFF");
                        titleText.put("weight", "bold");
                        titleText.put("align", "center");

                        header.put("contents", List.of(titleText));
                        bubble.put("header", header);

                        // -------------------------------------------------
                        // 2. 本文
                        // -------------------------------------------------
                        Map<String, Object> body = new HashMap<>();
                        body.put("type", "box");
                        body.put("layout", "vertical");

                        Map<String, Object> questionBody = new HashMap<>();
                        questionBody.put("type", "text");
                        questionBody.put("text", questionText);
                        questionBody.put("wrap", true);
                        questionBody.put("size", "md");
                        questionBody.put("color", "#333333");

                        body.put("contents", List.of(questionBody));
                        bubble.put("body", body);

                        // -------------------------------------------------
                        // 3. フッター
                        // -------------------------------------------------
                        Map<String, Object> footer = new HashMap<>();
                        footer.put("type", "box");
                        footer.put("layout", "vertical");

                        Map<String, Object> infoText = new HashMap<>();
                        infoText.put("type", "text");
                        infoText.put("text", "※答えを入力 / 「ヒント」でヒント表示");
                        infoText.put("size", "xs");
                        infoText.put("color", "#aaaaaa");
                        infoText.put("align", "center");

                        footer.put("contents", List.of(infoText));
                        bubble.put("footer", footer);

                        // -------------------------------------------------
                        // 5. FlexMessageへ変換
                        // -------------------------------------------------
                        Map<String, Object> flexMessageMap = new HashMap<>();
                        flexMessageMap.put("type", "flex");
                        flexMessageMap.put("altText", "新しい問題です");
                        flexMessageMap.put("contents", bubble);

                        return objectMapper.convertValue(flexMessageMap, FlexMessage.class);

                } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException("メッセージ作成エラー", e);
                }
        }
}
