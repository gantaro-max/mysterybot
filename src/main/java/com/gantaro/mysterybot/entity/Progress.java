package com.gantaro.mysterybot.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Progress {
    private Integer id;
    private Integer playerId;
    private Integer riddleId;
    private Boolean isCleared;
    private LocalDateTime clearedAt;

}
