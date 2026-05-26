package com.chatbot.backend.controller;

import com.chatbot.backend.config.aws.ImageGenerationResult;
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
    @DisplayName("정상 요청이면 200과 함께 base64 이미지를 반환한다")
    void generate_WhenValidRequest_ThenReturnsImage() throws Exception {
        when(imageGenerationService.generate(any()))
                .thenReturn(new ImageGenerationResult("BASE64IMG", 42L, "image/png"));

        mockMvc.perform(post("/api/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"우주를 나는 고양이\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.imageBase64").value("BASE64IMG"))
                .andExpect(jsonPath("$.data.seed").value(42))
                .andExpect(jsonPath("$.data.mimeType").value("image/png"));
    }

    @Test
    @DisplayName("prompt가 누락되면 400과 함께 안내 메시지를 반환한다")
    void generate_WhenPromptMissing_ThenReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("이미지 설명을 입력해주세요."));
    }

    @Test
    @DisplayName("prompt가 빈 문자열이면 400을 반환한다")
    void generate_WhenPromptBlank_ThenReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
