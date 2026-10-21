# ---- build ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# 의존성 레이어 캐시: 빌드 스크립트만 먼저 복사
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null

COPY src src
# plain jar 는 비활성화되어 있어 build/libs 에는 실행 가능한 jar 하나만 생김
RUN ./gradlew --no-daemon bootJar -x test \
    && cp build/libs/*.jar app.jar

# ---- run ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
