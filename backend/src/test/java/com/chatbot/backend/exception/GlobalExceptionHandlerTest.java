package com.chatbot.backend.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("statusCode가 없는 일반 예외는 500 상태와 에러 메시지를 반환한다")
    void handleGeneric_WhenPlainException_ThenReturnsInternalServerError() {
        //given
        Exception exception = new RuntimeException("Something went wrong");

        //when
        ResponseEntity<ErrorResponse> response = handler.handleGeneric(exception);

        //then
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getError().getMessage()).isEqualTo("Something went wrong");
        assertThat(response.getBody().getError().getCode()).isNull();
    }

    @Test
    @DisplayName("커스텀 statusCode가 있는 예외는 해당 상태코드와 메시지/코드를 반환한다")
    void handleChatbotException_WhenCustomStatus_ThenReturnsThatStatus() {
        //given
        ChatbotException exception = new ChatbotException(
            "세션을 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value(), "NOT_FOUND");

        //when
        ResponseEntity<ErrorResponse> response = handler.handleChatbotException(exception);

        //then
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().getError().getMessage()).isEqualTo("세션을 찾을 수 없습니다.");
        assertThat(response.getBody().getError().getCode()).isEqualTo("NOT_FOUND");
    }
}
