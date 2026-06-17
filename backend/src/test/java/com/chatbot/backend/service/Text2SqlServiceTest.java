package com.chatbot.backend.service;

import com.chatbot.backend.config.aws.ContentBlock;
import com.chatbot.backend.config.aws.ConversationRequest;
import com.chatbot.backend.config.aws.ConversationResponse;
import com.chatbot.backend.config.aws.TokenUsage;
import com.chatbot.backend.dto.response.Text2SqlResponse;
import com.chatbot.backend.exception.LlmResponseParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Text2SqlServiceTest {

    @Mock
    private BedrockService bedrockService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private Text2SqlServiceImpl text2SqlService;

    @Test
    @DisplayName("질문을 받으면 의도 분류 → Entity 추출 → SQL 변환 순서로 Bedrock을 3번 호출하고 파싱된 결과를 반환한다")
    void whenQuestionGiven_thenChainsThreeConverseCallsAndReturnsParsedResult() {
        when(bedrockService.converse(any(ConversationRequest.class)))
            .thenReturn(response("{\"intent\": \"LIST_TRANSACTIONS\"}"))
            .thenReturn(response("```json\n{\"type\": null, \"category\": null, \"period_code\": \"LAST_1_MONTH\", \"keywords\": null}\n```"))
            .thenReturn(response("{\"sql\": \"SELECT * FROM transactions\", \"params\": [\"userId\"]}"));

        Text2SqlResponse result = text2SqlService.generate("지난달 거래 내역 보여줘");

        verify(bedrockService, times(3)).converse(any(ConversationRequest.class));
        assertThat(result.getIntent().get("intent").asText()).isEqualTo("LIST_TRANSACTIONS");
        // 코드펜스(```json)가 제거되고 객체로 파싱된다
        assertThat(result.getEntities().get("period_code").asText()).isEqualTo("LAST_1_MONTH");
        // sql 필드만 추출되어 문자열로 반환된다
        assertThat(result.getSql()).isEqualTo("SELECT * FROM transactions");
        assertThat(result.getParams()).containsExactly("userId");
    }

    @Test
    @DisplayName("각 단계마다 서로 다른 시스템 프롬프트가 사용된다")
    void whenChainRuns_thenEachStepUsesDistinctSystemPrompt() {
        when(bedrockService.converse(any(ConversationRequest.class)))
            .thenReturn(response("{\"intent\": \"UNKNOWN\"}"))
            .thenReturn(response("{\"type\": null}"))
            .thenReturn(response("{\"sql\": \"SELECT 1\", \"params\": []}"));

        text2SqlService.generate("질문");

        ArgumentCaptor<ConversationRequest> captor = ArgumentCaptor.forClass(ConversationRequest.class);
        verify(bedrockService, times(3)).converse(captor.capture());

        List<ConversationRequest> requests = captor.getAllValues();
        String intentPrompt = requests.get(0).getSystemPrompt();
        String entityPrompt = requests.get(1).getSystemPrompt();
        String sqlPrompt = requests.get(2).getSystemPrompt();

        assertThat(intentPrompt).isNotBlank();
        assertThat(entityPrompt).isNotBlank();
        assertThat(sqlPrompt).isNotBlank();
        assertThat(intentPrompt).isNotEqualTo(entityPrompt);
        assertThat(entityPrompt).isNotEqualTo(sqlPrompt);
        assertThat(intentPrompt).isNotEqualTo(sqlPrompt);
    }

    @Test
    @DisplayName("Bedrock 응답에 콘텐츠가 없으면 LLM 응답 파싱 예외가 발생한다")
    void whenResponseHasNoContent_thenThrowsParseException() {
        when(bedrockService.converse(any(ConversationRequest.class)))
            .thenReturn(new ConversationResponse(List.of(), "end_turn", new TokenUsage(0, 0)));

        assertThatThrownBy(() -> text2SqlService.generate("질문"))
            .isInstanceOf(LlmResponseParseException.class);
    }

    private ConversationResponse response(String text) {
        return new ConversationResponse(
            List.of(new ContentBlock(text)), "end_turn", new TokenUsage(1, 1));
    }
}
