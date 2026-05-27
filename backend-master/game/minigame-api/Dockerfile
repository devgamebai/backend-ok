# =============================================================================
# minigame-api — Spring Boot 2.7.18 standalone
# =============================================================================
# Hosts TaiXiu + Sicbo REST + STOMP on port 9290 (mgmt actuator on 9291).
#
# Build pipeline:
#   1. Stage A (parent-build): Gradle 5.2.1 against backend-master root to
#      produce the shared-lib JARs minigame-api depends on as file refs
#      (VbeeCommon, VinPlayUserCore, VinPlayDAL, VinPlayCardLib, CardCoreLib,
#       BitzeroAll, BitzeroMini, BitZeroMinigame, VBeeSecurity) plus the
#      :game:minigame-engine jar (also Gradle 5.2.1).
#   2. Stage B (spring-build): Gradle 7.6.4 against game/minigame-api/ —
#      Spring Boot 2.7.18 plugin requires Gradle 6.8+, root cannot do this.
#   3. Stage C (runtime): JRE 8 with the boot fat jar.
#
# Build context: backend-master/ (parent dir). Required to pull both the
# minigame-api source AND the parent shared-lib + engine modules.
# =============================================================================

# --- Stage A: parent shared-lib + engine build (Gradle 5.2.1) -----------------
FROM eclipse-temurin:8-jdk AS parent-build

WORKDIR /build
COPY . /build/

SHELL ["/bin/bash", "-o", "pipefail", "-c"]

# Build only the shared libs + minigame-engine. Skip the heavyweight
# api/* + game/* modules (not needed; would blow build time).
RUN --mount=type=cache,sharing=locked,target=/root/.gradle/caches \
    --mount=type=cache,sharing=locked,target=/root/.gradle/wrapper \
    chmod +x gradlew && \
    ./gradlew --no-daemon -x test \
        :VbeeCommon:jar :VBeeSecurity:jar \
        :VinPlayCardLib:jar :VinPlayDAL:jar :VinPlayUserCore:jar \
        :CardCoreLib:jar \
        :BitzeroAll:jar :BitzeroMini:jar :BitZeroMinigame:jar \
        :game:minigame-engine:jar

# --- Stage B: minigame-api Spring Boot build (Gradle 7.6.4) -------------------
FROM gradle:7.6.4-jdk8 AS spring-build

# Re-export GRADLE_USER_HOME so the cache mount survives across runs.
ENV GRADLE_USER_HOME=/home/gradle/.gradle

WORKDIR /workspace

# Copy the entire backend-master tree so the file-dep paths in
# game/minigame-api/build.gradle (`def parentBuild = file('../..')`) resolve.
COPY --from=parent-build /build /workspace/backend-master

# Stage-B build runs ONLY on the minigame-api module — uses its own
# standalone settings.gradle (rootProject.name = 'minigame-api').
WORKDIR /workspace/backend-master/game/minigame-api

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
    /workspace/backend-master/game/minigame-api/build/libs/minigame-api-0.1.0-SNAPSHOT-boot.jar \
    /app/minigame-api.jar

# Bake the legacy config templates into the image. The shell entrypoint
# sed-substitutes credentials from env vars at container start, mirroring
# entrypoint.sh in the legacy game servers.
COPY --from=spring-build \
    /workspace/backend-master/game/Minigame/config/db_pool.properties \
    /app/config-template/db_pool.properties
COPY --from=spring-build \
    /workspace/backend-master/game/Minigame/config/mongo.properties \
    /app/config-template/mongo.properties

# Shell entrypoint: copy templates → /app/config/, substitute credentials,
# then exec the JAR (exec preserves PID 1 for signal handling).
COPY game/minigame-api/docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

# Default flag values for the runtime container. The .env file passed via
# docker-compose `env_file:` overrides these per-deploy. Defaults flip the
# Spring app ON so curl smoke tests reach real code; BitZero bridge stays
# OFF (engine wiring is a separate ramp).
ENV MINIGAME_API_ENABLED=1
ENV MINIGAME_API_PUSH_ENABLED=0
ENV MINIGAME_API_BRIDGE_ENABLED=0
ENV MINIGAME_ENGINE_ENABLED=0
ENV MINIGAME_ENGINE_GHOST_MODE=0

EXPOSE 9290 9291

HEALTHCHECK --interval=30s --timeout=10s --retries=5 --start-period=60s \
    CMD curl -fsS http://localhost:9291/actuator/health > /dev/null || \
        curl -fsS http://localhost:9290/actuator/health > /dev/null || \
        exit 1

ENTRYPOINT ["/app/docker-entrypoint.sh"]
