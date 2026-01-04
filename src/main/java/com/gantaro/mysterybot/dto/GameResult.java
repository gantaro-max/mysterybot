package com.gantaro.mysterybot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GameResult {
    // 結果の状態を表すラベル
    public enum Status {
        SUCCESS, // 正解（Flex Messageを送る）
        FAILURE, // 不正解（文字だけ）
        TEXT_ONLY // その他エラーなど（文字だけ）
    }

    private Status status;
    private String mainText; // ストーリー文や返信メッセージ
    private String subText; // 次の問題文（正解時のみ使用）
}
