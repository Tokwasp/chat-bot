package com.chatbot.backend.exception;

import com.chatbot.backend.dto.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("일반 예외는 500 상태와 내부 정보를 감춘 한국어 메시지를 ApiResponse 포맷으로 반환한다")
    void handleGeneric_WhenPlainException_ThenReturnsInternalServerError() {
        //given
        Exception exception = new RuntimeException("Something went wrong");

        //when
        ResponseEntity<ApiResponse<Void>> response = handler.handleGeneric(exception);

        //then
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getCode()).isEqualTo(500);
        assertThat(response.getBody().getStatus()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().getMessage())
            .isEqualTo("서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        assertThat(response.getBody().getMessage()).doesNotContain("Something went wrong");
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    @DisplayName("커스텀 statusCode가 있는 예외는 해당 상태코드와 메시지를 ApiResponse 포맷으로 반환한다")
    void handleChatbotException_WhenCustomStatus_ThenReturnsThatStatus() {
        //given
        ChatbotException exception = new ChatbotException(
            "세션을 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value(), "SESSION_NOT_FOUND");

        //when
        ResponseEntity<ApiResponse<Void>> response = handler.handleChatbotException(exception);

        //then
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().getCode()).isEqualTo(404);
        assertThat(response.getBody().getStatus()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().getMessage()).isEqualTo("세션을 찾을 수 없습니다.");
        assertThat(response.getBody().getData()).isNull();
    }
}
