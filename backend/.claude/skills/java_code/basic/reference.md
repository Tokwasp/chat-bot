# Readable Java — Reference 예시

SKILL.md의 각 규칙에 대응하는 Before(나쁜 코드) → After(좋은 코드) 예시와 그 이유를 정리한다.
SKILL.md의 섹션 번호와 동일한 번호 체계를 사용하므로, 규칙을 적용할 때 같은 번호 섹션을 열어 비교하면 된다.

객체지향 설계(Tell Don't Ask, VO, 일급 컬렉션, 다형성 등)에 대한 예시는 `oop-java` 스킬의 reference.md를 참고한다.

---

## 1.1 변수명 — 의미를 드러낸다

### Before
```java
for (int i = 0; i < 8; i++) {
    for (int j = 0; j < 10; j++) {
        board[i][j] = "□";
    }
}

String input = scanner.nextLine();
String input2 = scanner.nextLine();
char c = input.charAt(0);
char r = input.charAt(1);
```

### After
```java
for (int row = 0; row < BOARD_ROW_SIZE; row++) {
    for (int col = 0; col < BOARD_COL_SIZE; col++) {
        board[row][col] = CLOSED_CELL_SIGN;
    }
}

String cellInput = scanner.nextLine();
String userActionInput = scanner.nextLine();
char cellInputCol = cellInput.charAt(0);
char cellInputRow = cellInput.charAt(1);
```

### 왜
`i`, `j`는 단순 카운터처럼 보이지만 여기서는 행/열이라는 분명한 의미가 있다.
이름이 의미를 드러내면 인덱스 계산 실수도 줄어들고, 코드 어느 줄을 떼어내 봐도 맥락 없이 읽힌다.
`input`, `input2`는 "두 번째 입력이 뭔지" 읽는 사람이 위로 거슬러 올라가야 한다. 이름 자체로 답을 줘야 한다.

---

## 1.2 메서드명 — 부정형 메서드 피하기

### Before
```java
public boolean isNotLockerType() { ... }

// 호출부
if (!isNotLockerType()) {
    // 사물함 사용 가능
}
```

### After
```java
public boolean isLockerType() { ... }
public boolean isNotLockerType() {
    return !isLockerType();
}

// 호출부에서는 의미에 맞는 쪽을 골라 쓴다
if (passType.isLockerType()) { ... }
if (selectedPass.cannotUseLocker()) { ... }
```

### 왜
`!isNotLockerType()`은 이중 부정이라 의미 파악에 한 박자 늦는다.
긍정형을 기본으로 두고, 부정형이 자주 필요하면 부정형 메서드도 함께 제공한다.
단, `cannotUseLocker()`처럼 도메인 개념 자체가 부정형으로 자연스럽게 표현되는 경우는 그대로 둔다 (이중부정과는 다름).

---

## 1.3 매직 넘버 / 매직 스트링

### Before
```java
private static String[][] board = new String[8][10];
private static int gameStatus = 0; // 0: 게임 중, 1: 승리, -1: 패배

board[row][col] = "□";
if (gameStatus == -1) { ... }
```

### After
```java
public static final int BOARD_ROW_SIZE = 8;
public static final int BOARD_COL_SIZE = 10;
public static final String CLOSED_CELL_SIGN = "□";
public static final String FLAG_SIGN = "⚑";

private static GameStatus gameStatus = GameStatus.IN_PROGRESS;

board[row][col] = CLOSED_CELL_SIGN;
if (gameStatus == GameStatus.LOSE) { ... }
```

### 왜
`8`, `10`은 무엇의 8과 10인지 코드만 봐서 알 수 없다.
`-1`, `0`, `1`처럼 "의미는 있지만 코드엔 안 적혀 있는" 값들은 주석으로 보완하는 게 아니라 enum이나 상수로 의미를 드러낸다.
주석으로 의미를 적어두면 코드가 바뀔 때 주석이 같이 안 바뀌어서 거짓말을 하기 시작한다.

---

## 2.1 추상화 레벨 맞추기

### Before
```java
public static void main(String[] args) {
    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    System.out.println("지뢰찾기 게임 시작!");
    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    Scanner scanner = new Scanner(System.in);
    for (int i = 0; i < 8; i++) {
        for (int j = 0; j < 10; j++) {
            board[i][j] = "□";
        }
    }
    for (int i = 0; i < 10; i++) {
        int col = new Random().nextInt(10);
        int row = new Random().nextInt(8);
        landMines[row][col] = true;
    }
    // ... 100줄 이상의 디테일
}
```

### After
```java
public void run() {
    outputHandler.showGameStartComments();

    while (gameBoard.isInProgress()) {
        try {
            outputHandler.showBoard(gameBoard);
            CellPosition cellPosition = getCellInputFromUser();
            UserAction userAction = getUserActionInputFromUser();
            actOnCell(cellPosition, userAction);
        } catch (GameException e) {
            outputHandler.showExceptionMessage(e);
        }
    }

    outputHandler.showGameResult(gameBoard);
}
```

### 왜
Before의 `main`은 "게임 시작 안내문 출력" → "보드 초기화 디테일" → "지뢰 배치 디테일"이 한 메서드에 다 있다.
높은 추상화 레벨(전체 흐름)과 낮은 추상화 레벨(2차원 배열 초기화)가 섞여있어 한 번에 이해하기 어렵다.

After는 한 메서드 안의 모든 줄이 같은 추상화 레벨이다. "안내 출력 → 진행 중 동안 입출력 → 결과 출력". 디테일은 각 메서드 안으로 내려갔다.

판단법: 메서드를 한 줄씩 읽었을 때 모든 줄이 "같은 높이에서의 설명"으로 느껴져야 한다.

---

## 2.2 Early Return

### Before
```java
private static void actOnCell(String cellInput, String userActionInput) {
    int selectedColIndex = getSelectedColIndex(cellInput);
    int selectedRowIndex = getSelectedRowIndex(cellInput);
    if (doesUserChooseToPlantFlag(userActionInput)) {
        BOARD[selectedRowIndex][selectedColIndex] = FLAG_SIGN;
        checkIfGameIsOver();
    } else if (doesUserChooseToOpenCell(userActionInput)) {
        if (isLandMineCell(selectedRowIndex, selectedColIndex)) {
            BOARD[selectedRowIndex][selectedColIndex] = LAND_MINE_SIGN;
            changeGameStatusToLose();
        } else {
            open(selectedRowIndex, selectedColIndex);
            checkIfGameIsOver();
        }
    } else {
        System.out.println("잘못된 번호를 선택하셨습니다.");
    }
}
```

### After
```java
private void actOnCell(CellPosition cellPosition, UserAction userAction) {
    if (doesUserChooseToPlantFlag(userAction)) {
        gameBoard.flagAt(cellPosition);
        return;
    }

    if (doesUserChooseToOpenCell(userAction)) {
        gameBoard.openAt(cellPosition);
        return;
    }

    throw new GameException("잘못된 번호를 선택하셨습니다.");
}
```

### 왜
if-else 체인은 모든 분기를 머릿속에 동시에 들고 읽어야 한다.
조건을 만족하면 즉시 return하면, 한 분기를 끝낸 뒤 다음 분기로 시야가 깨끗하게 넘어간다.
들여쓰기가 줄어들면서 평면적인 흐름이 되어 읽기 쉽다.

---

## 2.3 사고의 depth 줄이기

### Before
```java
private static boolean isAllCellOpened() {
    boolean isAllOpened = true;
    for (int row = 0; row < BOARD_ROW_SIZE; row++) {
        for (int col = 0; col < BOARD_COL_SIZE; col++) {
            if (BOARD[row][col].equals(CLOSED_CELL_SIGN)) {
                isAllOpened = false;
            }
        }
    }
    return isAllOpened;
}
```

### After
```java
private static boolean isAllCellOpened() {
    return Arrays.stream(BOARD)
        .flatMap(Arrays::stream)
        .noneMatch(cell -> cell.equals(CLOSED_CELL_SIGN));
}
```

### 왜
중첩 for문 + if + 플래그 변수는 의도가 "모든 셀이 열려있는지"인데, 코드는 "두 번 돌면서 닫힌 셀이 있으면 플래그를 false로"라는 절차로 적혀있다.
stream의 `noneMatch`는 의도 그대로다: "닫힌 셀이 하나도 없는가". 코드가 의도를 직접 말한다.

주의: stream이 항상 정답은 아니다. 복잡한 조건이 겹치면 for문이 더 명료할 수 있다. 판단 기준은 **의도가 더 잘 드러나는 쪽**.

---

## 2.4 메서드 파라미터 / boolean 파라미터

### Before
```java
outputHandler.showPassOrderSummary(selectedPass, null);  // 사물함 없을 때
outputHandler.showPassOrderSummary(selectedPass, lockerPass);  // 있을 때

// 또는
processOrder(pass, true);   // discount 적용
processOrder(pass, false);  // 미적용
```

### After
```java
// 1. null 대신 도메인 객체로 묶기
StudyCafePassOrder passOrder = StudyCafePassOrder.of(
    selectedPass,
    optionalLockerPass.orElse(null)
);
ioHandler.showPassOrderSummary(passOrder);

// 2. boolean 대신 메서드 분리 또는 enum
processOrderWithDiscount(pass);
processOrderWithoutDiscount(pass);
```

### 왜
파라미터 자리에 `null`을 넘기는 건 "여기는 없을 수도 있어"를 호출자가 매번 알아야 한다는 뜻이다.
관련 값을 묶어 `Order` 같은 객체로 만들면, 의미 있는 도메인 개념이 코드에 나타나고 null 전달이 사라진다.

boolean 파라미터는 호출부에서 `true`/`false`만 보면 무슨 뜻인지 알 수 없다. `processOrder(pass, true)`는 가독성이 0이다.

---

## 2.6 클래스 내 멤버 선언 순서

### After
```java
public class StudyCafeSeatPass {

    // 1. 인스턴스 필드
    private final StudyCafePassType passType;
    private final int duration;
    private final int price;
    private final double discountRate;

    // 2. 생성자 (private)
    private StudyCafeSeatPass(StudyCafePassType passType, int duration, int price, double discountRate) {
        this.passType = passType;
        this.duration = duration;
        this.price = price;
        this.discountRate = discountRate;
    }

    // 3. 정적 팩토리 메서드
    public static StudyCafeSeatPass of(StudyCafePassType passType, int duration, int price, double discountRate) {
        return new StudyCafeSeatPass(passType, duration, price, discountRate);
    }

    // 4. public 메서드 (행위)
    public boolean cannotUseLocker() {
        return this.passType.isNotLockerType();
    }

    public int getDiscountPrice() {
        return (int) (this.price * this.discountRate);
    }

    // 5. private 메서드 (내부 헬퍼) — 있다면 여기

    // 6. getter (마지막)
    public StudyCafePassType getPassType() {
        return passType;
    }

    public int getDuration() {
        return duration;
    }

    public int getPrice() {
        return price;
    }
}
```

### 왜
위에서 아래로 읽었을 때 "이 클래스가 무엇이고 → 어떻게 만들어지고 → 무엇을 할 수 있고 → 어떤 정보를 노출하는가" 순으로 자연스럽게 흐른다.
getter를 아래에 두면 비즈니스 행위가 먼저 눈에 띄어서 객체의 "역할"이 강조된다.

상수(static final)가 있다면 인스턴스 필드보다 위에 둔다.

---

## 3.1 해피 케이스 / 예외 처리

### Before
```java
// 예외를 그냥 println으로 처리
} else {
    System.out.println("잘못된 번호를 선택하셨습니다.");
}

// 모든 예외를 한 곳에서 삼키기
} catch (Exception e) {
    // do nothing
}
```

### After
```java
// 도메인 예외를 정의해서 의미 있게 던지기
public class GameException extends RuntimeException {
    public GameException(String message) {
        super(message);
    }
}

// 사용
private CellPosition getCellInputFromUser() {
    outputHandler.showCommentForSelectingCell();
    CellPosition cellPosition = inputHandler.getCellPositionFromUser();
    if (gameBoard.isInvalidCellPosition(cellPosition)) {
        throw new GameException("잘못된 좌표를 선택하셨습니다.");
    }
    return cellPosition;
}

// 호출부에서 의미별로 처리
try {
    ...
} catch (GameException e) {
    outputHandler.showExceptionMessage(e);
} catch (Exception e) {
    outputHandler.showSimpleMessage("프로그램에 문제가 생겼습니다.");
}
```

### 왜
예외 메시지가 그대로 `println`으로 흘러나가면 호출자가 예외를 다룰 방법이 없다.
의미 있는 예외 클래스를 만들면 "이건 비즈니스 규칙 위반"과 "이건 시스템 오류"를 구분해 처리할 수 있다.

`catch (Exception e)`로 다 삼키면 디버깅이 지옥이다. 의미 있는 예외부터 잡고, 마지막에 unknown fallback을 둔다.

---

## 종합 체크리스트 — 코드 작성 후 마지막 점검

다음 항목 중 하나라도 걸리면 SKILL.md의 해당 섹션을 다시 본다.

| 신호 | 해당 규칙 |
|---|---|
| `i`, `j`, `temp`, `data` 같은 이름이 보임 | 1.1 |
| `!isNotXxx()` 이중 부정이 보임 | 1.2, 3.3 |
| 코드에 박힌 숫자/문자열 리터럴이 있음 | 1.3 |
| 클래스명에 `Manager`, `Helper`, `Util` 접미사 | 1.4 |
| 한 메서드 안에서 추상화 수준이 들쭉날쭉 | 2.1 |
| if-else 체인의 들여쓰기가 깊음 | 2.2 |
| if 안에 if 안에 for가 있음 | 2.3 |
| 파라미터 자리에 `null` 전달 / boolean 파라미터로 분기 | 2.4 |
| 한 메서드가 한 화면을 넘어감 | 2.5 |
| 클래스 내 멤버 순서가 뒤죽박죽 (getter가 위에 등) | 2.6 |
| `catch (Exception e)` 하나로 다 잡음 | 3.1 |
| 사고 단락이 바뀌는데 공백 라인이 없음 | 3.2 |

> 도메인 객체 설계 관련 신호(Tell Don't Ask 위반, 일급 컬렉션 누락, 타입 분기 if-else 등)는 `oop-java` 스킬의 체크리스트를 본다.
