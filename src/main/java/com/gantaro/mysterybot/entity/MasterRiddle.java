package com.gantaro.mysterybot.entity;

import lombok.Data;

@Data
public class MasterRiddle {

    private Integer id;
    private String question;
    private String answer;
    private String hintMsg;
    private String nextMsg;
    private Integer imageId;
    private String category;
    private String imageUuid;

}
