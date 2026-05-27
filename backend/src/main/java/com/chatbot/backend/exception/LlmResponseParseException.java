package com.chatbot.backend.exception;

public class LlmResponseParseException extends BusinessException {

    public LlmResponseParseException() {
        super(ErrorCode.LLM_RESPONSE_PARSE_ERROR);
    }
}
