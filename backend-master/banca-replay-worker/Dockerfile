# =============================================================================
# Standalone Dockerfile for the BanCa failed-settle replay worker.
#
# The primary backend Dockerfile (../Dockerfile) already builds this module
# as part of the shared multi-stage image and ships its JAR in /app/libs/app.
# That is the deployment path used by docker-compose.banca-replay.yml.
#
# This Dockerfile is provided as a fallback for ops teams that want to ship
# the worker as a stand-alone image (smaller surface, independent rollout).
# =============================================================================

FROM eclipse-temurin:8-jdk AS builder
WORKDIR /build
COPY . /build/
RUN chmod +x gradlew && \
    ./gradlew :banca-replay-worker:jar :VbeeCommon:jar copyRuntimeDeps -x test --no-daemon

FROM eclipse-temurin:8-jre
WORKDIR /app
RUN mkdir -p /app/libs/app /app/libs/runtime /app/logs

COPY --from=builder /build/banca-replay-worker/build/libs/banca-replay-worker-*.jar /app/libs/app/
COPY --from=builder /build/VbeeCommon/build/libs/VbeeCommon-*.jar /app/libs/runtime/
COPY --from=builder /build/runtime-libs/*.jar /app/libs/runtime/

COPY banca-replay-worker/config/logback.xml /app/config/logback.xml

ENV BANCA_REPLAY_ENABLED=false
ENV BANCA_REPLAY_INTERVAL_SEC=30

ENTRYPOINT ["java", "-Xmx128m", "-Xms64m", \
            "-Dservice.name=banca-replay-worker", \
            "-Dlogback.configurationFile=/app/config/logback.xml", \
            "-cp", "/app/libs/app/*:/app/libs/runtime/*", \
            "com.vinplay.bancareplay.BancaReplayWorker"]
