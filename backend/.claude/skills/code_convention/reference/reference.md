# Backend Code Convention — Reference

`SKILL.md`의 각 규칙에 대응하는 코드 예시. 섹션 번호는 1:1 매칭.

---

## 1. Java 버전

````java
// ✅ Java 16+ .toList()
return events.stream()
        .map(Event::getId)
        .toList();
````

````java
// ❌ record / sealed / switch 표현식 / text block 사용 금지
public record UserDto(String name, int age) {}                 // ❌
String label = switch (status) { case A -> "a"; default -> "b"; }; // ❌
String sql = """ SELECT * FROM users """;                       // ❌

// ✅ DTO는 @Getter + @Builder 클래스로 (2번 섹션 참고)
````

---

## 2. Lombok

````java
// ✅ Service — @Transactional → @RequiredArgsConstructor → @Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
}

// ✅ Controller — @RestController → @RequiredArgsConstructor → @RequestMapping
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserApiController { ... }
````

````java
// ✅ DTO — 기본 생성자 PROTECTED, @Builder 생성자 private
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSaveRequest {

    @NotBlank private String username;
    @Email    private String email;
    @NotBlank private String password;

    @Builder
    private UserSaveRequest(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }
}
````

````java
// ❌ @Setter / @Data — 무분별한 노출
@Setter @Data public class User { ... }

// ✅ 의미를 담은 메서드
public void modify(String username, String introduction, ...) { ... }
````

---

## 3. Optional

````java
// ✅ 표준 — orElseThrow
User user = userRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new NotFoundException("user not found"));

// ✅ 체이닝 — map + orElse
Status status = repository.findByKey(key)
        .map(Item::getStatus)
        .orElse(Status.DEFAULT);

// ✅ 존재 시 예외 — ifPresent
userRepository.findByUsername(request.getUsername())
        .ifPresent(u -> {
            throw new BusinessException("username already exists");
        });

// ✅ Repository 반환 타입
Optional<User> findByEmail(String email);   // 단건
List<User> findByIdIn(List<Long> ids);      // 다건은 List
````

````java
// ❌ 금지 케이스
private Optional<String> nickname;                  // 필드
public void update(Optional<String> name) { ... }   // 파라미터
public Optional<List<User>> findAll() { ... }       // 컬렉션 감싸기
String name = optional.get();                       // isPresent 없는 .get()
````

---

## 4. Stream

````java
// ✅ 종료는 .toList()
return events.stream()
        .map(Event::getId)
        .toList();

// ✅ 그룹화 / 맵 변환
Map<Long, User> userMap = users.stream()
        .collect(Collectors.toMap(User::getId, Function.identity()));

Map<Category, List<Event>> grouped = events.stream()
        .collect(Collectors.groupingBy(Event::getCategory));

// ✅ 사용 후 한 줄 띄움
List<Long> ids = events.stream()
        .map(Event::getId)
        .toList();

Map<Long, Long> counts = repository.findCountsByIds(ids);
````

````java
// ❌ Collectors.toList() — 사용하지 않음
list.stream().collect(Collectors.toList());

// ❌ 단순 순회만 / 외부 상태 변경
list.stream().forEach(item -> log.info(item.toString()));
items.stream().forEach(item -> result.add(item.getName()));

// ✅ 단순 순회는 for, 변환은 map+toList
for (Item item : list) { log.info(item.toString()); }

List<String> names = items.stream()
        .map(Item::getName)
        .toList();
````

> 메서드 레퍼런스 변환 패턴은 7번 람다 섹션 참고.

---

## 5. final 키워드

````java
// ✅ 필드에만
@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final MailService mailService;
}

// ❌ 파라미터 / 지역변수 / 메서드 / 클래스
public void editUser(final UserModifyRequest request) { ... }   // ❌
public final void process() { ... }                              // ❌
public final class UserService { ... }                           // ❌
````

---

## 6. String 체크

````java
// ✅ DTO 입력 검증은 어노테이션 (표준 또는 커스텀)
@NotBlank        private String username;
@NotBlank @Email private String email;
@NotBlank        private String password;

// ✅ 컨트롤러에서 @Valid로 활성화
@PostMapping("/signup")
public ResponseEntity<Void> signUp(@RequestBody @Valid UserSaveRequest request) { ... }

// ✅ 동적 체크가 정말 필요한 곳에서만 명시적 if
if (str != null && !str.isBlank()) {
    process(str);
}
````

````java
// ❌ Apache Commons StringUtils — 의존성 추가 안 함
import org.apache.commons.lang3.StringUtils;
````

---

## 7. 람다 표현식

````java
// ✅ 메서드 레퍼런스 우선
list.stream().map(Item::getName).toList();
list.forEach(System.out::println);

// ✅ 단일 파라미터 괄호 생략, 타입 생략, 한 줄이면 중괄호/return 생략
item -> item.getPrice() * 2

// ✅ 두 줄 이상이면 중괄호
.ifPresent(u -> {
    throw new BusinessException("username already exists");
});
````

````java
// ❌ 3줄 이상 람다 본문 — 메서드로 추출
list.stream()
        .filter(item -> {
            if (item.getStatus() == Status.ACTIVE) {
                return item.getPrice() > 1000 && item.getCategory().equals("A");
            }
            return false;
        })
        .toList();

// ✅ 메서드 추출 + 메서드 레퍼런스
list.stream()
        .filter(this::isHighPriceActiveItemInCategoryA)
        .toList();
````

---

## 8. 줄바꿈 / 띄어쓰기

````java
// ✅ Stream — 메서드별 줄바꿈, . 앞에
List<String> names = list.stream()
        .filter(Item::isActive)
        .map(Item::getName)
        .toList();

// ✅ Builder — 각 setter 줄바꿈, .build() 별도 줄
return User.builder()
        .username(username)
        .password(encoder.encode(password))
        .email(email)
        .role(role)
        .build();

// ✅ 메서드 체이닝 — 2개 이하는 한 줄, 3개 이상은 줄바꿈
String result = str.trim().toLowerCase();
String result = str.trim()
        .toLowerCase()
        .replace(" ", "_")
        .substring(0, 10);

// ✅ if / for / while — 키워드 뒤 공백, 한 줄도 중괄호
if (condition) {
    doSomething();
}

// ✅ 연산자 앞뒤 공백, 콤마 뒤만 공백
int sum = a + b;
if (a == 1 && b == 2) { ... }
method(a, b, c);
````

---

## 9. 조건문 규칙

````java
// ❌ else if / else
if (user.isAdmin())   return "ADMIN";
else if (user.isPremium()) return "PREMIUM";
else                  return "BASIC";

// ✅ early return
if (user.isAdmin())   return "ADMIN";
if (user.isPremium()) return "PREMIUM";
return "BASIC";
````

````java
// ✅ 표준 흐름 — 검증 → orElseThrow → 정상 흐름
@Transactional
public void editUser(UserModifyRequest request, Long userId) {
    checkDuplicatedUsername(request);

    User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("user not found"));

    user.modify(request.getUsername(), request.getIntroduction(), ...);
}

// ✅ 위배 조건 throw → 정상 흐름 계속
if (userRepository.existsByEmailOrUsername(email, username)) {
    throw new BusinessException("email or username already exists");
}

User user = request.toEntity(encoder);
userRepository.save(user);
````

> 클래스 어노테이션 순서는 2번 Lombok 섹션의 Service/Controller 예시 참고.
