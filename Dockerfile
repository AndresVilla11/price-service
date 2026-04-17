# ── Stage 1: build ────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY gradle/wrapper/gradle-wrapper.jar    gradle/wrapper/gradle-wrapper.jar
COPY gradle/wrapper/gradle-wrapper.properties gradle/wrapper/gradle-wrapper.properties
COPY gradlew ./
COPY build.gradle.kts settings.gradle.kts ./

RUN chmod +x gradlew

RUN ./gradlew dependencies --no-daemon -q

COPY src ./src
RUN ./gradlew bootJar --no-daemon -q

RUN java -Djarmode=layertools \
    -jar build/libs/price-service.jar \
    extract --destination build/extracted

# ── Stage 2: runtime ──────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

COPY --from=builder /app/build/extracted/dependencies          ./
COPY --from=builder /app/build/extracted/spring-boot-loader    ./
COPY --from=builder /app/build/extracted/snapshot-dependencies ./
COPY --from=builder /app/build/extracted/application           ./

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "org.springframework.boot.loader.launch.JarLauncher"]