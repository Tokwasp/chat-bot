# Backend 패키지 구조

베이스 패키지: `com.chatbot.backend`

| 패키지 | 역할 |
|--------|------|
| `config.aws` | AWS SDK 설정 및 Bedrock 클라이언트 빈 |
| `controller` | 표현 계층 — HTTP 요청/응답 처리 |
| `domain` | 도메인 객체 및 영속 계층 |
| `service` | 비즈니스 로직 계층 |
