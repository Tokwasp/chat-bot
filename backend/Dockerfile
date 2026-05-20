# syntax=docker/dockerfile:1

# 1단계: Gradle로 빌드 (JDK 필요)
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# 의존성 캐싱: 빌드 스크립트/래퍼 먼저 복사
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# 소스 복사 후 실행 가능한 부트 JAR 빌드 (테스트는 실제 Bedrock 호출이라 빌드 단계에서 제외)
COPY src ./src
RUN ./gradlew clean bootJar --no-daemon -x test \
    && cp build/libs/*-SNAPSHOT.jar app.jar

# 2단계: 실행 (JRE만 있으면 충분, 이미지 경량화)
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# root가 아닌 사용자로 실행
RUN useradd -r -u 1001 appuser
USER appuser

COPY --from=build /workspace/app.jar app.jar

EXPOSE 3001

ENTRYPOINT ["java", "-jar", "app.jar"]
