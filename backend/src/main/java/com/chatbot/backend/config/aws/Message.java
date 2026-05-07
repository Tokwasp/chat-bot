package com.chatbot.backend.config.aws;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class Message {
    private final String role;
    private final List<ContentBlock> content;
}
