# Backend 패키지 구조

베이스 패키지: `com.chatbot.backend`

## 한 줄 요약

> AWS SDK와 우리 코드 사이에 **번역 계층**을 둬서, SDK 버전이 올라가도 컨트롤러·프론트엔드 계약은 흔들리지 않게 만든다.

그 번역 계층의 타입들이 모여 있는 곳이 **`config.aws`** 패키지다. TS 레퍼런스의 `backend/src/types/index.ts`에 해당한다.

## 왜 SDK 타입을 그대로 쓰지 않나

AWS SDK(`software.amazon.awssdk....`)는 자체적으로 `Message`, `ContentBlock`, 각종 응답 객체를 갖고 있다. 이걸 컨트롤러나 프론트엔드에 그대로 노출하면 두 가지 문제가 생긴다.

1. **SDK 버전이 올라갈 때마다 우리 코드 전체가 흔들린다.** SDK 메이저 업데이트에서 필드명이나 구조가 바뀌면, SDK 타입을 직접 쓰는 곳을 모두 손봐야 한다.
2. **SDK 표현이 우리 도메인보다 풍부하거나 다르다.** SDK Message는 멀티모달·도구 호출·구조화된 콘텐츠를 다 표현하지만, 우리 챗봇은 그중 일부만 쓴다.

해결책: **SDK 타입은 `BedrockService` 내부에서만 만진다.** 바깥(컨트롤러·프론트·다른 서비스)에는 우리 자체 객체(`config.aws` 패키지)만 보여준다.

```
[React]  ←→  [Controller]  ←→  [BedrockService]  ←→  [AWS SDK]  ←→  [Bedrock]
                            우리 타입 │ SDK 타입
                                     ↑
                              여기서만 양방향 변환
```

이 격리 덕분에 SDK 버전이 바뀌어도 손대는 곳은 `BedrockService` 한 곳뿐이다. 컨트롤러, 프론트엔드 DTO 계약, 다른 서비스 호출자들은 영향받지 않는다.

## 패키지 한눈에

| 패키지 | 역할 |
|--------|------|
| `config.aws` | **우리 표현 계층 타입들** — 대화 데이터 객체 + 스트리밍 이벤트 (아래에서 상세) |
| `config` | Spring 빈 설정 (Bedrock 클라이언트, CORS, WebClient, 시스템 프롬프트 등) |
| `controller` | HTTP 요청/응답 처리 (REST + SSE) |
| `domain` | JPA 엔티티 — `Session`, `Message`(히스토리 저장용) |
| `repository` | Spring Data JPA 리포지토리 |
| `service` | 비즈니스 로직 — `BedrockService`, `SessionManager`, `MessageHistoryService` 등 |
| `dto` | 컨트롤러용 요청/응답 DTO |
| `exception` | 커스텀 예외 + 전역 핸들러 |

> 헷갈리기 쉬운 두 `Message`
> - `config.aws.Message` — Bedrock에 보내는 **대화 메시지 표현**(role + ContentBlock 리스트)
> - `domain.Message` — **히스토리 저장용 JPA 엔티티**(sessionId + role + content + createdAt)

## `config.aws` — 우리 표현 계층 타입 상세

여기 있는 클래스들은 SDK가 어떻게 생겼든 **우리 쪽 코드와 클라이언트가 보는 모양을 고정**시킨다.

### 대화 데이터 객체

| 클래스 | 역할 |
|--------|------|
| `Message` | 대화의 한 메시지 — `role`(user/assistant 문자열) + `content`(`ContentBlock` 리스트) |
| `ContentBlock` | 메시지 안의 콘텐츠 한 조각 — 현재는 `text` 한 필드. 멀티모달 확장 여지를 위해 별도 객체로 둠 |
| `ConversationRequest` | 프론트 → 백엔드 요청 DTO — `messages`, `systemPrompt`, `maxTokens`, `temperature` |
| `ConversationResponse` | 비스트리밍(`/converse`) 응답 DTO — `content`, `stopReason`, `usage` |
| `TokenUsage` | 토큰 사용량 — `inputTokens`, `outputTokens` |
| `Session` | 인메모리 세션(현재 미사용, 추후 사용 예정) — 영속 세션은 `domain.Session` 쪽이 담당 |

### 스트리밍 이벤트 — SSE로 프론트에 흘려보냄

`StreamEvent`가 공통 부모 인터페이스. 각 구현체는 `getType()`으로 자기 타입 문자열을 노출하고, 프론트는 그 문자열로 분기 처리한다.

| 구현체 | `getType()` | 언제 발생 / 무엇이 들어있나 |
|--------|-------------|----------------------------|
| `TextDeltaEvent` | `text_delta` | 모델이 토큰 단위로 흘려보내는 텍스트 조각 — `text` |
| `ToolUseStartEvent` | `tool_use_start` | 도구 호출 시작 — `toolUseId`(추적 ID), `toolName` |
| `ToolResultEvent` | `tool_result` | 도구 실행이 끝난 시점 — `toolUseId`, `toolResult` |
| `MessageStopEvent` | `message_complete` | 한 응답 메시지 완료 — `stopReason`, `usage`(토큰 수) |
| `ErrorEvent` | `error` | 스트리밍 도중 에러 — `message` |

## 요청 한 번이 흐르는 과정

**비스트리밍 (`/converse`)**

1. 프론트가 `ConversationRequest` JSON을 보냄
2. 컨트롤러가 `BedrockService.converse(...)` 호출
3. `BedrockService`가 우리 `Message`/`ContentBlock`을 SDK 타입으로 변환 → Bedrock 호출
4. SDK 응답을 다시 `ConversationResponse`로 변환해서 반환
5. 컨트롤러가 JSON으로 직렬화해 응답

**스트리밍 (`/converseStream`)**

1. 프론트가 SSE로 접속하면서 `ConversationRequest` 전송
2. 컨트롤러가 `BedrockService.converseStream(...)` 호출
3. SDK가 흘려보내는 스트림을 위 5종의 `StreamEvent` 구현체로 매핑 (토큰 → `TextDeltaEvent`, 종료 → `MessageStopEvent`, 에러 → `ErrorEvent` …)
4. 매 이벤트를 SSE로 푸시
