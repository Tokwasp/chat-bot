package com.chatbot.backend.integration;

import com.chatbot.backend.config.aws.*;
import com.chatbot.backend.service.BedrockService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StringUtils;

import java.util.Collections;

@Slf4j
@Tag("integration")
@SpringBootTest
public class StreamIntegrationTest {

    @Autowired
    private BedrockService bedrockService;

    @Value("${aws.bedrock.model-id}")
    private String modelId;

    @Test
    @DisplayName("실제 Bedrock ConverseStream API를 호출하여 텍스트 델타와 토큰 사용량을 출력한다")
    void whenRealBedrockConverseStream_thenPrintTextDeltasAndUsage() {
        log.info("모델 ID: {}", modelId);
        log.info("스트리밍 응답 테스트 시작");

        ConversationRequest request = new ConversationRequest(
                Collections.singletonList(
                        new Message("user", Collections.singletonList(
                                new ContentBlock("1부터 5까지 세어줘. 짧게.")))),
                null, null, null);

        bedrockService.converseStream(request, this::handleStreamEvent);
    }

    private void handleStreamEvent(StreamEvent event) {
        if (event instanceof TextDeltaEvent) {
            TextDeltaEvent textEvent = (TextDeltaEvent) event;
            if (StringUtils.hasText(textEvent.getText())) {
                log.info("[text_delta] {}", textEvent.getText());
            }
            return;
        }

        if (event instanceof MessageStopEvent) {
            MessageStopEvent stopEvent = (MessageStopEvent) event;
            if (stopEvent.getUsage() != null) {
                log.info("토큰 사용량 - 입력: {}, 출력: {}",
                        stopEvent.getUsage().getInputTokens(),
                        stopEvent.getUsage().getOutputTokens());
            }
        }
    }
}
