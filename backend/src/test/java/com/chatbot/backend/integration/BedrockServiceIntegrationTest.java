package com.chatbot.backend.integration;

import com.chatbot.backend.config.aws.ContentBlock;
import com.chatbot.backend.config.aws.ConversationRequest;
import com.chatbot.backend.config.aws.ConversationResponse;
import com.chatbot.backend.config.aws.Message;
import com.chatbot.backend.service.BedrockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;

@SpringBootTest
public class BedrockServiceIntegrationTest {

    @Autowired
    private BedrockService bedrockService;

    @Value("${aws.bedrock.model-id}")
    private String modelId;

    @Test
    @DisplayName("실제 Bedrock Converse API를 호출하여 응답과 토큰 사용량을 출력한다")
    void whenRealBedrockConverse_thenPrintResponseAndUsage() {
        System.out.println("모델 ID: " + modelId);
        System.out.println("메시지 전송 중...\n");

        ConversationRequest request = new ConversationRequest(
                Collections.singletonList(
                        new Message("user", Collections.singletonList(
                                new ContentBlock("안녕? 너는 누구야? 간단히 소개해줘.")))),
                null, null, null);

        ConversationResponse response = bedrockService.converse(request);

        System.out.println("응답: " + response.getContent().get(0).getText() + "\n");
        System.out.println("토큰 사용량:");
        System.out.println("  - 입력: " + response.getUsage().getInputTokens());
        System.out.println("  - 출력: " + response.getUsage().getOutputTokens());
        System.out.println("  - 종료 사유: " + response.getStopReason());
    }
}
