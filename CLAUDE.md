# AI 챗봇 프로젝트

## 프로젝트 개요

AWS Bedrock의 Claude 모델을 사용하는 웹 기반 AI 챗봇 애플리케이션

## 기술 스택

- Frontend: React + TypeScript + Vite
- Backend: Java + Spring Boot + Spring Data JPA
- AI: AWS Bedrock (Claude Sonnet 4.6 / Haiku 4.5)
- API: Converse API with ConverseStream
- Streaming: Server-Sent Events (SSE)

## 프로젝트 구조

chatbot/
├── frontend/ # React 클라이언트
├── backend/ # Spring Boot 서버
├── docker-compose.yml
└── CLAUDE.md

## 코딩 컨벤션

### 테스트

- TDD 방식으로 개발: 테스트 먼저 작성
- 테스트 파일: *Test.java
- 테스트 프레임워크: JUnit5

### 자바

- 명시적 타입 선언 사용
- 와일드카드 import 사용 금지
- interface 우선 사용

### 에러 처리

- 커스텀 에러 클래스 사용
- 에러 메시지는 사용자 친화적으로

### 네이밍

- 변수/함수: camelCase
- 클래스/인터페이스: PascalCase
- 상수: UPPER_SNAKE_CASE
- 파일: PascalCase (Java 클래스명과 동일)

## 구현하지 않을 것 (Scope 외)

다음 기능은 이번 프로젝트 범위에 포함되지 않습니다.
관련 코드를 생성하지 마세요.

- 사용자 인증/인가 (로그인, 회원가입)
- 데이터베이스 연동 (세션 기반 메모리 저장 사용)
- 파일 업로드
- 이미지/음성 입력 (멀티모달)
- RAG (검색 증강 생성)

## AWS 설정

- Region: us-east-1
- 모델 ID: global.anthropic.claude-sonnet-4-6
- 개발 시 Haiku 사용 가능: global.anthropic.claude-haiku-4-5-20251001-v1:0

## 설계 결정 사항

- 아키텍처: 3-tier (React → Spring Boot → AWS Bedrock)
- 스트리밍 방식: SSE (Server-Sent Events)
- 세션 저장: 인메모리 (데이터베이스 미사용)
- API 스타일: RESTful + SSE 스트리밍
- 에러 메시지: 사용자 응답은 한국어, 내부 로그는 영어

테스트 코드가 명세 역할을 합니다.
테스트 파일을 참조하여 구현 코드를 생성하세요.
