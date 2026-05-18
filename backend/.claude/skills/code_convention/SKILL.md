---
name: code_convention
description: Java 백엔드 코드를 작성, 수정, 리뷰할 때 사용한다. Optional, Stream, 람다, final, String null 체크, Lombok, 줄바꿈/띄어쓰기 등 백엔드 코딩 컨벤션 규칙을 정의한다. 자바 파일(.java) 작업 시 항상 트리거된다.
---

# Backend 코딩 컨벤션

이 문서는 백엔드 Java 코드 작성/리뷰 시 반드시 따라야 하는 규칙을 정의한다.

## 사용 지침 (Claude Code용)

**Java 코드를 작성, 수정, 리뷰하기 전에 반드시 다음을 수행한다:**

1. 이 문서(`.claude/skills/code_convention/SKILL.md`)의 모든 규칙을 숙지한다.
2. **Read 도구를 사용해 다음 파일을 읽고 구체적인 예시를 함께 참고한다:**
   - `.claude/skills/code_convention/reference/Reference.md`
3. 규칙 위반 여부를 판단할 때 위 레퍼런스 파일의 예시 코드를 기준으로 삼는다.

### 작성 규칙

- **수정 내용**과 **수정 이유**는 항목 순서를 동일하게 맞춘다 (1번 항목끼리, 2번 항목끼리 대응).
- 수정 이유에는 반드시 **컨벤션의 섹션 번호와 이름**을 명시한다. (예: "3. Optional - .get() 호출 규칙")
- 단순 코드 작성(신규)인 경우 "수정 내용"은 "신규 작성"으로, "수정 이유"는 적용된 컨벤션 항목들을 나열한다.
- 컨벤션과 무관한 변경(비즈니스 로직 수정 등)은 이 목록에 포함하지 않는다.

### 예시

````markdown
## 📋 적용한 컨벤션

### 수정 내용
- `user.getName() != null && !user.getName().isEmpty()` → `StringUtils.hasText(user.getName())`로 변경
- Stream의 `.filter().map().collect()`를 메서드 단위로 줄바꿈
- 람다 `item -> item.getPrice()`를 메서드 레퍼런스 `Item::getPrice`로 변경

### 수정 이유
- `6. String null / 빈 문자열 체크` — `str != null` 직접 비교 금지, `StringUtils.hasText()` 사용
- `8. 줄바꿈 / 띄어쓰기 규칙 - Stream` — 3개 이상 메서드 체이닝은 줄바꿈
- `7. 람다 표현식 - 메서드 레퍼런스 우선 사용` — 단순 메서드 호출은 `::`로 대체
````

---

## 1. Java 버전

Java 11 이하 문법만 사용한다.

- `record` 사용 금지 (Java 14+)
- `sealed class` 사용 금지 (Java 17+)
- `switch` 표현식 사용 금지 (Java 14+)
- `text block` 사용 금지 (Java 15+)

---

## 2. Lombok

### 허용 어노테이션
- `@Getter`
- `@Setter`
- `@Data`
- `@RequiredArgsConstructor`
- `@AllArgsConstructor`
- `@Slf4j`

### 사용 금지
- `@Value`
- `@Setter` (단일 필드 변경 목적)
  - 단일 필드 하나만 변경이 필요한 경우 `@Setter` 대신 해당 로직을 담은 메서드를 작성한다.

---

## 3. Optional

### 사용 권장
- 메서드 반환 타입에서 "값이 없을 수 있음"을 명시할 때 (특히 Repository의 단건 조회)
- `null` 체크 후 분기/변환 로직이 이어질 때 (`map`, `filter`, `orElse`로 표현 가능한 경우)

### 사용 금지
- **필드 타입으로 사용 금지** (직렬화/JPA 호환성 문제)
- **메서드 파라미터로 사용 금지** — 호출자가 `Optional.of(...)`로 감싸야 하는 부담을 줌
- **컬렉션 타입 감싸기 금지** — 빈 컬렉션(`Collections.emptyList()`)을 반환하면 됨
- 단순 `null` 체크 한 번이면 끝나는 경우 — `if (x != null)`이 더 짧고 명확

### `.get()` 호출 규칙
- `isPresent()` 체크 없이 `.get()` 호출 금지
- 가능하면 `orElse`, `orElseThrow`, `orElseGet`으로 대체

📎 Reference: `.claude/skills/code_convention/reference/Reference.md` → "Optional 예시"

---

## 4. Stream

### 사용 권장
- 컬렉션 변환(`map`), 필터링(`filter`), 집계(`reduce`, `count`, `sum`)가 **연속될 때**
- 결과를 다른 컬렉션 타입으로 변환할 때 (`toList`, `toMap`, `groupingBy`)

### 사용 금지 / 지양
- 단순 순회만 하는 경우 — 일반 `for` 루프가 더 명확
- Stream 내부에서 **외부 상태를 변경**하는 경우 (`forEach` 안에서 외부 리스트에 add 등) — `for` 루프로 작성
- 예외를 던지는 로직이 들어가는 경우 — checked exception 처리가 지저분해짐
- `peek()`을 디버깅 외 용도로 사용 금지
- **3단계 이상 중첩된 stream** — 가독성이 떨어지므로 중간 변수로 분리하거나 메서드로 추출
- 스트림 사용 후엔 가독성을 위해 한 줄 띄울 것

