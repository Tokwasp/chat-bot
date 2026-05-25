package com.chatbot.backend.exception;

public class SessionNotFoundException extends BusinessException {

    public SessionNotFoundException() {
        super(ErrorCode.SESSION_NOT_FOUND);
    }
}
