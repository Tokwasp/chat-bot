package com.chatbot.backend.service;

import com.chatbot.backend.config.aws.ImageGenerationRequest;
import com.chatbot.backend.config.aws.ImageGenerationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

@Slf4j
@Service
public class ImageGenerationService {

    private static final String MIME_TYPE = "image/png";
    private static final int DEFAULT_DIMENSION = 1024;
    private static final int DEFAULT_CFG_SCALE = 7;
    private static final int DEFAULT_STEPS = 30;

    private final BedrockRuntimeClient bedrockRuntimeClient;
    private final ObjectMapper objectMapper;
    private final String imageModelId;

    public ImageGenerationService(BedrockRuntimeClient bedrockRuntimeClient,
                                  ObjectMapper objectMapper,
                                  @Value("${aws.bedrock.image-model-id:stability.stable-diffusion-xl-v1}") String imageModelId) {
        this.bedrockRuntimeClient = bedrockRuntimeClient;
        this.objectMapper = objectMapper;
        this.imageModelId = imageModelId;
    }

    public ImageGenerationResult generate(ImageGenerationRequest request) {
        InvokeModelResponse response = invoke(buildRequestBody(request));
        return parseResult(response);
    }

    private InvokeModelResponse invoke(String body) {
        try {
            return bedrockRuntimeClient.invokeModel(InvokeModelRequest.builder()
                    .modelId(imageModelId)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromUtf8String(body))
                    .build());
        } catch (AccessDeniedException e) {
            throw new BedrockServiceError("ACCESS_DENIED");
        } catch (ResourceNotFoundException e) {
            throw new BedrockServiceError("MODEL_NOT_FOUND");
        } catch (ThrottlingException e) {
            throw new BedrockServiceError("THROTTLING", true);
        }
    }

    private String buildRequestBody(ImageGenerationRequest request) {
        ObjectNode root = objectMapper.createObjectNode();

        ArrayNode textPrompts = root.putArray("text_prompts");
        textPrompts.addObject()
                .put("text", request.getPrompt())
                .put("weight", 1.0);
        if (StringUtils.hasText(request.getNegativePrompt())) {
            textPrompts.addObject()
                    .put("text", request.getNegativePrompt())
                    .put("weight", -1.0);
        }

        root.put("cfg_scale", orDefault(request.getCfgScale(), DEFAULT_CFG_SCALE));
        root.put("steps", orDefault(request.getSteps(), DEFAULT_STEPS));
        root.put("width", orDefault(request.getWidth(), DEFAULT_DIMENSION));
        root.put("height", orDefault(request.getHeight(), DEFAULT_DIMENSION));
        if (request.getSeed() != null) {
            root.put("seed", request.getSeed());
        }

        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new BedrockServiceError("INVALID_REQUEST");
        }
    }

    private ImageGenerationResult parseResult(InvokeModelResponse response) {
        JsonNode root = readBody(response);

        JsonNode artifacts = root.path("artifacts");
        if (!artifacts.isArray() || artifacts.isEmpty()) {
            log.error("Bedrock image response has no artifacts: {}", root.path("result").asText());
            throw new BedrockServiceError("EMPTY_RESPONSE");
        }

        JsonNode artifact = artifacts.get(0);
        String finishReason = artifact.path("finishReason").asText("SUCCESS");
        if ("CONTENT_FILTERED".equals(finishReason)) {
            throw new BedrockServiceError("CONTENT_FILTERED");
        }
        if (!"SUCCESS".equals(finishReason)) {
            log.error("Bedrock image generation finished with reason: {}", finishReason);
            throw new BedrockServiceError("GENERATION_FAILED");
        }

        return new ImageGenerationResult(
                artifact.path("base64").asText(),
                artifact.path("seed").asLong(),
                MIME_TYPE);
    }

    private JsonNode readBody(InvokeModelResponse response) {
        try {
            return objectMapper.readTree(response.body().asUtf8String());
        } catch (Exception e) {
            throw new BedrockServiceError("INVALID_RESPONSE");
        }
    }

    private int orDefault(Integer value, int defaultValue) {
        return value != null ? value : defaultValue;
    }
}
