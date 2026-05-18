---
name: service-test-style
description: 서비스 테스트를 짤때 따르는 개인 코딩 스타일/관례 모음. 새로운 Service Test를 작성하거나 기존 테스트를 수정할 때 트리거로 사용.
---

> 구체적인 코드 예시는 같은 디렉터리의 [`reference.md`](./reference.md)에 정리되어 있다.
> 규칙만으로 판단이 모호하면 `reference.md`를 열어 해당 섹션 번호를 찾아본다.

## 1. 테스트 클래스 기본 골격

- **모킹을 쓰지 않고 실제 Spring 컨텍스트 + 트랜잭션으로 통합 테스트한다.** `@SpringBootTest + @ActiveProfiles("test") + @Transactional` 조합을 기본으로 한다 (공통 부모 클래스를 두고 상속하는 방식 선호).
- 의존성은 **필드 `@Autowired`** 로 주입한다. 생성자 주입/필드 final 안 씀.
- 접근 제어자: 테스트 클래스는 **package-private (`class`)**, 필드도 대부분 default 또는 private.

## 2. 패키지 구조와 네이밍

- 서비스 패키지를 그대로 미러링한다. 예) `service.event.EventService` → `service.event.EventServiceTest`
- 테스트 클래스명: `{대상클래스}Test`
- 테스트 메서드명: **camelCase + 시나리오 서술형**
  - 기본 동작: `saveEvent`, `getRegularEvent`, `deleteBookmark`
  - 조건 분기: `saveEvent_WhenNotRegisterFile_UploadFileIsNull`
  - 예외: `editEventWhenNotRegisteredByMe`, `signUp_WhenDuplicateNameOrEmail_ThrowsException`
  - 패턴은 `메서드명_When조건_Then결과` 또는 `메서드명When조건` 두 가지를 섞어 쓴다 (When/Then이 명확할 때는 언더스코어로 끊어줌).

## 3. `@DisplayName` 규칙 (가장 중요)

- **모든 `@Test`에는 한글 `@DisplayName`을 단다. 예외 없음.**
- 한 문장으로 시나리오를 그대로 설명한다. "X 한다." / "X 일때 Y 한다." / "X 라면 예외가 발생 한다." 같은 종결어.
- 어순은 **상황 → 동작 → 결과** 순으로 자연스럽게 쓴다.
- `@DisplayName`을 `@Test`보다 **위에** 두는 게 일관된 스타일이다.

## 4. given / when / then 주석

테스트 본문은 항상 세 블록으로 나누고, **각 블록 위에 `//given`, `//when`, `//then` 주석을 단다.**

- 예외 검증처럼 when/then이 한 줄로 끝날 땐 **`//when //then`** 한 줄로 합쳐 쓴다.

## 5. 테스트 데이터는 private 헬퍼 메서드로

엔티티/DTO 생성은 새로운 객체 빌더를 본문에 직접 쓰지 않고 **클래스 하단의 private 헬퍼 메서드**로 모은다.

- 메서드명 규칙: `createUser`, `createEvent`, `createBookmark`, `createSaveDto`, `createEditDto` …
- **오버로딩으로 변형을 표현한다.** 필요한 필드만 받는 시그니처를 여러 개 만든다.
- 엔티티는 **Lombok `@Builder`** 로 생성한다.
- 외부 상태가 필요 없는 헬퍼는 `private static`, 인스턴스에 묶일 이유 없으면 그냥 `private`. 일관성보다 가독성에 맞춰 둔다.
- 헬퍼는 **파일 맨 아래에 모아둔다**. 테스트 메서드들을 위에, 헬퍼는 아래에.

## 6. 상태 정리 (`@BeforeEach cleanUp`)

연관된 엔티티가 많아 트랜잭션 롤백만으로 충분치 않거나, 시퀀스/연관 정리가 필요한 테스트 클래스에서는 `@BeforeEach`로 직접 정리한다.

- 메서드명은 `cleanUp` 으로 통일.
- **자식 → 부모 순서**로 `deleteAllInBatch()`를 호출한다 (FK 제약 고려).
- 모든 테스트에 강제로 넣지는 않는다. 데이터 격리 이슈가 보이는 클래스에만 둔다.

## 7. Assertion 스타일 (AssertJ)

- 기본 import: `assertThat`, `assertThatThrownBy` 는 static import.
- 여러 필드를 검증할 땐 **`extracting(...).containsExactly(...)`** 패턴을 우선한다.
- 컬렉션은:
  - 순서 보장 필요 → `containsExactly(...)`
  - 순서 무관 → `containsExactlyInAnyOrder(...)`
  - 여러 필드 묶음 → `org.assertj.core.groups.Tuple.tuple(...)`
