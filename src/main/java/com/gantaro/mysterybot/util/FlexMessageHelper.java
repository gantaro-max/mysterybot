package com.gantaro.mysterybot.util;

import java.util.List;
import com.linecorp.bot.messaging.model.FlexBox;
import com.linecorp.bot.messaging.model.FlexBubble;
import com.linecorp.bot.messaging.model.FlexMessage;
import com.linecorp.bot.messaging.model.FlexText;

public class FlexMessageHelper {

    // 正解返信用のカードを作成するメソッド
    public static FlexMessage createCorrectMessage(String storyText, String nextQuestionText) {

        // 1. ヘッダー（緑色の帯）を作る
        // 先に中身のテキストを作ります
        FlexText titleText = new FlexText.Builder().text("🎉 STAGE CLEAR 🎉").color("#FFFFFF")
                .weight(FlexText.Weight.BOLD).align(FlexText.Align.CENTER).build();

        // Boxを作るときに、レイアウトと中身を渡します
        FlexBox header = new FlexBox.Builder(FlexBox.Layout.VERTICAL, List.of(titleText))
                .backgroundColor("#2CBF4E").build();


        // 2. 本文（ストーリー）を作る
        FlexText storyBody = new FlexText.Builder().text(storyText).wrap(true).size("md")
                .color("#555555").build();

        FlexBox body = new FlexBox.Builder(FlexBox.Layout.VERTICAL, List.of(storyBody)).build();


        // 3. フッター（次の問題）を作る
        FlexText labelText = new FlexText.Builder().text("▼ 次の問題").size("xs").color("#aaaaaa")
                .align(FlexText.Align.CENTER).build();

        FlexText nextQText = new FlexText.Builder().text(nextQuestionText).wrap(true)
                .weight(FlexText.Weight.BOLD).size("sm").align(FlexText.Align.CENTER).margin("sm")
                .build();

        // 2つあるのでリストに並べます
        FlexBox footer =
                new FlexBox.Builder(FlexBox.Layout.VERTICAL, List.of(labelText, nextQText)).build();


        // 4. 合体して Bubble を作る
        // FlexBubbleのBuilderは引数なしでOKなはずです
        FlexBubble bubble =
                new FlexBubble.Builder().header(header).body(body).footer(footer).build();


        // 5. FlexMessageとして返す
        // altTextと中身(bubble)を渡します
        return new FlexMessage.Builder("正解！", bubble).build();
    }

    // 出題用のカードを作成するメソッド
    public static FlexMessage createQuestionMessage(String questionText) {

        // 1. ヘッダー（青色の帯）
        FlexText titleText = new FlexText.Builder().text("📝 MISSION 📝") // あるいは "QUESTION"
                .color("#FFFFFF").weight(FlexText.Weight.BOLD).align(FlexText.Align.CENTER).build();

        FlexBox header = new FlexBox.Builder(FlexBox.Layout.VERTICAL, List.of(titleText))
                .backgroundColor("#0055aa") // 落ち着いた青
                .build();

        // 2. 本文（問題文）
        FlexText questionBody = new FlexText.Builder().text(questionText).wrap(true).size("md")
                .color("#333333").build();

        FlexBox body = new FlexBox.Builder(FlexBox.Layout.VERTICAL, List.of(questionBody)).build();

        // 3. フッター（案内）
        FlexText infoText = new FlexText.Builder().text("※答えをメッセージで送信してください").size("xs")
                .color("#aaaaaa").align(FlexText.Align.CENTER).build();

        FlexBox footer = new FlexBox.Builder(FlexBox.Layout.VERTICAL, List.of(infoText)).build();

        // 4. 合体
        FlexBubble bubble =
                new FlexBubble.Builder().header(header).body(body).footer(footer).build();

        // 5. 返却
        return new FlexMessage.Builder("新しい問題です", bubble).build();
    }
}
