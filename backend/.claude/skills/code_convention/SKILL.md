---
name: code-convention
description: Java 백엔드 코드를 작성, 수정, 리뷰할 때 사용한다. Lombok, Optional, Stream, 람다, final, Bean Validation, 줄바꿈/띄어쓰기, 조건문 등 백엔드 코딩 컨벤션 규칙을 정의한다. 자바 파일(.java) 작업 시 트리거.
---

> 구체적인 코드 예시는 같은 디렉터리의 [`reference.md`](./reference.md)에 정리되어 있다.
> 규칙만으로 판단이 모호하면 `reference.md`를 열어 해당 섹션 번호를 찾아본다.

## 1. Java 버전

Java 16+ 문법을 사용한다. 단, 아래 문법은 사용하지 않는다.

- `record` 사용 금지 (불변 DTO도 `@Getter` + `@Builder` 클래스로 작성)
- `sealed class` 사용 금지
- `switch` 표현식 사용 금지 (전통 `switch` 문은 허용)
- `text block`(`"""..."""`) 사용 금지

Stream의 종료 연산은 Java 16+의 `.toList()`를 사용한다 (자세한 내용은 4번 섹션 참고).

---

## 2. Lombok

### 허용 어노테이션과 조합

- **Service/Controller**: `@RequiredArgsConstructor` + `@Service` / `@RestController` + (`@Transactional` 또는 `@RequestMapping`)
- **DTO**: `@Getter` + `@Builder` + `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
- **로깅**: `@Slf4j`

### 사용 금지

- `@Setter` — 단일 필드 변경이 필요한 경우 의미를 담은 메서드를 작성한다 (`modify()`, `updateCapacity()`, `connectEvent()` 등)
- `@Data` — 너무 많은 어노테이션을 한 번에 포함하므로 사용하지 않음
- `@AllArgsConstructor` (public) — DTO는 `@Builder`로 명시 생성, 엔티티는 정적 팩토리/Builder로 생성

### Builder 생성자 가시성

DTO의 `@Builder`를 붙인 생성자는 **private**으로 둔다. 외부에서는 반드시 빌더만 사용하도록 강제.

📎 Reference: `reference.md` → "2. Lombok"

---

## 3. Optional

### 사용 권장

- **Repository 단건 조회의 반환 타입** (`Optional<User> findByEmail(String email)`)
- `.orElseThrow(() -> new NotFoundException(Reason, Message))` 패턴이 **표준**
- `.map(...).orElse(...)` 체이닝으로 분기 표현
- 존재 시 예외를 던지는 경우 `.ifPresent(u -> { throw ... })`

### 사용 금지

- **필드 타입으로 사용 금지** (직렬화/JPA 호환성)
- **메서드 파라미터로 사용 금지** — 호출자가 `Optional.of(...)`로 감싸야 하는 부담
- **컬렉션 타입 감싸기 금지** — 빈 컬렉션을 반환하면 됨
- **`isPresent()` 체크 없는 `.get()` 호출 금지** — `orElseThrow`/`orElse`/`orElseGet`으로 대체

📎 Reference: `reference.md` → "3. Optional"

---

## 4. Stream

### 사용 권장

- 컬렉션 변환(`map`), 필터링(`filter`), 집계가 **연속될 때**
- 종료 연산은 **`.toList()`** 우선 (`Collectors.toList()` 사용하지 않음)
- 그룹화/맵 변환은 `Collectors.toMap`, `Collectors.groupingBy` 사용
- **메서드 레퍼런스(`::`)** 로 변환 가능한 람다는 변환 (`Item::getName`)

### 사용 금지 / 지양

- 단순 순회만 하는 경우 (`forEach`로 로깅 등) → 일반 `for` 루프
- Stream 내부에서 **외부 상태 변경** (`forEach` 안에서 외부 리스트에 add 등) → `for` 루프
- 예외를 던지는 로직 (checked exception 처리가 지저분해짐)
- `peek()`을 디버깅 외 용도로 사용 금지
- **3단계 이상 중첩된 stream** — 중간 변수로 분리하거나 메서드로 추출
- Stream 사용 후엔 **가독성을 위해 한 줄 띄울 것**

📎 Reference: `reference.md` → "4. Stream"

---

## 5. final 키워드

### 사용

- **인스턴스 필드에만 사용한다** — Service/Controller의 `private final` 의존성 필드가 대표 예
- 불변성을 보장하고 객체 상태가 변경되지 않음을 명시할 때

### 사용 금지

- **지역 변수**에 사용 금지
- **메서드 파라미터**에 사용 금지
- **메서드 / 클래스**에 사용 금지 (상속 제한이 명확히 필요한 경우 제외)

📎 Reference: `reference.md` → "5. final 키워드"

---

## 6. String null / 빈 문자열 체크

입력 검증은 **Bean Validation 어노테이션**으로 처리하고, 동적인 String null 체크는 거의 사용하지 않는다.

### 사용 권장

- **컨트롤러/DTO 입력 검증**: `@NotBlank`, `@NotNull`, `@Email`, `@Pattern` 같은 표준 Bean Validation 어노테이션. 도메인에 맞는 커스텀 validator도 허용
- 컨트롤러에서 `@Valid` / `@Validated`로 활성화

### 동적 체크가 정말 필요할 때

- `if (str != null && !str.isBlank())` 같이 의미를 명시적으로 작성
- `org.apache.commons.lang3.StringUtils`는 사용 금지 (의존성 추가 안 함)
- `org.springframework.util.StringUtils.hasText(...)`는 강제하지 않지만, 사용해도 무방

### 주의

- 한 DTO 안에서 검증 어노테이션과 if 체크를 혼용하지 않는다 (어노테이션으로 통일).

📎 Reference: `reference.md` → "6. String 체크"

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

람다가 단순히 메서드 하나만 호출하는 경우 메서드 레퍼런스로 대체한다.

### 파라미터 작성 규칙

- **파라미터가 1개일 때는 괄호 생략** (`u -> u.getId()`)
- **파라미터 타입은 생략** (컴파일러가 추론)
- 파라미터 이름은 의미가 드러나도록 작성. 단순 변환은 `u`, `e`, `dto` 같은 짧은 이름 허용

### 본문 작성 규칙

- **한 줄이면 중괄호와 `return` 생략**
- **두 줄 이상이면 중괄호 사용**
- 복잡한 람다는 메서드로 추출

📎 Reference: `reference.md` → "7. 람다 표현식"

---

## 8. 줄바꿈 / 띄어쓰기 규칙

가독성을 위해 아래 케이스에서는 **연산자/메서드 단위로 줄바꿈**한다.

### Stream

- 각 중간 연산(`filter`, `map`, `sorted` 등)과 종료 연산(`toList`, `forEach` 등)은 **줄을 나눠 작성**
- 점(`.`)을 **줄 앞에** 두어 메서드 체이닝임을 명시
- Stream 종료 후 다음 코드와 **한 줄 띄움**

### Builder

- 각 setter 호출마다 **줄을 나눠 작성**
- `.build()`도 별도 줄에 작성
- 점(`.`)을 줄 앞에 둠

### if / for / while 문

- **중괄호 `{}`는 한 줄짜리라도 반드시 사용**
- `if`, `for`, `while` 키워드와 `(` 사이는 **한 칸 띄움**
- `)`와 `{` 사이도 **한 칸 띄움**
- `else`는 9번 섹션 참고 (사용하지 않음)

### 메서드 체이닝 (공통 규칙)

- **2개 이하**면 한 줄에 작성 가능
- **3개 이상**이면 메서드마다 줄바꿈
- 줄바꿈 시 점(`.`)을 다음 줄 **앞쪽**에 배치

### 연산자 / 콤마

- 이항 연산자(`+`, `-`, `==`, `&&` 등) **앞뒤로 한 칸 띄움**
- 콤마(`,`) **뒤에만 한 칸 띄움**, 앞에는 띄우지 않음

📎 Reference: `reference.md` → "8. 줄바꿈 / 띄어쓰기"

---

## 9. 조건문 규칙

- `if`는 허용하되 **`else if`는 사용하지 않는다.**
- **`else`도 지양** — Early return으로 처리할 수 있다면 early return을 우선한다.
- 예외 throw나 `.orElseThrow()`를 early return처럼 활용한다.
- 클래스 어노테이션 순서 (Service 기준 권장):
  `@Transactional(readOnly = true)` → `@RequiredArgsConstructor` → `@Service`
  Controller의 경우:
  `@RestController` → `@RequiredArgsConstructor` → `@RequestMapping`

📎 Reference: `reference.md` → "9. 조건문 규칙"
