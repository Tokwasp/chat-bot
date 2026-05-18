package com.chatbot.backend.service;

import com.chatbot.backend.config.aws.ContentBlock;
import com.chatbot.backend.config.aws.ConversationRequest;
import com.chatbot.backend.config.aws.ConversationResponse;
import com.chatbot.backend.config.aws.Message;
import com.chatbot.backend.config.aws.MessageStopEvent;
import com.chatbot.backend.config.aws.StreamEvent;
import com.chatbot.backend.config.aws.TextDeltaEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
public class BedrockServiceTest {

    @Autowired
    private BedrockService bedrockService;

    @MockitoBean
    private BedrockRuntimeAsyncClient bedrockRuntimeClient;

    @Test
    @DisplayName("BedrockService 빈이 스프링 컨텍스트에 정상 등록된다")
    void whenContextLoads_thenBedrockServiceBeanIsRegistered() {
        assertThat(bedrockService).isNotNull();
    }

    @Test
    @DisplayName("메시지를 전송하면 Bedrock Converse API를 호출하고 응답 내용과 토큰 사용량을 반환한다")
    void whenMessageSent_thenReturnResponseWithContentAndTokenUsage() {
        when(bedrockRuntimeClient.converse(any(ConverseRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(buildConverseResponse("안녕하세요! 무엇을 도와드릴까요?", 10, 15)));

        ConversationResponse response = bedrockService.converse(buildRequest("안녕하세요"));

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getText()).isEqualTo("안녕하세요! 무엇을 도와드릴까요?");
        assertThat(response.getStopReason()).isEqualTo("end_turn");
        assertThat(response.getUsage().getInputTokens()).isEqualTo(10);
        assertThat(response.getUsage().getOutputTokens()).isEqualTo(15);
    }

    @Test
    @DisplayName("시스템 프롬프트를 포함한 요청을 전송하면 응답을 반환한다")
    void whenSystemPromptProvided_thenReturnResponse() {
        when(bedrockRuntimeClient.converse(any(ConverseRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(buildConverseResponse("저는 친절한 AI입니다.", 20, 10)));

        ConversationResponse response = bedrockService.converse(
            buildRequest("당신은 누구인가요?", "당신은 친절한 AI 어시스턴트입니다."));

        assertThat(response.getContent().get(0).getText()).isEqualTo("저는 친절한 AI입니다.");
    }

    @Test
    @DisplayName("Bedrock API 호출이 실패하면 예외를 던진다")
    void whenApiCallFails_thenThrowException() {
        when(bedrockRuntimeClient.converse(any(ConverseRequest.class)))
            .thenThrow(new RuntimeException("API Error"));

        assertThatThrownBy(() -> bedrockService.converse(buildRequest("안녕")))
            .hasMessageContaining("API Error");
    }

    @Test
    @DisplayName("스트리밍 요청 시 텍스트 델타 이벤트 3개와 완료 이벤트 1개가 순서대로 수신된다")
    void whenStreamingRequested_thenReceiveTextDeltaEventsAndCompleteEvent() throws Exception {
        mockStreaming(Arrays.asList(
            textDeltaOutput("안녕"),
            textDeltaOutput("하세요"),
            textDeltaOutput("!"),
            messageStopOutput(StopReason.END_TURN),
            metadataOutput(10, 5)
        ));

        List<StreamEvent> events = collectStreamEvents(buildRequest("안녕"));

        List<StreamEvent> textEvents = filterByType(events, "text_delta");
        assertThat(textEvents).hasSize(3);
        assertThat(((TextDeltaEvent) textEvents.get(0)).getText()).isEqualTo("안녕");
        assertThat(((TextDeltaEvent) textEvents.get(1)).getText()).isEqualTo("하세요");
        assertThat(((TextDeltaEvent) textEvents.get(2)).getText()).isEqualTo("!");

        MessageStopEvent completeEvent = (MessageStopEvent) findFirstByType(events, "message_complete");
        assertThat(completeEvent).isNotNull();
        assertThat(completeEvent.getUsage().getInputTokens()).isEqualTo(10);
        assertThat(completeEvent.getUsage().getOutputTokens()).isEqualTo(5);
    }

    @Test
    @DisplayName("스트리밍 중 tool_use 응답을 받으면 도구를 실행하고 결과를 포함한 최종 응답을 반환한다")
    void whenToolUseResponseReceived_thenExecuteToolAndReturnFinalResponse() throws Exception {
        ToolOrchestrator mockOrchestrator = mock(ToolOrchestrator.class);
        when(mockOrchestrator.executeTool("get_current_time", "{\"timezone\":\"Asia/Seoul\"}"))
            .thenReturn("2026-02-23 15:30:00");
        bedrockService.setToolOrchestrator(mockOrchestrator);

        mockStreamingWithTwoResponses(
            Arrays.asList(
                toolUseStartOutput("t1", "get_current_time"),
                toolUseInputOutput("{\"timezone\":\"Asia/Seoul\"}"),
                messageStopOutput(StopReason.TOOL_USE)
            ),
            Arrays.asList(
                textDeltaOutput("현재 시간은 15:30입니다."),
                messageStopOutput(StopReason.END_TURN),
                metadataOutput(20, 10)
            )
        );

        List<StreamEvent> events = collectStreamEvents(buildRequest("지금 몇 시야?"));

        assertThat(findFirstByType(events, "tool_use_start")).isNotNull();
        assertThat(findFirstByType(events, "tool_result")).isNotNull();

        List<StreamEvent> textEvents = filterByType(events, "text_delta");
        assertThat(textEvents).hasSize(1);
        assertThat(((TextDeltaEvent) textEvents.get(0)).getText()).isEqualTo("현재 시간은 15:30입니다.");

        verify(bedrockRuntimeClient, times(2)).converseStream(
            any(ConverseStreamRequest.class), any(ConverseStreamResponseHandler.class));
    }

    @Test
    @DisplayName("ToolOrchestrator가 설정된 경우 Bedrock 스트리밍 요청에 toolConfig가 포함된다")
    void whenOrchestratorConfigured_thenToolConfigIncludedInRequest() throws Exception {
        bedrockService.setToolOrchestrator(mock(ToolOrchestrator.class));

        ArgumentCaptor<ConverseStreamRequest> requestCaptor = ArgumentCaptor.forClass(ConverseStreamRequest.class);
        mockStreamingWithCaptor(Arrays.asList(
            textDeltaOutput("응답"),
            messageStopOutput(StopReason.END_TURN),
            metadataOutput(5, 3)
        ), requestCaptor);

        collectStreamEvents(buildRequest("안녕"));

        assertThat(requestCaptor.getValue().toolConfig()).isNotNull();
    }

    @Test
    @DisplayName("Bedrock에서 AccessDeniedException이 발생하면 retryable이 false인 BedrockServiceError(ACCESS_DENIED)를 던진다")
    void whenAccessDeniedException_thenThrowNonRetryableBedrockServiceError() {
        when(bedrockRuntimeClient.converse(any(ConverseRequest.class)))
            .thenThrow(AccessDeniedException.builder().message("Access denied").build());

        assertThatThrownBy(() -> bedrockService.converse(buildRequest("안녕")))
            .isInstanceOf(BedrockServiceError.class)
            .hasMessage("ACCESS_DENIED")
            .satisfies(e -> assertThat(((BedrockServiceError) e).isRetryable()).isFalse());
    }

    @Test
    @DisplayName("Bedrock에서 ResourceNotFoundException이 발생하면 retryable이 false인 BedrockServiceError(MODEL_NOT_FOUND)를 던진다")
    void whenResourceNotFoundException_thenThrowNonRetryableBedrockServiceError() {
        when(bedrockRuntimeClient.converse(any(ConverseRequest.class)))
            .thenThrow(ResourceNotFoundException.builder().message("Model not found").build());

        assertThatThrownBy(() -> bedrockService.converse(buildRequest("안녕")))
            .isInstanceOf(BedrockServiceError.class)
            .hasMessage("MODEL_NOT_FOUND")
            .satisfies(e -> assertThat(((BedrockServiceError) e).isRetryable()).isFalse());
    }

    @Test
    @DisplayName("Bedrock에서 ThrottlingException이 발생하면 retryable이 true인 BedrockServiceError(THROTTLING)를 던진다")
    void whenThrottlingException_thenThrowRetryableBedrockServiceError() {
        when(bedrockRuntimeClient.converse(any(ConverseRequest.class)))
            .thenThrow(ThrottlingException.builder().message("Rate exceeded").build());

        assertThatThrownBy(() -> bedrockService.converse(buildRequest("안녕")))
            .isInstanceOf(BedrockServiceError.class)
            .hasMessage("THROTTLING")
            .satisfies(e -> assertThat(((BedrockServiceError) e).isRetryable()).isTrue());
    }

    private ConversationRequest buildRequest(String userText) {
        return new ConversationRequest(
            Arrays.asList(new Message("user", Arrays.asList(new ContentBlock(userText)))),
            null, null, null
        );
    }

    private ConversationRequest buildRequest(String userText, String systemPrompt) {
        return new ConversationRequest(
            Arrays.asList(new Message("user", Arrays.asList(new ContentBlock(userText)))),
            systemPrompt, null, null
        );
    }

    private ConverseResponse buildConverseResponse(String text, int inputTokens, int outputTokens) {
        software.amazon.awssdk.services.bedrockruntime.model.Message sdkMessage =
            software.amazon.awssdk.services.bedrockruntime.model.Message.builder()
                .role(ConversationRole.ASSISTANT)
                .content(software.amazon.awssdk.services.bedrockruntime.model.ContentBlock.builder()
                    .text(text).build())
                .build();
        return ConverseResponse.builder()
            .output(ConverseOutput.builder().message(sdkMessage).build())
            .stopReason(StopReason.END_TURN)
            .usage(software.amazon.awssdk.services.bedrockruntime.model.TokenUsage.builder()
                .inputTokens(inputTokens).outputTokens(outputTokens)
                .totalTokens(inputTokens + outputTokens).build())
            .build();
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
            .usage(software.amazon.awssdk.services.bedrockruntime.model.TokenUsage.builder()
                .inputTokens(inputTokens).outputTokens(outputTokens)
                .totalTokens(inputTokens + outputTokens).build())
            .build();
    }

    private ConverseStreamOutput toolUseStartOutput(String toolUseId, String toolName) {
        return ConverseStreamOutput.contentBlockStartBuilder()
            .start(ContentBlockStart.fromToolUse(ToolUseBlockStart.builder()
                .toolUseId(toolUseId).name(toolName).build()))
            .contentBlockIndex(0).build();
    }

    private ConverseStreamOutput toolUseInputOutput(String input) {
        return ConverseStreamOutput.contentBlockDeltaBuilder()
            .delta(ContentBlockDelta.fromToolUse(ToolUseBlockDelta.builder().input(input).build()))
            .contentBlockIndex(0).build();
    }

    private void mockStreaming(List<ConverseStreamOutput> events) {
        doAnswer(invocation -> {
            ConverseStreamResponseHandler handler = invocation.getArgument(1);
            handler.onEventStream(subscriber -> subscriber.onSubscribe(new org.reactivestreams.Subscription() {
                @Override
                public void request(long n) {
                    for (ConverseStreamOutput e : events) subscriber.onNext(e);
                    subscriber.onComplete();
                }
                @Override
                public void cancel() {}
            }));
            handler.complete();
            return CompletableFuture.completedFuture(null);
        }).when(bedrockRuntimeClient).converseStream(any(ConverseStreamRequest.class), any(ConverseStreamResponseHandler.class));
    }

    private void mockStreamingWithTwoResponses(List<ConverseStreamOutput> first, List<ConverseStreamOutput> second) {
        AtomicInteger callCount = new AtomicInteger(0);
        doAnswer(invocation -> {
            List<ConverseStreamOutput> stream = callCount.getAndIncrement() == 0 ? first : second;
            ConverseStreamResponseHandler handler = invocation.getArgument(1);
            handler.onEventStream(subscriber -> subscriber.onSubscribe(new org.reactivestreams.Subscription() {
                @Override
                public void request(long n) {
                    for (ConverseStreamOutput e : stream) subscriber.onNext(e);
                    subscriber.onComplete();
                }
                @Override
                public void cancel() {}
            }));
            handler.complete();
            return CompletableFuture.completedFuture(null);
        }).when(bedrockRuntimeClient).converseStream(any(ConverseStreamRequest.class), any(ConverseStreamResponseHandler.class));
    }

    private void mockStreamingWithCaptor(List<ConverseStreamOutput> events, ArgumentCaptor<ConverseStreamRequest> captor) {
        doAnswer(invocation -> {
            ConverseStreamResponseHandler handler = invocation.getArgument(1);
            handler.onEventStream(subscriber -> subscriber.onSubscribe(new org.reactivestreams.Subscription() {
                @Override
                public void request(long n) {
                    for (ConverseStreamOutput e : events) subscriber.onNext(e);
                    subscriber.onComplete();
                }
                @Override
                public void cancel() {}
            }));
            handler.complete();
            return CompletableFuture.completedFuture(null);
        }).when(bedrockRuntimeClient).converseStream(captor.capture(), any(ConverseStreamResponseHandler.class));
    }

    private List<StreamEvent> collectStreamEvents(ConversationRequest request) throws Exception {
        List<StreamEvent> events = new ArrayList<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> bedrockService.converseStream(request, events::add));
        future.get(5, TimeUnit.SECONDS);
        return events;
    }

    private List<StreamEvent> filterByType(List<StreamEvent> events, String type) {
        List<StreamEvent> result = new ArrayList<>();
        for (StreamEvent e : events) {
            if (type.equals(e.getType())) result.add(e);
        }
        return result;
    }

    private StreamEvent findFirstByType(List<StreamEvent> events, String type) {
        for (StreamEvent e : events) {
            if (type.equals(e.getType())) return e;
        }
        return null;
    }
}
