package com.gantaro.mysterybot.entity;

import lombok.Data;

@Data
public class Riddle {
    private Integer id;
    private String groupId;
    private Integer stageNo;
    private String question;
    private String answer;
    private String nextMsg;

}
