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
    private Integer imageId; // 内部紐付け用
    private String hintMsg;
    private String imageUuid; // 画像の公開用UUID

}
