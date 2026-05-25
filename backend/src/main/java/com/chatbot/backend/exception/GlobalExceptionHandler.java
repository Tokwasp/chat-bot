package com.chatbot.backend.exception;

import com.chatbot.backend.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.info("Business error [{}]: {}", errorCode.name(), e.getMessage());
        return build(errorCode.getStatus(), errorCode.name(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ErrorCode.INVALID_REQUEST.getMessage());
        return build(ErrorCode.INVALID_REQUEST.getStatus(), ErrorCode.INVALID_REQUEST.name(), message);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccess(DataAccessException e) {
        log.error("Data access error", e);
        ErrorCode errorCode = ErrorCode.DATA_ACCESS_ERROR;
        return build(errorCode.getStatus(), errorCode.name(), errorCode.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception e) {
        log.error("Unhandled exception", e);
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        return build(errorCode.getStatus(), errorCode.name(), errorCode.getMessage());
    }

    private ResponseEntity<ApiResponse<Void>> build(HttpStatus status, String errorCode, String message) {
        return ResponseEntity.status(status)
                .body(ApiResponse.error(status, errorCode, message));
    }
}
