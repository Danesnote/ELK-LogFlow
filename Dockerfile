# ---- build ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# 의존성 레이어 캐시: 빌드 스크립트만 먼저 복사
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null

COPY src src
# bootWar 는 실행 가능한 war 를, war 는 *-plain.war 를 만듦 → 실행 가능한 쪽만 사용
RUN ./gradlew --no-daemon bootWar -x test \
    && cp "$(ls build/libs/*.war | grep -v plain)" app.war

# ---- run ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/app.war app.war
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.war"]
