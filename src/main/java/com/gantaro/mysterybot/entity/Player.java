package com.gantaro.mysterybot.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Player {
    private Integer id;
    private String lineUserId;
    private String groupId;
    private Integer currentStage;
    private LocalDateTime lastActive;
    private String playerName;
    private LocalDateTime startAt;
    private LocalDateTime finishedAt;
    private Integer currentRiddleId;

    // 経過時間を計算する便利メソッド（ランキング表示用）
    public String getClearTime() {
        if (startAt == null || finishedAt == null)
            return "-";
        long diff = java.time.Duration.between(startAt, finishedAt).toSeconds();
        long mm = diff / 60;
        long ss = diff % 60;
        return String.format("%02d:%02d", mm, ss);
    }

}
