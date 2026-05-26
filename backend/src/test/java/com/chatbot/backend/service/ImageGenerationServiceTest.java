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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImageGenerationServiceTest {

    private static final String MODEL_ID = "stability.stable-diffusion-xl-v1";

    private final BedrockRuntimeClient client = mock(BedrockRuntimeClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ImageGenerationService service =
            new ImageGenerationService(client, objectMapper, MODEL_ID);

    @Test
    @DisplayName("프롬프트로 이미지를 생성하면 base64 이미지와 seed를 반환한다")
    void whenPromptGiven_thenReturnsBase64ImageAndSeed() throws Exception {
        when(client.invokeModel(any(InvokeModelRequest.class)))
                .thenReturn(invokeResponse("success", "BASE64DATA", 12345L, "SUCCESS"));

        ImageGenerationResult result = service.generate(
                new ImageGenerationRequest("우주를 나는 고양이", null, null, null, null, null, null));

        assertThat(result.getImageBase64()).isEqualTo("BASE64DATA");
        assertThat(result.getSeed()).isEqualTo(12345L);
        assertThat(result.getMimeType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("요청 본문에 모델 ID와 text_prompts가 올바르게 담긴다")
    void whenGenerate_thenRequestBodyContainsModelIdAndPrompt() throws Exception {
        ArgumentCaptor<InvokeModelRequest> captor = ArgumentCaptor.forClass(InvokeModelRequest.class);
        when(client.invokeModel(captor.capture()))
                .thenReturn(invokeResponse("success", "IMG", 1L, "SUCCESS"));

        service.generate(new ImageGenerationRequest("a red apple", "blurry", 512, 512, 8, 40, 7L));

        InvokeModelRequest sent = captor.getValue();
        assertThat(sent.modelId()).isEqualTo(MODEL_ID);

        JsonNode body = objectMapper.readTree(sent.body().asUtf8String());
        assertThat(body.get("cfg_scale").asInt()).isEqualTo(8);
        assertThat(body.get("steps").asInt()).isEqualTo(40);
        assertThat(body.get("seed").asLong()).isEqualTo(7L);
        assertThat(body.get("width").asInt()).isEqualTo(512);
        assertThat(body.get("height").asInt()).isEqualTo(512);
        assertThat(body.get("text_prompts").get(0).get("text").asText()).isEqualTo("a red apple");
        assertThat(body.get("text_prompts").get(0).get("weight").asDouble()).isEqualTo(1.0);
        assertThat(body.get("text_prompts").get(1).get("text").asText()).isEqualTo("blurry");
        assertThat(body.get("text_prompts").get(1).get("weight").asDouble()).isLessThan(0.0);
    }

    @Test
    @DisplayName("옵션을 생략하면 기본값(1024x1024 등)이 적용된다")
    void whenOptionsOmitted_thenDefaultsApplied() throws Exception {
        ArgumentCaptor<InvokeModelRequest> captor = ArgumentCaptor.forClass(InvokeModelRequest.class);
        when(client.invokeModel(captor.capture()))
                .thenReturn(invokeResponse("success", "IMG", 1L, "SUCCESS"));

        service.generate(new ImageGenerationRequest("a cat", null, null, null, null, null, null));

        JsonNode body = objectMapper.readTree(captor.getValue().body().asUtf8String());
        assertThat(body.get("width").asInt()).isEqualTo(1024);
        assertThat(body.get("height").asInt()).isEqualTo(1024);
        assertThat(body.get("text_prompts").size()).isEqualTo(1);
    }

    @Test
    @DisplayName("결과가 content filtered이면 사용자 친화적 에러를 던진다")
    void whenContentFiltered_thenThrowsError() {
        when(client.invokeModel(any(InvokeModelRequest.class)))
                .thenReturn(invokeResponse("success", "", 1L, "CONTENT_FILTERED"));

        assertThatThrownBy(() -> service.generate(
                new ImageGenerationRequest("nsfw", null, null, null, null, null, null)))
                .isInstanceOf(BedrockServiceError.class)
                .hasMessage("CONTENT_FILTERED");
    }

    @Test
    @DisplayName("AccessDeniedException이 발생하면 BedrockServiceError(ACCESS_DENIED)를 던진다")
    void whenAccessDenied_thenThrowsBedrockServiceError() {
        when(client.invokeModel(any(InvokeModelRequest.class)))
                .thenThrow(AccessDeniedException.builder().message("denied").build());

        assertThatThrownBy(() -> service.generate(
                new ImageGenerationRequest("a cat", null, null, null, null, null, null)))
                .isInstanceOf(BedrockServiceError.class)
                .hasMessage("ACCESS_DENIED");
    }

    @Test
    @DisplayName("ResourceNotFoundException이 발생하면 BedrockServiceError(MODEL_NOT_FOUND)를 던진다")
    void whenResourceNotFound_thenThrowsBedrockServiceError() {
        when(client.invokeModel(any(InvokeModelRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().message("not found").build());

        assertThatThrownBy(() -> service.generate(
                new ImageGenerationRequest("a cat", null, null, null, null, null, null)))
                .isInstanceOf(BedrockServiceError.class)
                .hasMessage("MODEL_NOT_FOUND");
    }

    @Test
    @DisplayName("ThrottlingException이 발생하면 retryable=true인 BedrockServiceError(THROTTLING)를 던진다")
    void whenThrottling_thenThrowsRetryableBedrockServiceError() {
        when(client.invokeModel(any(InvokeModelRequest.class)))
                .thenThrow(ThrottlingException.builder().message("rate").build());

        assertThatThrownBy(() -> service.generate(
                new ImageGenerationRequest("a cat", null, null, null, null, null, null)))
                .isInstanceOf(BedrockServiceError.class)
                .hasMessage("THROTTLING")
                .satisfies(e -> assertThat(((BedrockServiceError) e).isRetryable()).isTrue());
    }

    private InvokeModelResponse invokeResponse(String result, String base64, long seed, String finishReason) {
        String json = "{\"result\":\"" + result + "\",\"artifacts\":[{"
                + "\"seed\":" + seed + ","
                + "\"base64\":\"" + base64 + "\","
                + "\"finishReason\":\"" + finishReason + "\"}]}";
        return InvokeModelResponse.builder()
                .body(SdkBytes.fromUtf8String(json))
                .build();
    }
}
