package com.chatbot.backend.service;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MessageHistoryOptions {

    private Integer maxMessages;
    private Integer maxTokens;

    @Builder
    private MessageHistoryOptions(Integer maxMessages, Integer maxTokens) {
        this.maxMessages = maxMessages;
        this.maxTokens = maxTokens;
    }
}
