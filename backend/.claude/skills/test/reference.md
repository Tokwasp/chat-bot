# Service Test Style — Reference

`SKILL.md`의 각 규칙에 대응하는 실제 코드 예시 모음.
번호는 `SKILL.md`의 섹션 번호와 1:1 매칭된다.

---

## 1. 테스트 클래스 기본 골격

공통 부모를 두고 통합 테스트 셋업을 모아두는 방식:

```java
@Transactional
@ActiveProfiles("test")
@SpringBootTest
public abstract class IntegrationTestSupport {
}
```

```java
class EventServiceTest extends IntegrationTestSupport {

    @Autowired
    EventService eventService;

    @Autowired
    EventRepository eventRepository;
}
```

---

## 2. 패키지 구조와 네이밍

```
src/main/java/.../service/event/EventService.java
src/test/java/.../service/event/EventServiceTest.java
```

메서드 네이밍 패턴:

```java
void saveEvent()                                            // 기본 동작
void saveEvent_WhenNotRegisterFile_UploadFileIsNull()       // 메서드_When조건_Then결과
void editEventWhenNotRegisteredByMe()                       // 메서드When조건 (간결할 때)
void signUp_WhenDuplicateNameOrEmail_ThrowsException()      // 예외 케이스
```

---

## 3. `@DisplayName` 규칙

```java
@DisplayName("모임을 등록 한다.")
@Test
void saveEvent() { ... }

@DisplayName("모임을 등록할때 만든 회원은 모임에 참여 한다.")
@Test
void saveEvent_ThenParticipateMySelf() { ... }

@DisplayName("모임을 수정할때 모임 참여인원이 변경 최대 인원보다 많다면 예외가 발생 한다.")
@Test
void editEvent_WhenParticipantCountExceedsNewCapacity_ThenException() { ... }
```

---

## 4. given / when / then 주석

기본 3블록:

```java
@DisplayName("모임을 등록 한다.")
@Test
void saveEvent() throws IOException {
    //given
    String username = "테스터";
    User user = createUser(username, "testEmail");
    userRepository.save(user);
    EventSaveRequestDto request = createRequestDto("자전거 모임", "서울", 10);

    //when
    eventService.saveEvent(request, username);

    //then
    Event findEvent = eventRepository.findByName("자전거 모임");
    assertThat(findEvent).extracting("name", "capacity")
            .containsExactly("자전거 모임", 10);
}
```

예외 검증 — when/then 합치기:

```java
//when //then
assertThatThrownBy(() -> userService.login(request))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("회원을 찾지 못했습니다.");
```

---

## 5. 테스트 데이터는 private 헬퍼 메서드로

오버로딩으로 변형 표현:

```java
private User createUser(String username, String email) {
    return User.builder()
            .username(username)
            .email(email)
            .build();
}

private User createUser(String username, Mbti mbti) {
    return User.builder()
            .username(username)
            .mbti(mbti)
            .build();
}

private User createUser(String username, String email, String password) {
    return User.builder()
            .username(username)
            .email(email)
            .password(password)
            .build();
}

private static Event createEvent(String author, String name) {
    return Event.builder()
            .name(name)
            .author(author)
            .build();
}

private static Event createEvent(String author, String name, int capacity) {
    return Event.builder()
            .name(name)
            .author(author)
            .capacity(capacity)
            .build();
}

private static Event createEvent(String author, String name, String content, Category category) {
    return Event.builder()
            .name(name)
            .author(author)
            .content(content)
            .category(category)
            .build();
}
```

---

## 6. 상태 정리 (`@BeforeEach cleanUp`)

```java
@BeforeEach
void cleanUp() {
    eventParticipantRepository.deleteAllInBatch();
    bookmarkRepository.deleteAllInBatch();
    eventRepository.deleteAllInBatch();
    userRepository.deleteAllInBatch();
}
```

자식 → 부모 순서로 호출하는 점을 주목.

---

## 7. Assertion 스타일 (AssertJ)

**여러 필드를 한번에:**

```java
assertThat(findEvent).extracting("name", "capacity")
        .containsExactly("자전거 모임", 10);
```

**컬렉션 + Tuple:**

```java
assertThat(comments).hasSize(2)
        .extracting("username", "isRegistrant", "content")
        .containsExactlyInAnyOrder(
                Tuple.tuple("테스터", true, "댓글"),
                Tuple.tuple("테스터2", false, "댓글2")
        );
```

**예외 + 메시지:**

```java
assertThatThrownBy(() -> userService.signUp(newMember))
        .isInstanceOf(BusinessException.class)
        .hasMessage("이미 존재하는 닉네임 혹은 이메일 입니다.");
```

**부재 검증 두 가지 스타일:**

```java
// 스타일 A
assertThatThrownBy(() -> repo.findById(id).orElseThrow())
        .isInstanceOf(NoSuchElementException.class);

// 스타일 B
assertThat(repo.findById(id)).isEmpty();
```

---

## 8. 시간 관련 테스트

