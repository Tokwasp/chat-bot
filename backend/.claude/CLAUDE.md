# Backend 코딩 컨벤션

## Java 버전

Java 11 이하 문법만 사용한다.
- `record` 사용 금지 (Java 14+)
- `sealed class` 사용 금지 (Java 17+)
- `switch` 표현식 사용 금지 (Java 14+)
- `text block` 사용 금지 (Java 15+)

## Lombok

허용하는 어노테이션:
- `@Getter`
- `@Setter`
- `@Data`
- `@RequiredArgsConstructor`
- `@AllArgsConstructor`

사용 금지:
- `@Value`

`@Setter`는 클래스 전체 또는 다수 필드에 쓸 때만 사용한다.
단일 필드 하나만 변경이 필요한 경우 `@Setter` 대신 해당 로직을 담은 메서드를 작성한다.
