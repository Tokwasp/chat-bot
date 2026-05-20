package com.chatbot.backend.integration;

import com.chatbot.backend.repository.MessageRepository;
import com.chatbot.backend.repository.SessionRepository;
import com.chatbot.backend.service.MessageHistoryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChatIntegrationTest {

    private static final long HISTORY_AWAIT_TIMEOUT_MS = 5_000L;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MessageHistoryService messageHistoryService;

    @Autowired
    SessionRepository sessionRepository;

    @Autowired
    MessageRepository messageRepository;

    @MockitoBean
    BedrockRuntimeAsyncClient bedrockRuntimeClient;

    @BeforeEach
    void cleanUp() {
        messageRepository.deleteAllInBatch();
        sessionRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("세션 생성 후 메시지를 보내면 SSE 스트리밍 응답을 받고 히스토리에 user/assistant 메시지가 저장된다")
    void chat_WhenMessageSent_ThenStreamsSseAndPersistsHistory() throws Exception {
        //given
        mockStreaming(Arrays.asList(
            textDeltaOutput("안녕"),
            textDeltaOutput("하세요"),
            messageStopOutput(StopReason.END_TURN),
            metadataOutput(10, 5)));
        String sessionId = createSession();

        //when
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(chatBody(sessionId, "안녕")))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString(MediaType.TEXT_EVENT_STREAM_VALUE)));
        awaitHistorySize(sessionId, 2);

        //then
        mockMvc.perform(get("/api/sessions/{id}/messages", sessionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].role").value("user"))
            .andExpect(jsonPath("$[0].content").value("안녕"))
            .andExpect(jsonPath("$[1].role").value("assistant"))
            .andExpect(jsonPath("$[1].content").value("안녕하세요"));
    }

    @Test
    @DisplayName("같은 세션에서 여러 메시지를 주고받으면 히스토리에 user/assistant가 순서대로 4개 유지된다")
    void chat_WhenMultipleMessages_ThenHistoryKeepsConversationOrder() throws Exception {
        //given
        mockStreaming(Arrays.asList(
            textDeltaOutput("응답"),
            messageStopOutput(StopReason.END_TURN),
            metadataOutput(8, 3)));
        String sessionId = createSession();

        //when
        sendMessage(sessionId, "첫 질문");
        awaitHistorySize(sessionId, 2);
        sendMessage(sessionId, "둘째 질문");
        awaitHistorySize(sessionId, 4);

        //then
        mockMvc.perform(get("/api/sessions/{id}/messages", sessionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(4)))
            .andExpect(jsonPath("$[0].role").value("user"))
            .andExpect(jsonPath("$[0].content").value("첫 질문"))
            .andExpect(jsonPath("$[1].role").value("assistant"))
            .andExpect(jsonPath("$[2].role").value("user"))
            .andExpect(jsonPath("$[2].content").value("둘째 질문"))
            .andExpect(jsonPath("$[3].role").value("assistant"));
    }

    private String createSession() throws Exception {
        String response = mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Integration Test Chat\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(response);
        return node.get("id").asText();
    }

    private void sendMessage(String sessionId, String message) throws Exception {
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(chatBody(sessionId, message)))
            .andExpect(status().isOk());
    }

    private String chatBody(String sessionId, String message) {
        return "{\"sessionId\":\"" + sessionId + "\",\"message\":\"" + message + "\"}";
    }

    private void awaitHistorySize(String sessionId, int expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + HISTORY_AWAIT_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (messageHistoryService.get(sessionId).size() >= expected) {
                return;
            }
            Thread.sleep(50);
        }

        assertThat(messageHistoryService.get(sessionId)).hasSize(expected);
    }

    private void mockStreaming(List<ConverseStreamOutput> events) {
        doAnswer(invocation -> {
            ConverseStreamResponseHandler handler = invocation.getArgument(1);
            handler.onEventStream(subscriber -> subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    for (ConverseStreamOutput event : events) {
                        subscriber.onNext(event);
                    }
                    subscriber.onComplete();
                }

                @Override
                public void cancel() {
                }
            }));
            handler.complete();
            return CompletableFuture.completedFuture(null);
        }).when(bedrockRuntimeClient).converseStream(any(ConverseStreamRequest.class), any(ConverseStreamResponseHandler.class));
    }

    private ConverseStreamOutput textDeltaOutput(String text) {
        return ConverseStreamOutput.contentBlockDeltaBuilder()
            .delta(ContentBlockDelta.fromText(text))
            .contentBlockIndex(0).build();
    }

    private ConverseStreamOutput messageStopOutput(StopReason stopReason) {
        return ConverseStreamOutput.messageStopBuilder()
            .stopReason(stopReason).build();
    }

    private ConverseStreamOutput metadataOutput(int inputTokens, int outputTokens) {
        return ConverseStreamOutput.metadataBuilder()
            .usage(TokenUsage.builder()
                .inputTokens(inputTokens).outputTokens(outputTokens)
                .totalTokens(inputTokens + outputTokens).build())
            .build();
    }
}
