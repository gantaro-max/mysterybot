package com.gantaro.mysterybot.dto;

public record MasterRiddleRequest(String question, String answer, String nextMsg, String hintMsg,
        Integer imageId, String category) {
}
