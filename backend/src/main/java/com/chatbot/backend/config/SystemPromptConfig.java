package com.chatbot.backend.config;

/**
 * 시스템 프롬프트 조회 API.
 * 페르소나 키를 받아 해당 시스템 프롬프트 문자열로 변환한다.
 * 이 문자열이 ConversationRequest.systemPrompt에 실려 Bedrock 호출 시 system 블록으로 들어간다.
 * (TS 레퍼런스의 DEFAULT_SYSTEM_PROMPT 상수 + getSystemPrompt 함수에 대응)
 */
public class SystemPromptConfig {

    public static final String DEFAULT_SYSTEM_PROMPT = Persona.DEFAULT.getSystemPrompt();

    private SystemPromptConfig() {
    }

    public static String getSystemPrompt(String personaKey) {
        return Persona.from(personaKey).getSystemPrompt();
    }
}
