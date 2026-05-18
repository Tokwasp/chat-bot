package com.chatbot.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ChatbotException.class)
    public ResponseEntity<ErrorResponse> handleChatbotException(ChatbotException e) {
        log.error(e.getMessage(), e);
        return ResponseEntity.status(e.getStatusCode())
                .body(new ErrorResponse(new ErrorResponse.ErrorBody(e.getMessage(), e.getCode())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception e) {
        log.error(e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(new ErrorResponse.ErrorBody(e.getMessage(), null)));
    }
}
