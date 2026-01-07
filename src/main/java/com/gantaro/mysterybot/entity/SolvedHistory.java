package com.gantaro.mysterybot.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SolvedHistory {
    private Integer id;
    private Integer playerId;
    private Integer riddleId;
    private LocalDateTime solvedAt;
}
