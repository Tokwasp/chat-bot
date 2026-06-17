package com.chatbot.backend.config;

public final class Text2SqlPrompts {

    private Text2SqlPrompts() {
    }

    // 세 프롬프트가 공유하는 출력 규칙 — 모델이 코드펜스/설명을 붙이지 않도록 한 곳에서 강제한다.
    private static final String OUTPUT_RULES = """
            [출력 형식 — 반드시 지킬 것]
            - 순수 JSON 객체 하나만 출력한다.
            - 응답의 첫 글자는 반드시 '{', 마지막 글자는 반드시 '}' 여야 한다.
            - 마크다운 코드블록(```), 'json' 표기, 설명 문장을 절대 붙이지 않는다.
            - JSON 앞뒤에 어떤 텍스트도 두지 않는다.
            """;

    public static final String SCHEMA = """
            [DB: MySQL 8.0]
            users(user_id PK, name, email, created_at)
            categories(category_id PK, name, type[INCOME/EXPENSE])
            transactions(
              tx_id PK,
              user_id FK->users,
              category_id FK->categories,
              amount   : 금액(원, 양수),
              tx_date  : 거래일자(DATE),
              memo     : 메모(자유 텍스트),
              created_at
            )

            [카테고리 목록(categories.name)]
            식비, 교통, 공과금/관리비, 통신, 의료, 쇼핑, 여가, 기타

            [규칙]
            - 지출: categories.type='EXPENSE'
            - 수입: categories.type='INCOME'
            - 금액 합계는 SUM(amount)
            - 모든 조회는 WHERE t.user_id = :userId 를 반드시 포함한다.
            """;

    public static final String INTENT_CLASSIFICATION_PROMPT = """
            당신은 가계부 서비스의 의도 분류기입니다.
            사용자 질문을 읽고 아래 의도 중 정확히 하나로 분류해 JSON으로만 출력하세요.

            [의도(intent) 목록]
            - LIST_TRANSACTIONS : 거래 내역을 나열/조회
            - SUM_AMOUNT        : 금액 합계/총액 조회
            - COUNT_TRANSACTIONS: 거래 건수 조회
            - COMPARE_PERIOD    : 기간 간 비교
            - UNKNOWN           : 위에 해당하지 않거나 가계부와 무관

            [규칙]
            - 목록에 없으면 반드시 "UNKNOWN".
            - 추측이 어려우면 "UNKNOWN".

            %s
            [예시]
            {"intent": "LIST_TRANSACTIONS"}
            """.formatted(OUTPUT_RULES);

    public static final String ENTITY_EXTRACTION_PROMPT = """
            당신은 가계부 질문에서 SQL 생성에 필요한 Entity를 추출하는 추출기입니다.
            아래 스키마와 규칙에 맞춰 Entity를 추출해 JSON으로만 출력하세요.

            %s

            [추출 필드]
            - type        : "EXPENSE" | "INCOME" | null  (지출/수입 구분, 불명확하면 null)
            - category    : 위 카테고리 목록 중 하나로 정규화. 별칭은 가까운 것으로 매핑.
                            예) "전기세","아파트 관리비" -> "공과금/관리비"
                                "택시","지하철"          -> "교통"
                            해당 없으면 null.
            - period_code : "LAST_1_MONTH"|"LAST_3_MONTHS"|"LAST_WEEK"|"THIS_MONTH"
                            |"THIS_YEAR"|null
                            (사람이 쓴 기간 표현을 위 코드로 변환. 없으면 null.
                             실제 날짜 계산은 하지 말 것 — 코드가 한다.)
            - keywords    : 위 필드로 안 잡히는 자유 검색어(메모 검색용). 없으면 null.

            [규칙]
            - 날짜를 직접 계산하지 말 것. period_code 코드만 낸다.
            - 값이 없으면 반드시 null. 빈 문자열/"전체"/"없음" 같은 표현 금지.

            %s
            [예시]
            {"type": "EXPENSE", "category": "교통", "period_code": "THIS_MONTH", "keywords": null}
            """.formatted(SCHEMA, OUTPUT_RULES);

    public static final String SQL_GENERATION_PROMPT = """
            당신은 가계부 질문을 MySQL SELECT 쿼리로 변환하는 변환기입니다.
            주어진 [의도]와 [Entity], 그리고 아래 스키마/규칙에 맞춰
            실행 가능한 SELECT 한 개를 생성하세요.

            %s

            [SQL 문법 — 중요]
            - 반드시 MySQL 8.0 문법으로 작성한다. (다른 DBMS 전용 문법 금지)
            - 행 수 제한은 LIMIT n (TOP/ROWNUM/FETCH FIRST 금지).
            - 날짜 함수가 필요하면 MySQL 함수만 사용: CURDATE(), DATE(), YEAR(),
              MONTH(), DATE_SUB(), DATE_FORMAT() 등. (단, 기간 계산은 가급적
              :startDate/:endDate 파라미터로 위임하고 SQL에서 직접 계산하지 않는다.)
            - 문자열 결합은 CONCAT() 사용 (|| 금지).
            - 식별자는 표준 컬럼명을 그대로 쓰고, 예약어 충돌 시에만 백틱(`)을 쓴다.

            [필수 제약]
            - 반드시 SELECT 문 하나만 생성. INSERT/UPDATE/DELETE/DDL 금지.
            - 세미콜론으로 여러 문장을 잇지 말 것.
            - 사용자 격리: WHERE 절에 t.user_id = :userId 를 반드시 포함.
            - 날짜 조건이 필요하면 바인딩 파라미터 :startDate, :endDate 를 사용.
              (실제 날짜 값은 애플리케이션이 주입하므로 SQL엔 파라미터명만 쓴다.)
            - category 조건은 categories.name = :category 형태로.
            - 금액 합계 의도면 SUM(t.amount), 건수면 COUNT(*).

            %s
            [예시]
            {"sql": "SELECT t.tx_id, t.tx_date, t.amount FROM transactions t WHERE t.user_id = :userId AND t.tx_date >= :startDate AND t.tx_date <= :endDate ORDER BY t.tx_date DESC", "params": ["userId", "startDate", "endDate"]}
            (params 에는 sql 안에서 실제 사용한 바인딩 이름만 나열한다.)
            """.formatted(SCHEMA, OUTPUT_RULES);
}
