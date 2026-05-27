package com.chatbot.backend.config;

// TODO: 실제 프롬프트/테이블 스키마로 교체 (현재는 플레이스홀더)
public final class Text2SqlPrompts {

    private Text2SqlPrompts() {
    }

    public static final String INTENT_CLASSIFICATION_PROMPT = """
            당신은 사용자의 자연어 질문에서 의도를 분류하는 분류기입니다.
            사용자 질문을 읽고 어떤 종류의 거래내역 조회 의도인지 한 줄로 분류하세요.
            (플레이스홀더: 실제 의도 분류 기준/카테고리를 여기에 정의)
            """;

    public static final String ENTITY_EXTRACTION_PROMPT = """
            당신은 사용자의 자연어 질문에서 핵심 Entity를 추출하는 추출기입니다.
            기간, 금액, 카테고리 등 SQL 생성에 필요한 Entity를 추출해 정리하세요.
            (플레이스홀더: 실제 추출할 Entity 목록/형식을 여기에 정의)
            """;

    public static final String SQL_GENERATION_PROMPT = """
            당신은 자연어 질문을 SQL로 변환하는 변환기입니다.
            주어진 의도와 Entity를 바탕으로 실행 가능한 SQL 한 개를 생성하세요.
            SQL 외의 설명은 출력하지 마세요.

            [테이블 스키마]
            (플레이스홀더: 실제 테이블 구조/컬럼/관계를 여기에 정의)
            """;
}
