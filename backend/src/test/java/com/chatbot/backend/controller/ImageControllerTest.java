package com.chatbot.backend.controller;

import com.chatbot.backend.service.BedrockServiceError;
import com.chatbot.backend.service.ImageGenerationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImageController.class)
class ImageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ImageGenerationService imageGenerationService;

    @Test
    @DisplayName("prompt가 누락되면 400 에러와 함께 '프롬프트를 입력해주세요.' 메시지를 반환한다")
    void generate_WhenPromptMissing_ThenReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/images")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("프롬프트를 입력해주세요."))
            .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("prompt가 빈 문자열이면 400 에러와 함께 '프롬프트를 입력해주세요.' 메시지를 반환한다")
    void generate_WhenPromptBlank_ThenReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/images")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"prompt\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("프롬프트를 입력해주세요."));
    }

    @Test
    @DisplayName("정상 요청이면 200과 함께 base64 이미지를 data.image로 반환한다")
    void generate_WhenValidRequest_ThenReturnsBase64Image() throws Exception {
        when(imageGenerationService.generate(any())).thenReturn("BASE64_IMAGE_DATA");

        mockMvc.perform(post("/api/images")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"prompt\":\"귀여운 로봇\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.image").value("BASE64_IMAGE_DATA"));
    }

    @Test
    @DisplayName("이미지 생성 중 BedrockServiceError가 발생하면 502와 한국어 에러 메시지를 반환한다")
    void generate_WhenBedrockServiceError_ThenReturnsBadGateway() throws Exception {
        when(imageGenerationService.generate(any()))
                .thenThrow(new BedrockServiceError("IMAGE_GENERATION_FAILED"));

        mockMvc.perform(post("/api/images")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"prompt\":\"귀여운 로봇\"}"))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.errorCode").value("IMAGE_GENERATION_FAILED"))
            .andExpect(jsonPath("$.message").value("이미지 생성에 실패했습니다. 잠시 후 다시 시도해주세요."));
    }
}
