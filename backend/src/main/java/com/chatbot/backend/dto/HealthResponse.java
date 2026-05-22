package com.chatbot.backend.dto;

import lombok.Getter;

@Getter
public class HealthResponse {

    private static final String STATUS_OK = "ok";

    private final String status;

    private HealthResponse(String status) {
        this.status = status;
    }

    public static HealthResponse ok() {
        return new HealthResponse(STATUS_OK);
    }
}
