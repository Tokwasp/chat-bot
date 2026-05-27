package com.chatbot.backend.controller;

import com.chatbot.backend.dto.request.Text2SqlRequest;
import com.chatbot.backend.dto.response.ApiResponse;
import com.chatbot.backend.dto.response.Text2SqlResponse;
import com.chatbot.backend.service.Text2SqlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/text2sql")
public class Text2SqlController {

    private final Text2SqlService text2SqlService;

    @PostMapping
    public ResponseEntity<ApiResponse<Text2SqlResponse>> generate(@Valid @RequestBody Text2SqlRequest request) {
        Text2SqlResponse data = text2SqlService.generate(request.getQuestion());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
