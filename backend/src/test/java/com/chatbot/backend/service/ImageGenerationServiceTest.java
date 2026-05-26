package com.chatbot.backend.service;

import com.chatbot.backend.dto.request.ImageGenerationRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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

    private static final String MODEL_ID = "amazon.nova-canvas-v1:0";

    private BedrockRuntimeClient bedrockRuntimeClient;
    private ObjectMapper objectMapper;
    private ImageGenerationService imageGenerationService;

    @BeforeEach
    void setUp() {
        bedrockRuntimeClient = mock(BedrockRuntimeClient.class);
        objectMapper = new ObjectMapper();
        imageGenerationService = new ImageGenerationService(bedrockRuntimeClient, MODEL_ID, objectMapper);
    }

    @Test
    @DisplayName("프롬프트로 이미지 생성을 요청하면 Nova Canvas 응답의 base64 이미지를 반환한다")
    void whenPromptProvided_thenReturnBase64Image() {
        mockInvokeModel("{\"images\":[\"BASE64_IMAGE_DATA\"],\"error\":null}");

        String image = imageGenerationService.generate(buildRequest("귀여운 로봇"));

        assertThat(image).isEqualTo("BASE64_IMAGE_DATA");
    }

    @Test
    @DisplayName("요청 본문에 taskType과 프롬프트, 기본 이미지 크기(1024)가 포함된다")
    void whenGenerate_thenRequestBodyContainsTaskTypeAndPrompt() throws Exception {
        mockInvokeModel("{\"images\":[\"BASE64\"]}");

        ArgumentCaptor<InvokeModelRequest> captor = ArgumentCaptor.forClass(InvokeModelRequest.class);
        imageGenerationService.generate(buildRequest("바다 풍경"));

        org.mockito.Mockito.verify(bedrockRuntimeClient).invokeModel(captor.capture());
        InvokeModelRequest sent = captor.getValue();
        assertThat(sent.modelId()).isEqualTo(MODEL_ID);

        JsonNode body = objectMapper.readTree(sent.body().asUtf8String());
        assertThat(body.get("taskType").asText()).isEqualTo("TEXT_IMAGE");
        assertThat(body.get("textToImageParams").get("text").asText()).isEqualTo("바다 풍경");
        assertThat(body.get("imageGenerationConfig").get("width").asInt()).isEqualTo(1024);
        assertThat(body.get("imageGenerationConfig").get("height").asInt()).isEqualTo(1024);
    }

    @Test
    @DisplayName("negativePrompt가 주어지면 요청 본문에 negativeText로 포함된다")
    void whenNegativePromptProvided_thenIncludedAsNegativeText() throws Exception {
        mockInvokeModel("{\"images\":[\"BASE64\"]}");

        ArgumentCaptor<InvokeModelRequest> captor = ArgumentCaptor.forClass(InvokeModelRequest.class);
        ImageGenerationRequest request = ImageGenerationRequest.builder()
                .prompt("도시 야경")
                .negativePrompt("사람, 글자")
                .build();
        imageGenerationService.generate(request);

        org.mockito.Mockito.verify(bedrockRuntimeClient).invokeModel(captor.capture());
        JsonNode body = objectMapper.readTree(captor.getValue().body().asUtf8String());
        assertThat(body.get("textToImageParams").get("negativeText").asText()).isEqualTo("사람, 글자");
    }

    @Test
    @DisplayName("응답에 images 배열이 비어 있으면 BedrockServiceError(IMAGE_GENERATION_FAILED)를 던진다")
    void whenNoImagesInResponse_thenThrowError() {
        mockInvokeModel("{\"images\":[]}");

        assertThatThrownBy(() -> imageGenerationService.generate(buildRequest("테스트")))
                .isInstanceOf(BedrockServiceError.class)
                .hasMessage("IMAGE_GENERATION_FAILED");
    }

    @Test
    @DisplayName("응답에 error 필드가 있으면 BedrockServiceError(IMAGE_GENERATION_FAILED)를 던진다")
    void whenResponseHasError_thenThrowError() {
        mockInvokeModel("{\"images\":null,\"error\":\"content filtered\"}");

        assertThatThrownBy(() -> imageGenerationService.generate(buildRequest("테스트")))
                .isInstanceOf(BedrockServiceError.class)
                .hasMessage("IMAGE_GENERATION_FAILED");
    }

    @Test
    @DisplayName("AccessDeniedException이 발생하면 BedrockServiceError(ACCESS_DENIED)를 던진다")
    void whenAccessDenied_thenThrowAccessDeniedError() {
        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class)))
                .thenThrow(AccessDeniedException.builder().message("denied").build());

        assertThatThrownBy(() -> imageGenerationService.generate(buildRequest("테스트")))
                .isInstanceOf(BedrockServiceError.class)
                .hasMessage("ACCESS_DENIED");
    }

    @Test
    @DisplayName("ResourceNotFoundException이 발생하면 BedrockServiceError(MODEL_NOT_FOUND)를 던진다")
    void whenResourceNotFound_thenThrowModelNotFoundError() {
        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().message("not found").build());

        assertThatThrownBy(() -> imageGenerationService.generate(buildRequest("테스트")))
                .isInstanceOf(BedrockServiceError.class)
                .hasMessage("MODEL_NOT_FOUND");
    }

    @Test
    @DisplayName("ThrottlingException이 발생하면 retryable이 true인 BedrockServiceError(THROTTLING)를 던진다")
    void whenThrottling_thenThrowRetryableError() {
        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class)))
                .thenThrow(ThrottlingException.builder().message("rate exceeded").build());

        assertThatThrownBy(() -> imageGenerationService.generate(buildRequest("테스트")))
                .isInstanceOf(BedrockServiceError.class)
                .hasMessage("THROTTLING")
                .satisfies(e -> assertThat(((BedrockServiceError) e).isRetryable()).isTrue());
    }

    @Test
    @DisplayName("ValidationException이 발생하면 BedrockServiceError(INVALID_IMAGE_REQUEST)를 던진다")
    void whenValidationException_thenThrowInvalidRequestError() {
        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class)))
                .thenThrow(ValidationException.builder().message("invalid").build());

        assertThatThrownBy(() -> imageGenerationService.generate(buildRequest("테스트")))
                .isInstanceOf(BedrockServiceError.class)
                .hasMessage("INVALID_IMAGE_REQUEST");
    }

    private ImageGenerationRequest buildRequest(String prompt) {
        return ImageGenerationRequest.builder().prompt(prompt).build();
    }

    private void mockInvokeModel(String responseBody) {
        InvokeModelResponse response = InvokeModelResponse.builder()
                .body(SdkBytes.fromUtf8String(responseBody))
                .build();
        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class))).thenReturn(response);
    }
}
