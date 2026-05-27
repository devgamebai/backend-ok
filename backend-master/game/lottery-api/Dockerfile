# =============================================================================
# lottery-api — Spring Boot 2.7.18 standalone
# =============================================================================
# Hosts XSMB Lô Đề REST + STOMP on port 9292 (mgmt actuator on 9293).
#
# Build pipeline mirrors backend-master/game/minigame-api/Dockerfile:
#   1. Stage A (parent-build): Gradle 5.2.1 → shared-lib + :game:lottery-engine
#      jars.
#   2. Stage B (spring-build): Gradle 7.6.4 → lottery-api bootJar (Spring Boot
#      2.7.18 requires Gradle 6.8+, root pins 5.2.1).
#   3. Stage C (runtime): JRE 8 + fat jar.
#
# Build context: backend-master/ (parent dir). Required because file deps in
# game/lottery-api/build.gradle reference `../..` for engine + shared libs.
#
# CRITICAL TZ POLICY: The container TZ env (Asia/Seoul from .env) is left
# UNCHANGED. The lottery code pins Asia/Ho_Chi_Minh internally via
# LotteryClock.VN + @Scheduled(zone="Asia/Ho_Chi_Minh"). Do NOT add TZ here.
# =============================================================================

# --- Stage A: parent shared-lib + engine build (Gradle 5.2.1) -----------------
FROM eclipse-temurin:8-jdk AS parent-build

WORKDIR /build
COPY . /build/

SHELL ["/bin/bash", "-o", "pipefail", "-c"]

RUN --mount=type=cache,sharing=locked,target=/root/.gradle/caches \
    --mount=type=cache,sharing=locked,target=/root/.gradle/wrapper \
    chmod +x gradlew && \
    ./gradlew --no-daemon -x test \
        :VbeeCommon:jar :VBeeSecurity:jar \
        :VinPlayCardLib:jar :VinPlayDAL:jar :VinPlayUserCore:jar \
        :CardCoreLib:jar \
        :BitzeroAll:jar :BitzeroMini:jar :BitZeroMinigame:jar \
        :game:lottery-engine:jar

# --- Stage B: lottery-api Spring Boot build (Gradle 7.6.4) --------------------
FROM gradle:7.6.4-jdk8 AS spring-build

ENV GRADLE_USER_HOME=/home/gradle/.gradle

WORKDIR /workspace

COPY --from=parent-build /build /workspace/backend-master

WORKDIR /workspace/backend-master/game/lottery-api

RUN --mount=type=cache,sharing=locked,target=/home/gradle/.gradle/caches \
    --mount=type=cache,sharing=locked,target=/home/gradle/.gradle/wrapper \
    gradle --no-daemon bootJar -x test

# --- Stage C: runtime (JRE 8) -------------------------------------------------
FROM eclipse-temurin:8-jre

ARG GIT_SHA=unknown
ARG BUILD_TIME=unknown
LABEL source.sha="${GIT_SHA}"
LABEL build.time="${BUILD_TIME}"
ENV SOURCE_SHA=${GIT_SHA}
ENV BUILD_TIME=${BUILD_TIME}

RUN apt-get update -qq && apt-get install -y -qq curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=spring-build \
    /workspace/backend-master/game/lottery-api/build/libs/lottery-api-0.1.0-SNAPSHOT-boot.jar \
    /app/lottery-api.jar

# Bake legacy config templates — same pattern as minigame-api Dockerfile.
COPY --from=spring-build \
    /workspace/backend-master/game/Minigame/config/db_pool.properties \
    /app/config-template/db_pool.properties
COPY --from=spring-build \
    /workspace/backend-master/game/Minigame/config/mongo.properties \
    /app/config-template/mongo.properties

# Shell entrypoint: copy templates, substitute credentials, exec JAR.
COPY game/lottery-api/docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

# Default flag values — Spring app ON; engine/bridge OFF for safe ramp.
ENV LOTTERY_API_ENABLED=1
ENV LOTTERY_ENGINE_ENABLED=0
ENV LOTTERY_ENGINE_GHOST_MODE=0
ENV LOTTERY_SHADOW_DIFF_ENABLED=0
ENV LOTTERY_MODE5_LEGACY_3OF4=0

EXPOSE 9292 9293

HEALTHCHECK --interval=30s --timeout=10s --retries=5 --start-period=60s \
    CMD curl -fsS http://localhost:9293/actuator/health > /dev/null || \
        curl -fsS http://localhost:9292/actuator/health > /dev/null || \
        exit 1

ENTRYPOINT ["/app/docker-entrypoint.sh"]
