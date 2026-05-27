package com.chatbot.backend.service;

import com.chatbot.backend.config.aws.ContentBlock;
import com.chatbot.backend.config.aws.ConversationRequest;
import com.chatbot.backend.config.aws.ConversationResponse;
import com.chatbot.backend.config.aws.TokenUsage;
import com.chatbot.backend.dto.response.Text2SqlResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Text2SqlServiceTest {

    @Mock
    private BedrockService bedrockService;

    @InjectMocks
    private Text2SqlServiceImpl text2SqlService;

    @Test
    @DisplayName("질문을 받으면 의도 분류 → Entity 추출 → SQL 변환 순서로 Bedrock을 3번 호출하고 각 단계 결과를 반환한다")
    void whenQuestionGiven_thenChainsThreeConverseCallsAndReturnsEachStep() {
        when(bedrockService.converse(any(ConversationRequest.class)))
            .thenReturn(response("거래내역 조회"))
            .thenReturn(response("기간: 지난달"))
            .thenReturn(response("SELECT * FROM transactions"));

        Text2SqlResponse result = text2SqlService.generate("지난달 거래 내역 보여줘");

        verify(bedrockService, times(3)).converse(any(ConversationRequest.class));
        assertThat(result.getIntent()).isEqualTo("거래내역 조회");
        assertThat(result.getEntities()).isEqualTo("기간: 지난달");
        assertThat(result.getSql()).isEqualTo("SELECT * FROM transactions");
    }

    @Test
    @DisplayName("각 단계마다 서로 다른 시스템 프롬프트가 사용된다")
    void whenChainRuns_thenEachStepUsesDistinctSystemPrompt() {
        when(bedrockService.converse(any(ConversationRequest.class)))
            .thenReturn(response("intent"))
            .thenReturn(response("entities"))
            .thenReturn(response("sql"));

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
    @DisplayName("Bedrock 응답에 콘텐츠가 없으면 해당 단계 결과는 빈 문자열이 된다")
    void whenResponseHasNoContent_thenStepResultIsEmpty() {
        when(bedrockService.converse(any(ConversationRequest.class)))
            .thenReturn(new ConversationResponse(List.of(), "end_turn", new TokenUsage(0, 0)));

        Text2SqlResponse result = text2SqlService.generate("질문");

        assertThat(result.getIntent()).isEmpty();
        assertThat(result.getEntities()).isEmpty();
        assertThat(result.getSql()).isEmpty();
    }

    private ConversationResponse response(String text) {
        return new ConversationResponse(
            List.of(new ContentBlock(text)), "end_turn", new TokenUsage(1, 1));
    }
}