- 예외 메시지는 **실제 운영 메시지를 그대로** 검증한다 (`hasMessage(...)`). 메시지 자체가 사양의 일부.
- 부재 검증은 `assertThatThrownBy(() -> repo.findById(id).orElseThrow()).isInstanceOf(NoSuchElementException.class);` 또는 `assertThat(repo.findById(id)).isEmpty();` 둘 다 사용.

## 8. 시간 관련 테스트

- 현재 시각이 동작에 영향을 주는 로직은 **`LocalDateTime`을 파라미터로 받는 형태로 서비스를 설계**하고, 테스트에서 명시적으로 주입한다.
- `LocalDateTime.now()`에 의존하지 않는다.

## 9. 파라미터화 테스트

여러 케이스를 한 메서드로 묶고 싶을 때 `@ParameterizedTest + @CsvSource`를 쓴다. 구분자는 `-`.

## 10. JPA flush/clear 명시

수정/삭제 후 영속성 컨텍스트 캐시 때문에 검증이 흐려질 수 있는 경우 `EntityManager`를 주입받아 명시적으로 비운다.

- `em.flush(); em.clear();`를 **한 줄에 붙여 쓰는** 게 스타일.

## 11. 동시성 테스트

`ExecutorService + CountDownLatch` 로 작성한다. 트랜잭션이 동시성 테스트에 방해될 땐 메서드에 `@Transactional(propagation = Propagation.NOT_SUPPORTED)`.

- 운영에서 매번 돌리고 싶진 않은 무거운 케이스엔 `@Disabled`를 붙여 보존.

## 12. `@Disabled` 사용

- 미완성/리팩토링 중인 테스트는 지우지 않고 `@Disabled` + DisplayName으로 의도를 남긴다.
- 코드 위에 `// 영속성 전이 문제` 같은 **짧은 한글 주석으로 이유를 적어두는 경우가 있다.**

## 13. import 스타일

- `assertThat`, `assertThatThrownBy` 는 static import.
- enum 상수도 적극적으로 static import 해서 가독성을 높임.

## 14. 한 테스트 = 하나의 시나리오

- 한 메서드 안에서 너무 많은 검증을 묶지 않는다. **시나리오가 달라지면 메서드를 분리한다.**

## 15. 매직넘버는 변수로

검증값이 의미를 가질 땐 인라인 리터럴 대신 지역변수로 빼서 이름으로 의미를 드러낸다.

## 16. 작성 후 반드시 테스트 실행으로 검증

- 테스트를 새로 짜거나 수정한 뒤에는 **반드시 직접 실행해서 전부 통과하는지 확인한다.** 작성만 하고 끝내지 않는다.
- 실행 단위는 좁은 것부터 점진적으로:
  1. 방금 작성/수정한 단일 테스트 메서드
  2. 해당 테스트 클래스 전체
  3. 영향 범위가 의심되면 모듈 전체 (`./gradlew test` 또는 `mvn test`)
- 실패하면 **빨간색 테스트가 0개가 될 때까지** 원인을 파악해서 고친다. "내 변경과 관련 없어 보이는 실패"라도 그냥 넘기지 않는다 — 트랜잭션 누수, FK 제약, 데이터 격리 이슈일 가능성이 높다.
- 통과해도 다음을 한 번 더 본다:
  - 콘솔에 `@Disabled`로 인해 skip된 테스트가 의도한 것만 있는가?
  - 예상보다 너무 빨리 끝났다면 실제로 실행은 됐는지 (`tests: N passed`) 숫자를 확인.
- CI에 푸시하기 전 로컬에서 **최소한 같은 패키지 안의 테스트는 그린**임을 확인하고 커밋한다.

---

## 새 서비스 테스트 작성 체크리스트

1. [ ] `@SpringBootTest + @ActiveProfiles("test") + @Transactional` (또는 상속한 공통 부모) 셋업이 되었는가?
2. [ ] `@DisplayName` 한글 한 줄로 시나리오 설명 달았는가?
3. [ ] `//given`, `//when`, `//then` 주석 세 블록 구조인가?
4. [ ] 엔티티/DTO 생성은 클래스 하단 private 헬퍼로 뺐는가?
5. [ ] AssertJ `extracting + containsExactly(InAnyOrder)` 패턴 썼는가?
6. [ ] 예외 검증은 `assertThatThrownBy` + `isInstanceOf` + `hasMessage` 까지 적었는가?
7. [ ] 수정/삭제 검증에 `em.flush(); em.clear();` 가 필요한지 확인했는가?
8. [ ] 시간 의존 로직은 `LocalDateTime`을 인자로 주입했는가?
9. [ ] 한 테스트당 하나의 시나리오만 검증하는가?
10. [ ] **작성/수정한 테스트를 실제로 실행해서 전부 통과(green)했는가?** 단일 메서드 → 클래스 전체 → 필요 시 모듈 전체 순으로 확인했는가?