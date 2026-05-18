# 코딩 컨벤션 — Reference (예시 모음)

이 문서는 `.claude/skills/code_convention/SKILL.md`에 정의된 규칙의 구체적인 코드 예시를 담고 있다.
규칙 정의는 반드시 `.claude/skills/code_convention/SKILL.md`를 먼저 확인할 것.

---

## Optional 예시

### `.get()` 호출 규칙

````java
// ❌ 지양
String name = optional.get();

if (optional.isPresent()) {
    String name = optional.get();
}

// ✅ 권장
String name = optional.orElse("default");
String name = optional.orElseThrow(() -> new IllegalStateException("not found"));
optional.ifPresent(value -> process(value));
````

### 반환 타입으로 사용

````java
// ✅ Repository 단건 조회
public Optional<User> findByEmail(String email) {
    return userRepository.findByEmail(email);
}
````

### 잘못된 사용

````java
// ❌ 필드 타입으로 사용 금지
public class User {
    private Optional<String> nickname; // 금지
}

// ❌ 메서드 파라미터로 사용 금지
public void update(Optional<String> name) { ... } // 금지

// ❌ 컬렉션을 Optional로 감싸기 금지
public Optional<List<User>> findAll() { ... } // 금지

// ✅ 대신 빈 컬렉션 반환
public List<User> findAll() {
    return Collections.emptyList();
}
````

---

## Stream 예시

### 권장 케이스

````java
// ✅ 변환 + 필터 + 집계가 연속될 때
List<String> activeNames = users.stream()
    .filter(User::isActive)
    .map(User::getName)
    .collect(toList());
````

### 지양 케이스

````java
// ❌ 단순 순회만 하는 경우
list.stream().forEach(item -> log.info(item.toString()));

// ✅ 일반 for 루프 사용
for (Item item : list) {
    log.info(item.toString());
}
````

````java
// ❌ 외부 상태 변경
List<String> result = new ArrayList<>();
items.stream().forEach(item -> result.add(item.getName()));

// ✅ collect 사용
List<String> result = items.stream()
    .map(Item::getName)
    .collect(toList());
````

---

## String null / 빈 문자열 체크 예시

````java
// ❌ 지양
if (name != null) { ... }
if (name != null && !name.isEmpty()) { ... }
if (name != null && !name.trim().isEmpty()) { ... }

// ✅ 권장
if (StringUtils.hasText(name)) { ... }
if (StringUtils.hasLength(name)) { ... }
````

### 의미별 구분

````java
// "실질적인 값이 있는가" — 공백만 있는 경우도 false
if (StringUtils.hasText(input)) { ... }

// "길이가 1 이상인가" — 공백도 유효한 값으로 인정
if (StringUtils.hasLength(input)) { ... }
````

---

## 람다 표현식 예시

### 메서드 레퍼런스(`::`) 우선 사용

````java
// ❌ 지양
list.stream().map(item -> item.getName()).collect(toList());
list.forEach(item -> System.out.println(item));

// ✅ 권장
list.stream().map(Item::getName).collect(toList());
list.forEach(System.out::println);
````

### 파라미터 작성 규칙

````java
// ❌ 지양
(Item item) -> item.getName()
(item) -> item.getName()

// ✅ 권장
item -> item.getName()
````

### 본문 작성 규칙

````java
// ✅ 한 줄 — 중괄호와 return 생략
item -> item.getPrice() * 2

// ✅ 여러 줄 — 중괄호 사용
item -> {
    int price = item.getPrice();
    return price * 2;
}
````

### 복잡한 람다는 메서드로 추출

````java
// ❌ 지양 — 람다 본문이 길고 복잡
list.stream()
    .filter(item -> {
        if (item.getStatus() == Status.ACTIVE) {
            return item.getPrice() > 1000 && item.getCategory().equals("A");
        }
        return false;
    })
    .collect(toList());

// ✅ 권장 — 메서드로 추출
list.stream()
    .filter(this::isHighPriceActiveItemInCategoryA)
    .collect(toList());

private boolean isHighPriceActiveItemInCategoryA(Item item) {
    return item.getStatus() == Status.ACTIVE
        && item.getPrice() > 1000
        && item.getCategory().equals("A");
}
````

---

## 줄바꿈 / 띄어쓰기 예시

### Stream

````java
// ❌ 지양
List<String> result = list.stream().filter(item -> item.isActive()).map(Item::getName).collect(toList());

// ✅ 권장
List<String> result = list.stream()
    .filter(Item::isActive)
    .map(Item::getName)
    .collect(toList());
````

### Builder

````java
// ❌ 지양
User user = User.builder().name("kim").age(20).email("a@b.com").build();

// ✅ 권장
User user = User.builder()
    .name("kim")
    .age(20)
    .email("a@b.com")
    .build();
````

### if / else / for / while 문

````java
// ❌ 지양
if(condition) doSomething();
if (condition){
    doSomething();
}
else {
    doOther();
}

// ✅ 권장
if (condition) {
    doSomething();
} else {
    doOther();
}
````

### 메서드 체이닝 (공통 규칙)

````java
// ✅ 짧으면 한 줄
String result = str.trim().toLowerCase();

// ✅ 길면 줄바꿈
String result = str.trim()
    .toLowerCase()
    .replace(" ", "_")
    .substring(0, 10);
````

### 연산자 / 콤마

````java
// ❌ 지양
int sum = a+b;
if (a==1&&b==2) { ... }
method(a,b,c);

// ✅ 권장
int sum = a + b;
if (a == 1 && b == 2) { ... }
method(a, b, c);
````
