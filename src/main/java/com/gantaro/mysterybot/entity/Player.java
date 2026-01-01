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

}
