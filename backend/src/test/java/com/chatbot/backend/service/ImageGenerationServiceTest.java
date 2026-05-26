package com.chatbot.backend.service;

import com.chatbot.backend.config.aws.ImageGenerationRequest;
import com.chatbot.backend.config.aws.ImageGenerationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.AccessDeniedException;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import software.amazon.awssdk.services.bedrockruntime.model.ResourceNotFoundException;
import software.amazon.awssdk.services.bedrockruntime.model.ThrottlingException;
import software.amazon.awssdk.services.bedrockruntime.model.ValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImageGenerationServiceTest {

    private static final String MODEL_ID = "stability.stable-image-core-v1:1";

    private final BedrockRuntimeClient client = mock(BedrockRuntimeClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ImageGenerationService service =
            new ImageGenerationService(client, objectMapper, MODEL_ID);

    @Test
    @DisplayName("프롬프트로 이미지를 생성하면 base64 이미지와 seed를 반환한다")
    void whenPromptGiven_thenReturnsBase64ImageAndSeed() {
        when(client.invokeModel(any(InvokeModelRequest.class)))
                .thenReturn(invokeResponse("BASE64DATA", 12345L, null));

        ImageGenerationResult result = service.generate(
                new ImageGenerationRequest("우주를 나는 고양이", null, null, null));

        assertThat(result.getImageBase64()).isEqualTo("BASE64DATA");
        assertThat(result.getSeed()).isEqualTo(12345L);
        assertThat(result.getMimeType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("요청 본문에 모델 ID와 Stable Image 신형 필드가 올바르게 담긴다")
    void whenGenerate_thenRequestBodyContainsModelIdAndPrompt() throws Exception {
        ArgumentCaptor<InvokeModelRequest> captor = ArgumentCaptor.forClass(InvokeModelRequest.class);
        when(client.invokeModel(captor.capture()))
                .thenReturn(invokeResponse("IMG", 1L, null));

        service.generate(new ImageGenerationRequest("a red apple", "blurry", "16:9", 7L));

        InvokeModelRequest sent = captor.getValue();
        assertThat(sent.modelId()).isEqualTo(MODEL_ID);

        JsonNode body = objectMapper.readTree(sent.body().asUtf8String());
        assertThat(body.get("prompt").asText()).isEqualTo("a red apple");
        assertThat(body.get("mode").asText()).isEqualTo("text-to-image");
        assertThat(body.get("output_format").asText()).isEqualTo("png");
        assertThat(body.get("aspect_ratio").asText()).isEqualTo("16:9");
        assertThat(body.get("negative_prompt").asText()).isEqualTo("blurry");
        assertThat(body.get("seed").asLong()).isEqualTo(7L);
    }

    @Test
    @DisplayName("옵션을 생략하면 기본 aspect_ratio(1:1)가 적용되고 negative_prompt/seed는 빠진다")
    void whenOptionsOmitted_thenDefaultsApplied() throws Exception {
        ArgumentCaptor<InvokeModelRequest> captor = ArgumentCaptor.forClass(InvokeModelRequest.class);
        when(client.invokeModel(captor.capture()))
                .thenReturn(invokeResponse("IMG", 1L, null));

        service.generate(new ImageGenerationRequest("a cat", null, null, null));

        JsonNode body = objectMapper.readTree(captor.getValue().body().asUtf8String());
        assertThat(body.get("aspect_ratio").asText()).isEqualTo("1:1");
        assertThat(body.has("negative_prompt")).isFalse();
        assertThat(body.has("seed")).isFalse();
    }

    @Test
    @DisplayName("finish_reasons에 사유가 있으면 CONTENT_FILTERED 에러를 던진다")
    void whenContentFiltered_thenThrowsError() {
        when(client.invokeModel(any(InvokeModelRequest.class)))
                .thenReturn(invokeResponse("", 1L, "Filter reason: prompt"));

        assertThatThrownBy(() -> service.generate(
                new ImageGenerationRequest("nsfw", null, null, null)))
                .isInstanceOf(BedrockServiceError.class)
                .hasMessage("CONTENT_FILTERED");
    }

    @Test
    @DisplayName("ValidationException이 발생하면 BedrockServiceError(INVALID_MODEL_OR_REQUEST)를 던진다")
    void whenValidationException_thenThrowsBedrockServiceError() {
        when(client.invokeModel(any(InvokeModelRequest.class)))
                .thenThrow(ValidationException.builder().message("The provided model identifier is invalid.").build());

        assertThatThrownBy(() -> service.generate(
                new ImageGenerationRequest("a cat", null, null, null)))
                .isInstanceOf(BedrockServiceError.class)
                .hasMessage("INVALID_MODEL_OR_REQUEST");
    }

    @Test
    @DisplayName("AccessDeniedException이 발생하면 BedrockServiceError(ACCESS_DENIED)를 던진다")
    void whenAccessDenied_thenThrowsBedrockServiceError() {
        when(client.invokeModel(any(InvokeModelRequest.class)))
                .thenThrow(AccessDeniedException.builder().message("denied").build());

        assertThatThrownBy(() -> service.generate(
                new ImageGenerationRequest("a cat", null, null, null)))
                .isInstanceOf(BedrockServiceError.class)
                .hasMessage("ACCESS_DENIED");
    }

    @Test
    @DisplayName("ResourceNotFoundException이 발생하면 BedrockServiceError(MODEL_NOT_FOUND)를 던진다")
    void whenResourceNotFound_thenThrowsBedrockServiceError() {
        when(client.invokeModel(any(InvokeModelRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().message("not found").build());

        assertThatThrownBy(() -> service.generate(
                new ImageGenerationRequest("a cat", null, null, null)))
                .isInstanceOf(BedrockServiceError.class)
                .hasMessage("MODEL_NOT_FOUND");
    }

    @Test
    @DisplayName("ThrottlingException이 발생하면 retryable=true인 BedrockServiceError(THROTTLING)를 던진다")
    void whenThrottling_thenThrowsRetryableBedrockServiceError() {
        when(client.invokeModel(any(InvokeModelRequest.class)))
                .thenThrow(ThrottlingException.builder().message("rate").build());

        assertThatThrownBy(() -> service.generate(
                new ImageGenerationRequest("a cat", null, null, null)))
                .isInstanceOf(BedrockServiceError.class)
                .hasMessage("THROTTLING")
                .satisfies(e -> assertThat(((BedrockServiceError) e).isRetryable()).isTrue());
    }

    private InvokeModelResponse invokeResponse(String base64, long seed, String finishReason) {
        String finish = finishReason == null ? "null" : "\"" + finishReason + "\"";
        String json = "{\"images\":[\"" + base64 + "\"],"
                + "\"seeds\":[" + seed + "],"
                + "\"finish_reasons\":[" + finish + "]}";
        return InvokeModelResponse.builder()
                .body(SdkBytes.fromUtf8String(json))
                .build();
    }
}
