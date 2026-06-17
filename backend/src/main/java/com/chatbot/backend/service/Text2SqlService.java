package com.chatbot.backend.service;

import com.chatbot.backend.dto.response.Text2SqlResponse;

public interface Text2SqlService {

    Text2SqlResponse generate(String question);
}
