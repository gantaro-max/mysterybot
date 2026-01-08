package com.gantaro.mysterybot.entity;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TeamGroup {
    private String groupId;
    private String groupName;
    private String adminPass;
    private LocalDateTime createdAt;
    private Boolean isRandomOrder;
    private Timestamp startedAt;

}
