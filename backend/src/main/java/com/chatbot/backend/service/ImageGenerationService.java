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
    private static final String DEFAULT_ASPECT_RATIO = "1:1";
    private static final String OUTPUT_FORMAT = "png";

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
            log.error("Image model access denied (modelId={}): {}", imageModelId, e.getMessage());
            throw new BedrockServiceError("ACCESS_DENIED");
        } catch (ResourceNotFoundException e) {
            log.error("Image model not found (modelId={}): {}", imageModelId, e.getMessage());
            throw new BedrockServiceError("MODEL_NOT_FOUND");
        } catch (ThrottlingException e) {
            log.error("Image model throttled (modelId={}): {}", imageModelId, e.getMessage());
            throw new BedrockServiceError("THROTTLING", true);
        } catch (ValidationException e) {
            log.error("Image model validation error (modelId={}): {}", imageModelId, e.getMessage());
            throw new BedrockServiceError("INVALID_IMAGE_REQUEST");
        } catch (BedrockServiceError e) {
            throw e;
        } catch (Exception e) {
            log.error("Image generation failed (modelId={})", imageModelId, e);
            throw new BedrockServiceError("IMAGE_GENERATION_FAILED");
        }
    }

    private String buildRequestBody(ImageGenerationRequest request) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("prompt", request.getPrompt());
        root.put("mode", "text-to-image");
        root.put("aspect_ratio", resolveAspectRatio(request.getAspectRatio()));
        root.put("output_format", OUTPUT_FORMAT);
        if (StringUtils.hasText(request.getNegativePrompt())) {
            root.put("negative_prompt", request.getNegativePrompt());
        }

        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new BedrockServiceError("INVALID_IMAGE_REQUEST");
        }
    }

    private String resolveAspectRatio(String aspectRatio) {
        return StringUtils.hasText(aspectRatio) ? aspectRatio : DEFAULT_ASPECT_RATIO;
    }

    private String extractImage(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            JsonNode finishReasons = root.get("finish_reasons");
            if (finishReasons != null && finishReasons.isArray() && !finishReasons.isEmpty()) {
                JsonNode firstReason = finishReasons.get(0);
                if (firstReason != null && !firstReason.isNull() && StringUtils.hasText(firstReason.asText())) {
                    log.error("Image generation blocked: {}", firstReason.asText());
                    throw new BedrockServiceError("IMAGE_GENERATION_FAILED");
                }
            }

            JsonNode images = root.get("images");
            if (images == null || !images.isArray() || images.isEmpty()) {
                log.error("Image model response did not contain any image");
                throw new BedrockServiceError("IMAGE_GENERATION_FAILED");
            }
            return images.get(0).asText();
        } catch (BedrockServiceError e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse image model response", e);
            throw new BedrockServiceError("IMAGE_GENERATION_FAILED");
        }
    }
}
