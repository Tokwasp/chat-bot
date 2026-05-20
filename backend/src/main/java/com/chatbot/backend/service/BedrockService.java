package com.chatbot.backend.service;

import com.chatbot.backend.config.aws.*;
import com.chatbot.backend.config.aws.TokenUsage;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.MessageStopEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class BedrockService {

    private static final int DEFAULT_MAX_TOKENS = 4096;

    private final BedrockRuntimeAsyncClient bedrockRuntimeClient;
    private final String modelId;
    private ToolOrchestrator toolOrchestrator;

    public BedrockService(BedrockRuntimeAsyncClient bedrockRuntimeClient,
                          @Value("${aws.bedrock.model-id}") String modelId) {
        this.bedrockRuntimeClient = bedrockRuntimeClient;
        this.modelId = modelId;
    }

    public void setToolOrchestrator(ToolOrchestrator toolOrchestrator) {
        this.toolOrchestrator = toolOrchestrator;
    }

    public ConversationResponse converse(ConversationRequest request) {
        ConverseRequest req = buildConverseRequest(request);
        try {
            ConverseResponse response = bedrockRuntimeClient.converse(req).join();
            return toConversationResponse(response);
        } catch (AccessDeniedException e) {
            throw new BedrockServiceError("ACCESS_DENIED");
        } catch (ResourceNotFoundException e) {
            throw new BedrockServiceError("MODEL_NOT_FOUND");
        } catch (ThrottlingException e) {
            throw new BedrockServiceError("THROTTLING", true);
        }
    }

    public void converseStream(ConversationRequest request, Consumer<StreamEvent> eventConsumer) {
        List<Message> messages = new ArrayList<>(toSdkMessages(request.getMessages()));

        while (true) {
            ConverseStreamRequest streamReq = buildConverseStreamRequest(request, messages);
            StreamVisitor visitor = new StreamVisitor(eventConsumer);

            ConverseStreamResponseHandler handler = ConverseStreamResponseHandler.builder()
                    .subscriber(() -> new StreamSubscriber(visitor, eventConsumer))
                    .onError(t -> eventConsumer.accept(new ErrorEvent(t.getMessage())))
                    .build();

            bedrockRuntimeClient.converseStream(streamReq, handler).join();

            if (!"tool_use".equals(visitor.stopReason)
                    || !StringUtils.hasLength(visitor.currentToolUseId)
                    || toolOrchestrator == null) {
                break;
            }

            String toolResult = toolOrchestrator.executeTool(
                    visitor.currentToolName, visitor.toolInputJson.toString());
            eventConsumer.accept(new ToolResultEvent(visitor.currentToolUseId, toolResult));

            messages.add(Message.builder()
                    .role(ConversationRole.ASSISTANT)
                    .content(ContentBlock.fromToolUse(
                            ToolUseBlock.builder()
                                    .toolUseId(visitor.currentToolUseId)
                                    .name(visitor.currentToolName)
                                    .input(Document.fromMap(Collections.emptyMap()))
                                    .build()))
                    .build());

            messages.add(Message.builder()
                    .role(ConversationRole.USER)
                    .content(ContentBlock.fromToolResult(
                            ToolResultBlock.builder()
                                    .toolUseId(visitor.currentToolUseId)
                                    .content(ToolResultContentBlock.fromText(toolResult))
                                    .build()))
                    .build());
        }
    }

    private ConverseRequest buildConverseRequest(ConversationRequest request) {
        ConverseRequest.Builder builder = ConverseRequest.builder()
                .modelId(modelId)
                .messages(toSdkMessages(request.getMessages()))
                .inferenceConfig(buildInferenceConfig(request));

        applySystemPrompt(request, builder::system);
        return builder.build();
    }

    private ConverseStreamRequest buildConverseStreamRequest(
            ConversationRequest request,
            List<Message> messages) {
        ConverseStreamRequest.Builder builder = ConverseStreamRequest.builder()
                .modelId(modelId)
                .messages(messages)
                .inferenceConfig(buildInferenceConfig(request));

        applySystemPrompt(request, builder::system);

        if (toolOrchestrator != null) {
            List<Tool> tools = Optional.ofNullable(toolOrchestrator.getToolDefinitions())
                    .orElse(Collections.emptyList());
            builder.toolConfig(ToolConfiguration.builder()
                    .tools(tools)
                    .build());
        }
        return builder.build();
    }

    private InferenceConfiguration buildInferenceConfig(ConversationRequest request) {
        InferenceConfiguration.Builder ic = InferenceConfiguration.builder()
                .maxTokens(Optional.ofNullable(request.getMaxTokens()).orElse(DEFAULT_MAX_TOKENS));

        if (request.getTemperature() != null) {
            ic.temperature(request.getTemperature().floatValue());
        }
        return ic.build();
    }

    private void applySystemPrompt(ConversationRequest request, Consumer<SystemContentBlock> systemSetter) {
        if (!StringUtils.hasText(request.getSystemPrompt())) {
            return;
        }

        systemSetter.accept(SystemContentBlock.builder()
                .text(request.getSystemPrompt())
                .build());
    }

    private List<Message> toSdkMessages(List<com.chatbot.backend.config.aws.Message> messages) {
        List<Message> result = new ArrayList<>();
        for (com.chatbot.backend.config.aws.Message msg : messages) {
            List<ContentBlock> blocks = msg.getContent().stream()
                    .map(block -> ContentBlock.fromText(block.getText()))
                    .collect(Collectors.toList());

            ConversationRole role = "assistant".equalsIgnoreCase(msg.getRole())
                    ? ConversationRole.ASSISTANT
                    : ConversationRole.USER;
            result.add(Message.builder()
                    .role(role)
                    .content(blocks)
                    .build());
        }
        return result;
    }

    private ConversationResponse toConversationResponse(ConverseResponse response) {
        List<ContentBlock> rawBlocks = Optional.ofNullable(response.output())
                .map(ConverseOutput::message)
                .map(Message::content)
                .orElse(Collections.emptyList());

        List<com.chatbot.backend.config.aws.ContentBlock> content = rawBlocks.stream()
                .filter(block -> StringUtils.hasText(block.text()))
                .map(block -> new com.chatbot.backend.config.aws.ContentBlock(block.text()))
                .collect(Collectors.toList());

        int inputTokens = Optional.ofNullable(response.usage())
                .map(u -> u.inputTokens())
                .orElse(0);

        int outputTokens = Optional.ofNullable(response.usage())
                .map(u -> u.outputTokens())
                .orElse(0);
        return new ConversationResponse(content, response.stopReasonAsString(), new TokenUsage(inputTokens, outputTokens));
    }

    private static class StreamVisitor implements ConverseStreamResponseHandler.Visitor {
        String currentToolUseId;
        String currentToolName;
        String stopReason;
        final StringBuilder toolInputJson = new StringBuilder();
        private final Consumer<StreamEvent> eventConsumer;

        StreamVisitor(Consumer<StreamEvent> eventConsumer) {
            this.eventConsumer = eventConsumer;
        }

        @Override
        public void visitContentBlockStart(ContentBlockStartEvent event) {
            Optional.ofNullable(event.start())
                    .map(ContentBlockStart::toolUse)
                    .ifPresent(this::onToolUseStart);
        }

        private void onToolUseStart(ToolUseBlockStart tu) {
            currentToolUseId = tu.toolUseId();
            currentToolName = tu.name();
            toolInputJson.setLength(0);
            eventConsumer.accept(new ToolUseStartEvent(tu.toolUseId(), tu.name()));
        }

        @Override
        public void visitContentBlockDelta(ContentBlockDeltaEvent event) {
            ContentBlockDelta delta = event.delta();
            if (delta == null) {
                return;
            }
            if (delta.toolUse() != null && StringUtils.hasLength(delta.toolUse().input())) {
                toolInputJson.append(delta.toolUse().input());
                return;
            }
            if (StringUtils.hasText(delta.text())) {
                eventConsumer.accept(new TextDeltaEvent(delta.text()));
            }
        }

        @Override
        public void visitMessageStop(MessageStopEvent event) {
            stopReason = event.stopReasonAsString();
        }

        @Override
        public void visitMetadata(ConverseStreamMetadataEvent event) {
            if (event.usage() == null) {
                return;
            }
            int inputTokens = Optional.ofNullable(event.usage().inputTokens()).orElse(0);
            int outputTokens = Optional.ofNullable(event.usage().outputTokens()).orElse(0);
            eventConsumer.accept(new com.chatbot.backend.config.aws.MessageStopEvent(
                    stopReason, new TokenUsage(inputTokens, outputTokens)));
        }
    }

    private static class StreamSubscriber implements Subscriber<ConverseStreamOutput> {
        private final StreamVisitor visitor;
        private final Consumer<StreamEvent> eventConsumer;

        StreamSubscriber(StreamVisitor visitor, Consumer<StreamEvent> eventConsumer) {
            this.visitor = visitor;
            this.eventConsumer = eventConsumer;
        }

        @Override
        public void onSubscribe(Subscription s) {
            s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ConverseStreamOutput event) {
            event.accept(visitor);
        }

        @Override
        public void onError(Throwable t) {
            eventConsumer.accept(new ErrorEvent(t.getMessage()));
        }

        @Override
        public void onComplete() {
        }
    }
}
