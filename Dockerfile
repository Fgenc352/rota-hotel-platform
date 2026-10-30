FROM maven:3.10.0-eclipse-temurin-25-noble AS build
WORKDIR /workspace
COPY pom.xml lombok.config ./
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre-noble AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app app \
    && mkdir -p /app/logs \
    && chown -R app:app /app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/target/hotel-booking-crm-1.0-SNAPSHOT.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=4s --start-period=60s --retries=12 \
    CMD curl --fail --silent --max-time 3 http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
