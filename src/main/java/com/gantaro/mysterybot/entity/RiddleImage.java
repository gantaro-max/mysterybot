package com.gantaro.mysterybot.entity;

import lombok.Data;

@Data
public class RiddleImage {

    private Integer id;
    private String uuid;
    private byte[] data;
    private String mimeType;

}
