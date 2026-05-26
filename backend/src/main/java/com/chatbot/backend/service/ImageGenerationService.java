package com.chatbot.backend.service;

import com.chatbot.backend.config.aws.ImageGenerationRequest;
import com.chatbot.backend.config.aws.ImageGenerationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final String OUTPUT_FORMAT = "png";
    private static final String MODE = "text-to-image";
    private static final String DEFAULT_ASPECT_RATIO = "1:1";

    private final BedrockRuntimeClient bedrockRuntimeClient;
    private final ObjectMapper objectMapper;
    private final String imageModelId;

    public ImageGenerationService(BedrockRuntimeClient bedrockRuntimeClient,
                                  ObjectMapper objectMapper,
                                  @Value("${aws.bedrock.image-model-id:stability.stable-image-core-v1:1}") String imageModelId) {
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
        } catch (ValidationException e) {
            log.error("Invalid Bedrock image request (modelId={}): {}", imageModelId, e.getMessage());
            throw new BedrockServiceError("INVALID_MODEL_OR_REQUEST");
        } catch (ThrottlingException e) {
            throw new BedrockServiceError("THROTTLING", true);
        }
    }

    private String buildRequestBody(ImageGenerationRequest request) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("prompt", request.getPrompt());
        root.put("mode", MODE);
        root.put("output_format", OUTPUT_FORMAT);
        root.put("aspect_ratio",
                StringUtils.hasText(request.getAspectRatio()) ? request.getAspectRatio() : DEFAULT_ASPECT_RATIO);
        if (StringUtils.hasText(request.getNegativePrompt())) {
            root.put("negative_prompt", request.getNegativePrompt());
        }
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

        JsonNode images = root.path("images");
        if (!images.isArray() || images.isEmpty()) {
            log.error("Bedrock image response has no images");
            throw new BedrockServiceError("EMPTY_RESPONSE");
        }

        JsonNode finishReason = root.path("finish_reasons").path(0);
        if (finishReason.isTextual() && StringUtils.hasText(finishReason.asText())) {
            log.error("Bedrock image generation filtered/failed: {}", finishReason.asText());
            throw new BedrockServiceError("CONTENT_FILTERED");
        }

        Long seed = root.path("seeds").path(0).isNumber() ? root.path("seeds").path(0).asLong() : null;
        return new ImageGenerationResult(images.get(0).asText(), seed, MIME_TYPE);
    }

    private JsonNode readBody(InvokeModelResponse response) {
        try {
            return objectMapper.readTree(response.body().asUtf8String());
        } catch (Exception e) {
            throw new BedrockServiceError("INVALID_RESPONSE");
        }
    }
}
