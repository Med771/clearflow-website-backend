# =========================
# Stage 1: Build
# =========================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q dependency:go-offline

COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B clean package -DskipTests


# =========================
# Stage 2: Runtime
# =========================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN useradd -r -u 1001 appuser

RUN mkdir -p /app/storage && chown -R appuser:appuser /app/storage

COPY --from=builder /app/target/*.jar app.jar

USER appuser

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]