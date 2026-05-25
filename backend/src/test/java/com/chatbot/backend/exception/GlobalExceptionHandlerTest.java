package com.chatbot.backend.exception;

import com.chatbot.backend.dto.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
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
        assertThat(response.getBody().getErrorCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().getMessage())
            .isEqualTo("서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        assertThat(response.getBody().getMessage()).doesNotContain("Something went wrong");
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    @DisplayName("비즈니스 예외는 ErrorCode의 상태/식별자와 메시지를 ApiResponse 포맷으로 반환한다")
    void handleBusinessException_WhenBusinessError_ThenReturnsMappedStatus() {
        //given
        BusinessException exception = new SessionNotFoundException();

        //when
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(exception);

        //then
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().getCode()).isEqualTo(404);
        assertThat(response.getBody().getStatus()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().getErrorCode()).isEqualTo("SESSION_NOT_FOUND");
        assertThat(response.getBody().getMessage()).isEqualTo("세션을 찾을 수 없습니다.");
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    @DisplayName("DB 예외는 500 상태와 고정 메시지를 반환하고 원본 메시지를 노출하지 않는다")
    void handleDataAccess_WhenDbError_ThenHidesOriginalMessage() {
        //given
        DataAccessResourceFailureException exception =
            new DataAccessResourceFailureException("connection refused: jdbc:h2:mem");

        //when
        ResponseEntity<ApiResponse<Void>> response = handler.handleDataAccess(exception);

        //then
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getErrorCode()).isEqualTo("DATA_ACCESS_ERROR");
        assertThat(response.getBody().getMessage())
            .isEqualTo("데이터 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        assertThat(response.getBody().getMessage()).doesNotContain("jdbc");
        assertThat(response.getBody().getData()).isNull();
    }
}
