# Backend 패키지 구조

베이스 패키지: `com.chatbot.backend`

| 패키지 | 역할 |
|--------|------|
| `config.aws` | AWS SDK 설정/Bedrock 클라이언트 빈 + 우리 도메인 데이터 객체/스트리밍 이벤트 |
| `controller` | 표현 계층 — HTTP 요청/응답 처리 |
| `domain` | 도메인 객체 및 영속 계층 |
| `service` | 비즈니스 로직 계층 |

## `config.aws` 하위 클래스

AWS SDK 타입(`software.amazon.awssdk....`)은 `BedrockService` 내부에서만 다루고,
외부(컨트롤러/클라이언트)에는 이 패키지의 타입으로 변환해서 노출한다.
즉 이 패키지는 **"우리 쪽 표현 계층용 데이터 객체"** 모음.

### 대화 데이터 객체

| 클래스 | 역할 |
|--------|------|
| `Message` | 대화의 한 메시지 — `role`(user/assistant 문자열) + `content`(`ContentBlock` 리스트) |
| `ContentBlock` | 메시지 안의 콘텐츠 블록 — 현재는 `text` 한 필드만. 멀티모달 확장 여지를 위해 별도 객체로 둠 |
| `ConversationRequest` | 클라이언트 → 백엔드 요청 DTO — `messages`, `systemPrompt`, `maxTokens`, `temperature` |
| `ConversationResponse` | 백엔드 → 클라이언트 동기 응답 DTO (`/converse` 비스트리밍용) — `content`, `stopReason`, `usage` |
| `TokenUsage` | 토큰 사용량 — `inputTokens`, `outputTokens` |
| `Session` | 인메모리 세션 (현재 미사용, 추후 사용 예정) — `sessionId` 기준으로 `messages` 누적 보관, `createdAt`/`updatedAt` 관리. 루트 CLAUDE.md의 "세션 저장: 인메모리" 정책 구현용 |

### 스트리밍 이벤트 (SSE로 클라이언트에 흘려보냄)

`StreamEvent`가 공통 부모 인터페이스. 각 구현체는 `getType()`으로 자기 타입 문자열을 노출하며, 클라이언트는 이 문자열로 분기 처리.

| 클래스 | `getType()` | 언제 발생 / 무엇이 들어있나 |
|--------|-------------|----------------------------|
| `StreamEvent` (interface) | — | 모든 스트리밍 이벤트의 공통 타입 마커 — `getType()` 한 메서드 정의 |
| `TextDeltaEvent` | `text_delta` | 모델이 토큰 단위로 흘려보내는 텍스트 조각 — `text` |
| `ToolUseStartEvent` | `tool_use_start` | 도구 호출 시작 — `toolUseId`(추적 ID), `toolName` |
| `ToolResultEvent` | `tool_result` | 도구 실행이 끝난 시점 — `toolUseId`, `toolResult` 문자열 |
| `MessageStopEvent` | `message_complete` | 한 응답 메시지 완료 시점 — `stopReason`, `usage`(토큰 수) |
| `ErrorEvent` | `error` | 스트리밍 도중 에러 — 에러 `message` 문자열 |

### 전체 흐름 요약

요청: 클라이언트 → `ConversationRequest` → `BedrockService.converse(...)` 또는 `converseStream(...)`
   ↓ (내부에서 SDK Message/ContentBlock으로 변환해 Bedrock 호출)

응답 (비스트리밍): SDK 응답 → `ConversationResponse` → 클라이언트

응답 (스트리밍): SDK 스트림 → 토큰/도구 호출/완료/에러를 위 5개 `StreamEvent` 구현체로 변환 → SSE로 push