📎 Reference: `.claude/skills/code_convention/reference/Reference.md` → "Stream 예시"

---

## 5. final 키워드

### 사용
- **인스턴스 변수(필드)에만 사용한다**
  - 불변성을 보장하고 객체 상태가 변경되지 않음을 명시할 때

### 사용 금지
- **지역 변수에 사용 금지**
- **메서드 파라미터에 사용 금지**
- **메서드 / 클래스에 사용 금지** (상속 제한이 명확히 필요한 경우 제외)

---

## 6. String null / 빈 문자열 체크

`str != null` 직접 체크 대신 `org.springframework.util.StringUtils`의 메서드를 사용한다.

### 사용 권장
- **`StringUtils.hasText(str)`** — null이 아니고, 빈 문자열도 아니고, 공백만 있지도 않은지 확인
- **`StringUtils.hasLength(str)`** — null이 아니고, 빈 문자열도 아닌지 확인 (공백은 유효한 값으로 봄)

### 사용 금지
- `str != null` 직접 비교 금지
- `str != null && !str.isEmpty()` 같은 중복 체크 금지
- `str != null && !str.trim().isEmpty()` 같은 중복 체크 금지

### 의미별 사용 구분
- **"실질적인 값이 있는가"** → `StringUtils.hasText(str)` (공백만 있는 경우도 false)
- **"길이가 1 이상인가"** → `StringUtils.hasLength(str)` (공백도 유효한 값으로 인정)

### 주의
- import 경로는 반드시 `org.springframework.util.StringUtils` 사용
- Apache Commons의 `org.apache.commons.lang3.StringUtils`와 혼동 금지

📎 Reference: `.claude/skills/code_convention/reference/Reference.md` → "String null / 빈 문자열 체크 예시"

---

## 7. 람다 표현식

### 사용 권장
- 함수형 인터페이스(`Function`, `Predicate`, `Consumer`, `Supplier` 등) 구현 시
- Stream API와 함께 사용할 때
- 짧고 명확하게 한 줄로 표현 가능한 경우

### 사용 금지 / 지양
- **3줄 이상의 람다 본문 금지** — 별도 메서드로 추출 후 메서드 레퍼런스(`::`)로 참조
- **중첩 람다 금지** — 람다 안에 또 람다가 들어가면 가독성 급격히 저하
- **람다 내부에서 외부 상태 변경 금지** — 사이드 이펙트가 있으면 일반 `for` 루프 사용
- **예외를 던지는 로직 지양** — checked exception 처리가 지저분해짐, 필요시 메서드로 추출

### 메서드 레퍼런스(`::`) 우선 사용
- 람다가 단순히 메서드 하나만 호출하는 경우 메서드 레퍼런스로 대체한다.

### 파라미터 작성 규칙
- **파라미터가 1개일 때는 괄호 생략**
- **파라미터 타입은 생략** (컴파일러가 추론)
- 파라미터 이름은 의미가 드러나도록 작성, 단순 변환은 `it`, `e` 같은 짧은 이름 허용

### 본문 작성 규칙
- **한 줄이면 중괄호와 `return` 생략**
- **두 줄 이상이면 중괄호 사용**
- 복잡한 람다는 메서드로 추출

📎 Reference: `.claude/skills/code_convention/reference/Reference.md` → "람다 표현식 예시"

---

## 8. 줄바꿈 / 띄어쓰기 규칙

가독성을 위해 아래 케이스에서는 **연산자/메서드 단위로 줄바꿈**한다.

### Stream
- 각 중간 연산(`filter`, `map`, `sorted` 등)과 종료 연산(`collect`, `forEach` 등)은 **줄을 나눠 작성**
- 점(`.`)을 줄 앞에 두어 메서드 체이닝임을 명시

### Builder
- 각 `setter` 호출마다 **줄을 나눠 작성**
- `.build()`도 별도 줄에 작성

### if / else / for / while 문
- **중괄호 `{}`는 한 줄짜리라도 반드시 사용**
- `if`, `else`, `for`, `while` 키워드와 `(` 사이는 **한 칸 띄움**
- `)`와 `{` 사이도 **한 칸 띄움**
- `else`, `else if`는 **닫는 중괄호 `}`와 같은 줄**에 작성

### 메서드 체이닝 (공통 규칙)
- **2개 이하**면 한 줄에 작성 가능
- **3개 이상**이면 메서드마다 줄바꿈
- 줄바꿈 시 점(`.`)을 다음 줄 **앞쪽**에 배치하고 **4 space 들여쓰기**

### 연산자 / 콤마
- 이항 연산자(`+`, `-`, `==`, `&&` 등) **앞뒤로 한 칸 띄움**
- 콤마(`,`) **뒤에만 한 칸 띄움**, 앞에는 띄우지 않음

📎 Reference: `.claude/skills/code_convention/reference/Reference.md` → "줄바꿈 / 띄어쓰기 예시"

## 9. 조건문 규칙

- `if`는 허용하되 `else if`는 사용하지 않는다.
- Early return이 가능하면 early return으로 작성한다 (`else` 생략).