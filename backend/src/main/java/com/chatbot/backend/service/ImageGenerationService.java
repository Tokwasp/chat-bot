package com.chatbot.backend.service;

import com.chatbot.backend.dto.request.ImageGenerationRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.AccessDeniedException;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import software.amazon.awssdk.services.bedrockruntime.model.ResourceNotFoundException;
import software.amazon.awssdk.services.bedrockruntime.model.ThrottlingException;
import software.amazon.awssdk.services.bedrockruntime.model.ValidationException;

@Slf4j
@Service
public class ImageGenerationService {

    private static final String CONTENT_TYPE = "application/json";
    private static final int DEFAULT_SIZE = 1024;

    private final BedrockRuntimeClient bedrockRuntimeClient;
    private final String imageModelId;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ImageGenerationService(BedrockRuntimeClient bedrockRuntimeClient,
                                  @Value("${aws.bedrock.image-model-id}") String imageModelId) {
        this.bedrockRuntimeClient = bedrockRuntimeClient;
        this.imageModelId = imageModelId;
    }

    public String generate(ImageGenerationRequest request) {
        InvokeModelRequest invokeRequest = InvokeModelRequest.builder()
                .modelId(imageModelId)
                .contentType(CONTENT_TYPE)
                .accept(CONTENT_TYPE)
                .body(SdkBytes.fromUtf8String(buildRequestBody(request)))
                .build();

        try {
            InvokeModelResponse response = bedrockRuntimeClient.invokeModel(invokeRequest);
            return extractImage(response.body().asUtf8String());
        } catch (AccessDeniedException e) {
            log.error("Nova Canvas access denied (modelId={}): {}", imageModelId, e.getMessage());
            throw new BedrockServiceError("ACCESS_DENIED");
        } catch (ResourceNotFoundException e) {
            log.error("Nova Canvas model not found (modelId={}): {}", imageModelId, e.getMessage());
            throw new BedrockServiceError("MODEL_NOT_FOUND");
        } catch (ThrottlingException e) {
            log.error("Nova Canvas throttled (modelId={}): {}", imageModelId, e.getMessage());
            throw new BedrockServiceError("THROTTLING", true);
        } catch (ValidationException e) {
            log.error("Nova Canvas validation error (modelId={}): {}", imageModelId, e.getMessage());
            throw new BedrockServiceError("INVALID_IMAGE_REQUEST");
        } catch (BedrockServiceError e) {
            throw e;
        } catch (Exception e) {
            log.error("Nova Canvas invocation failed (modelId={})", imageModelId, e);
            throw new BedrockServiceError("IMAGE_GENERATION_FAILED");
        }
    }

    private String buildRequestBody(ImageGenerationRequest request) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("taskType", "TEXT_IMAGE");

        ObjectNode textToImageParams = root.putObject("textToImageParams");
        textToImageParams.put("text", request.getPrompt());
        if (StringUtils.hasText(request.getNegativePrompt())) {
            textToImageParams.put("negativeText", request.getNegativePrompt());
        }

        ObjectNode config = root.putObject("imageGenerationConfig");
        config.put("numberOfImages", 1);
        config.put("width", resolveSize(request.getWidth()));
        config.put("height", resolveSize(request.getHeight()));
        config.put("cfgScale", 8.0);
        config.put("quality", "standard");

        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new BedrockServiceError("INVALID_IMAGE_REQUEST");
        }
    }

    private int resolveSize(Integer size) {
        return size == null ? DEFAULT_SIZE : size;
    }

    private String extractImage(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode error = root.get("error");
            if (error != null && !error.isNull() && StringUtils.hasText(error.asText())) {
                log.error("Nova Canvas returned an error: {}", error.asText());
                throw new BedrockServiceError("IMAGE_GENERATION_FAILED");
            }

            JsonNode images = root.get("images");
            if (images == null || !images.isArray() || images.isEmpty()) {
                log.error("Nova Canvas response did not contain any image");
                throw new BedrockServiceError("IMAGE_GENERATION_FAILED");
            }
            return images.get(0).asText();
        } catch (BedrockServiceError e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Nova Canvas response", e);
            throw new BedrockServiceError("IMAGE_GENERATION_FAILED");
        }
    }
}
