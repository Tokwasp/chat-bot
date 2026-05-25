package com.chatbot.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiResponse<T> {

    private final int code;
    private final String status;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String errorCode;

    private final String message;
    private final T data;

    private ApiResponse(int code, String status, String errorCode, String message, T data) {
        this.code = code;
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(HttpStatus.OK.value(), HttpStatus.OK.name(), null, HttpStatus.OK.name(), data);
    }

    public static <T> ApiResponse<T> error(HttpStatus httpStatus, String errorCode, String message) {
        return new ApiResponse<>(httpStatus.value(), httpStatus.name(), errorCode, message, null);
    }
}