```java
@DisplayName("회원 가입 시 검증 만료 시간 전이라면 메일 검증에 통과한다.")
@Test
void verifyMail() {
    //given
    Mail mail = createMail("test@naver.com", "testCode");
    LocalDateTime expirationDateTime = LocalDateTime.of(2025, 4, 14, 12, 0);
    LocalDateTime currentDateTime    = LocalDateTime.of(2025, 4, 14, 11, 59);
    mailVerificationStorage.put(mail, expirationDateTime);

    //when
    VerificationResultInfo result = mailService.verifyMail(mail, currentDateTime);

    //then
    assertThat(result.isVerificationPassed()).isTrue();
}
```

서비스 메서드 시그니처에 `LocalDateTime currentDateTime`을 받도록 설계한다.

---

## 9. 파라미터화 테스트

```java
@DisplayName("신규 회원 가입을 할 때 이미 등록된 닉네임 혹은 이메일이 있을 경우 예외가 발생 한다.")
@ParameterizedTest
@CsvSource(value = {
    "registeredEmail@naver.com-신규 닉네임",
    "nonRegisteredEmail@naver.com-중복 닉네임",
    "registeredEmail@naver.com-중복 닉네임"
}, delimiter = '-')
void signUp_WhenDuplicateNameOrEmail_ThrowsException(String email, String username) {
    //given
    UserSaveRequest request = createSaveDto("registeredEmail@naver.com", "중복 닉네임");
    userService.signUp(request);

    UserSaveRequest newMember = createSaveDto(email, username);

    //when //then
    assertThatThrownBy(() -> userService.signUp(newMember))
            .isInstanceOf(BusinessException.class)
            .hasMessage("이미 존재하는 닉네임 혹은 이메일 입니다.");
}
```

---

## 10. JPA flush/clear 명시

```java
@Autowired
private EntityManager em;

//when
Long modifiedPostId = postService.modify(request, 1L, post.getId());
em.flush(); em.clear();

//then
Post findPost = postRepository.findById(modifiedPostId).orElseThrow();
assertThat(findPost.getSubject()).isEqualTo("수정된 제목");
```

---

## 11. 동시성 테스트

```java
@Disabled
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("최대 인원이 100명인 모임에 10000명의 회원이 동시에 참가 하면 100명이 참가한다.")
@Test
void saveEventParticipation_WhenFiveUsersJoin_ThenThreeParticipantsAllowed() throws Exception {
    //given
    int taskCount = 10000;
    int capacity = 100;
    ExecutorService executorService = Executors.newFixedThreadPool(capacity);
    CountDownLatch countDownLatch = new CountDownLatch(taskCount);

    Event event = eventRepository.save(createEvent("테스터", "테스트 모임", capacity));

    List<User> users = Stream
            .generate(() -> {
                User user = createUser("테스터", "testEmail");
                userRepository.save(user);
                return user;
            })
            .limit(taskCount)
            .toList();

    //when
    AtomicInteger exceptionCount = new AtomicInteger(0);

    for (User user : users) {
        executorService.submit(() -> {
            try {
                eventParticipationService.saveEventParticipation(event.getId(), user.getId());
            } catch (BusinessException ex) {
                exceptionCount.incrementAndGet();
            } finally {
                countDownLatch.countDown();
            }
        });
    }
    countDownLatch.await();
    executorService.shutdown();

    //then
    em.clear();
    Event findEvent = eventRepository.findById(event.getId()).orElseThrow();
    assertThat(findEvent.getParticipantCount()).isEqualTo(100);
    assertThat(exceptionCount.get()).isEqualTo(9900);
}
```

---

## 12. `@Disabled` 사용

```java
// 영속성 전이 문제
@Disabled
@DisplayName("댓글을 삭제 한다.")
@Test
void delete() {
    ...
}
```

이유를 한 줄짜리 한글 주석으로 남긴다.

---

## 13. import 스타일

```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
```

assertion 함수 + enum 상수를 적극 static import.

---

## 14. 한 테스트 = 하나의 시나리오

같은 메서드 `getEventWithRegularInfo`라도 시나리오별로 쪼개 놓은 예:

```java
void getEventWithRegularInfo_WhenNoParticipationNoBookmark_ThenStatusCheck()
void getEventWithRegularInfo_WhenParticipatedEvent_ThenIsParticipatedIsTrue()
void getEventWithRegularInfo_WhenBookmarkedEvent_ThenBookmarkStatusIsBookmark()
void getEventWithRegularInfo_WhenIsEventRegistrant_ThenIsEventRegistrantIsTrue()
void getEventWithRegularInfo_WhenTwoParticipants_ThenCountIsTwo()
void getEventWithRegularInfo_WhenNotParticipatedRegularEvent_ThenReturnNotParticipated()
void getEventWithRegularInfo_WhenParticipatedRegularEvent_ThenReturnParticipated()
```

---

## 15. 매직넘버는 변수로

```java
String searchKeyword = "검색";
Category searchCategory = Category.PET;
int bookmarkCount = 3;
int userCount = 2;

Event event = createEvent("테스터", "모임 " + searchKeyword, "내용", searchCategory);
```

리터럴이 두 군데 이상 등장하거나, 검증값에 의미가 있을 때 항상 지역변수로 뺀다.
