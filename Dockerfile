# Build with the project JDK, then run with a JRE-only image.
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home --home-dir /app appuser \
    && mkdir -p /app/data/media \
    && chown -R appuser:appuser /app
WORKDIR /app

COPY --from=build --chown=appuser:appuser /workspace/target/noghte-rangi-*.jar /app/app.jar

USER appuser
ENV MEDIA_STORAGE_PATH=/app/data/media
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=6 \
    CMD curl --fail --silent http://127.0.0.1:8080/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
