# =============================================================================
# Multi-stage Dockerfile for SunKR Java Backend
# Stage 1: Build all modules from source with Gradle (JDK 8)
# Stage 2: Runtime image with compiled JARs (JRE 8)
# =============================================================================
# All modules compile from source. No precompiled JAR overrides needed.
# =============================================================================

# --- Stage 1: Build ---
FROM eclipse-temurin:8-jdk AS builder

# Source commit SHA + build timestamp, propagated from deploy.sh via
# `--build-arg GIT_SHA=... --build-arg BUILD_TIME=...`. Both default to
# "unknown" so local/dev builds don't fail when the args aren't supplied.
# Stamped onto the runtime image in Stage 2 so `docker inspect` can tell
# at a glance which commit a running container is actually serving —
# prevents the class of "forgot --build / stale jar still running" bug
# (incident 2026-04-22: DepositTelegramBot old bytecode kept running
# because `up --force-recreate` was called after source edits without a
# preceding `build`, and the bug was only noticed on visual inspection
# of the Telegram message).
ARG GIT_SHA=unknown
ARG BUILD_TIME=unknown

WORKDIR /build
COPY . /build/

# Build all modules from source.
# Must use bash with pipefail — default /bin/sh (dash) silently ignores
# `set -o pipefail`, so `./gradlew … | tail -10` would mask compile errors
# (tail exits 0, the RUN "succeeds", the image ships with whatever JARs
# happened to build before the failing one). That bit us on SUN-775: a
# compile error in GetRebateLogsProcessor produced an image with only
# vbee-1.0.jar in /app/libs/app and a silent container restart loop.
SHELL ["/bin/bash", "-o", "pipefail", "-c"]
# GitLab #24: dropped `| tail -10`. The pipefail + tail combo DID surface the
# exit code, but discarded the compile-error line itself — incident of
# 2026-04-22 lost 30 minutes hand-re-running gradle without the filter to
# find that LogoutSusscessHandler.java:32 was the offender. Let the full
# gradle log stream; BuildKit's interactive output is truncated anyway.
#
# GitLab #26 win #1: BuildKit cache mounts for gradle caches + wrapper
# downloads. Saves 3-5 min on every rebuild — Maven deps, compiled module
# classes, and the Gradle wrapper distribution survive across runs even
# though the layer above invalidates. First run populates, subsequent
# runs hit warm cache. Requires Docker BuildKit (default on 23.0+).
RUN --mount=type=cache,target=/root/.gradle/caches \
    --mount=type=cache,target=/root/.gradle/wrapper \
    chmod +x gradlew && \
    ./gradlew build copyRuntimeDeps -x test --no-daemon

# XML-class parity lint — fail the build if any <command><path>FQN</path></command>
# in api_backend.xml / api_portal.xml routes to a class that does not exist in the
# corresponding compiled JAR. Catches the !216 class — silent runtime SKIPs become
# loud build failures. Runs against the freshly-built JARs in /build/api/*/build/libs/.
# Note: the JDBC DECIMAL/getLong type-symmetry lint lives in
# backend-master/scripts/check-decimal-getlong.sh — runs on host (needs install/flyway/sql/
# from repo root, which is outside this Dockerfile's build context).
RUN apt-get update -qq && apt-get install -y --no-install-recommends unzip python3 && \
    rm -rf /var/lib/apt/lists/* && \
    set -e && \
    # Build full classpath class index from all source-built JARs (matches what
    # /app/libs/{app,runtime} sees at runtime). Cross-jar processor refs are
    # legit — XML in api_backend.xml may point at classes built in vbee, DAL,
    # or UserCore. So we union all class lists, not just VinPlayBackend-*.jar.
    : > /tmp/all-classes.txt && \
    for jar in \
        api/VinPlayBackend/build/libs/VinPlayBackend-*.jar \
        api/VinPlayPortal/build/libs/VinPlayPortal-*.jar \
        api/vbee/build/libs/vbee-*.jar \
        api/payment/build/libs/*.jar \
        VinPlayDAL/build/libs/VinPlayDAL-*.jar \
        VinPlayUserCore/build/libs/VinPlayUserCore-*.jar \
        VinPlayCardLib/build/libs/VinPlayCardLib-*.jar \
        CardCoreLib/build/libs/CardCoreLib-*.jar \
        VbeeCommon/build/libs/VbeeCommon-*.jar \
    ; do \
        [ -f "$jar" ] || continue; \
        unzip -l "$jar" | awk '/\.class$/{ gsub("/",".",$NF); sub("\\.class$","",$NF); print $NF }' >> /tmp/all-classes.txt; \
    done && \
    LC_ALL=C sort -u /tmp/all-classes.txt -o /tmp/all-classes.txt && \
    for pair in \
        "api/VinPlayBackend/config/api_backend.xml:backend" \
        "api/VinPlayPortal/config/api_portal.xml:portal" \
    ; do \
        xml="${pair%:*}"; label="${pair##*:}"; \
        [ -f "$xml" ] || { echo "[$label] missing $xml"; exit 1; }; \
        python3 -c "import re,sys; t=open('$xml').read(); t=re.sub(r'<!--.*?-->','',t,flags=re.S); [print(p) for p in set(re.findall(r'<path>([^<]+)</path>',t))]" \
            | LC_ALL=C sort -u > /tmp/$label-paths.txt; \
        missing=$(LC_ALL=C comm -23 /tmp/$label-paths.txt /tmp/all-classes.txt); \
        if [ -n "$missing" ]; then \
            echo "FAIL [$label]: XML <path> entries with no class in any source-built JAR:"; \
            echo "$missing" | sed 's/^/  /'; \
            echo "Either restore the class or remove the <command> entry from XML."; \
            exit 1; \
        fi; \
        echo "OK [$label]: all XML <path> entries resolve to classes in source-built JARs."; \
    done

# Money-write chokepoint lint — fail the build if any UPDATE on a wallet
# column appears OUTSIDE the canonical gateway files. Coverage extended
# 2026-05-01 to include xu / safe / money_vp / vip_point / recharge_money
# / vin_total / xu_total columns (the original lint only caught vin and
# current_money, leaving the BanCa fish wallet and VIP-point side bypassed).
#
# Cache-write bypasses (userMap.put / user.setVin) cannot be matched
# reliably by regex alone (too many false positives from legit cache-load
# code that reads from DB into a UserCacheModel). They're addressed at the
# architectural level by the read-through cache redesign in
# docs/MONEY_LEDGER_SCHEMA.md §2.4. The negative-balance trigger applied in
# 2026_05_02_users_negative_balance_trigger.sql catches the *effect* of
# any cache-staleness bug at the moment it lands in DB.
#
# legacy_allow lists files awaiting refactor; entries should empty out
# after the money_ledger migration completes.
#
# SUN-1240: UserServiceImpl.updateMoney was just rewritten to do an atomic
# `UPDATE users SET vin = vin + ?, vin_total = vin_total + ? WHERE id = ?
# AND vin + ? >= 0` directly — closes the cache-TOCTOU exploit that drained
# ~65M VIN through SicBo. The proper next step is routing through
# MoneyGateway.debitUser (which doesn't currently write vin_total and would
# need a small extension); until that lands, UserServiceImpl is on
# legacy_allow with this ticket reference.
RUN set -e && \
    canonical_allow="VinPlayDAL/src/main/java/com/vinplay/dal/service/MoneyGateway.java VinPlayDAL/src/main/java/com/vinplay/dal/rebate/RebateService.java VinPlayDAL/src/main/java/com/vinplay/dal/dao/impl/CreditWalletDaoImpl.java VinPlayDAL/src/main/java/com/vinplay/dal/dao/CreditWalletDao.java"; \
    legacy_allow=""; \
    allow_pat=$(echo "$canonical_allow $legacy_allow" | tr ' ' '\n' | grep -v '^$' | sed 's/[.]/\\./g' | paste -sd '|' -); \
    violations=$(grep -RHnE "UPDATE\s+(vinplay\.)?users\s+SET[^;]*\b(vin|current_money|xu|safe|money_vp|recharge_money|vin_total|xu_total|vip_point|vip_point_save)\b|UPDATE\s+(vinplay\.)?agency_wallet|UPDATE\s+(vinplay\.)?credit_wallet" \
        --include='*.java' \
        --exclude-dir=test \
        VinPlayDAL VinPlayUserCore VinPlayCardLib VbeeCommon CardCoreLib api/ game/ 2>/dev/null \
        | grep -vE "$allow_pat" || true); \
    if [ -n "$violations" ]; then \
        echo "FAIL [money-gateway]: direct UPDATE on a wallet column outside the canonical gateway:"; \
        echo "$violations" | sed 's/^/  /'; \
        echo ""; \
        echo "Every money move must go through MoneyGateway.creditUser / RebateService.creditAgencyWallet /"; \
        echo "CreditWalletService.* / (after rollout) post_money_transaction SP, so an audit row lands"; \
        echo "in money_gateway_log / agency_wallet_transactions / credit_wallet_transactions / money_ledger."; \
        echo "See docs/MONEY_FLOW_AUDIT_2026-05-01.md for the current bypass map."; \
        echo "Add the file to legacy_allow with a SUN-XXXX migration ticket if intentional."; \
        exit 1; \
    fi; \
    legacy_count=$(echo "$legacy_allow" | wc -w); \
    echo "OK [money-gateway]: no UNALLOWED direct money-table UPDATEs ($legacy_count legacy bypass file(s) on allowlist — see docs/MONEY_FLOW_AUDIT_2026-05-01.md)."

# Collect all JARs into organized directories
RUN mkdir -p /dist/libs/runtime /dist/libs/app /dist/libs/api-precompiled && \
    \
    # --- Runtime dependencies (3rd party from Maven) ---
    cp runtime-libs/*.jar /dist/libs/runtime/ 2>/dev/null || true && \
    # Remove SLF4J 2.x — incompatible with logback 1.2.x (causes "No SLF4J providers" error)
    rm -f /dist/libs/runtime/slf4j-api-2.0.7.jar && \
    # Remove Servlet API 2.3 — pulled in transitively by an old dep, shadows the
    # javax.servlet-api 4.0.1 that Jetty 9.4 needs. Symptom: backend-api boots
    # to "Starting Jetty on port 19082..." then crashes with
    # NoSuchMethodError: javax.servlet.ServletContext.createFilter — that
    # method only exists in Servlet 3.0+. Drop the 2.3 jar so the 4.0.1 wins.
    rm -f /dist/libs/runtime/servlet-api-2.3.jar && \
    \
    # --- Shared libraries (built from source) ---
    for jar in \
        BitzeroAll/build/libs/BitzeroAll-*.jar \
        BitzeroMini/build/libs/BitzeroMini-*.jar \
        BitZeroMinigame/build/libs/BitZeroMinigame-*.jar \
        CardCoreLib/build/libs/CardCoreLib-*.jar \
        VbeeCommon/build/libs/VbeeCommon-*.jar \
        VinPlayDAL/build/libs/VinPlayDAL-*.jar \
        VinPlayUserCore/build/libs/VinPlayUserCore-*.jar \
        VinPlayCardLib/build/libs/VinPlayCardLib-*.jar \
        VBeeSecurity/build/libs/VBeeSecurity-*.jar \
    ; do cp $jar /dist/libs/runtime/ 2>/dev/null || true; done && \
    \
    # --- Application JARs (APIs + games, built from source) ---
    for jar in \
        api/VinPlayPortal/build/libs/VinPlayPortal-1.0.jar \
        api/VinPlayBackend/build/libs/VinPlayBackend-1.0.jar \
        api/vbee/build/libs/vbee-1.0.jar \
        api/payment/build/libs/payment.jar \
        api/commission-history-drainer/build/libs/commission-history-drainer-1.0.jar \
        banca-replay-worker/build/libs/banca-replay-worker-1.0.jar \
        game/slot/build/libs/SlotMachine-1.0.jar \
        game/Minigame/build/libs/Minigame.jar \
        game/thirdParty/build/libs/thirdParty-1.0.jar \
        game/poker/build/libs/poker-1.0.jar \
        game/pokertour/build/libs/pokertour-1.0.jar \
        game/tlmn/build/libs/tlmn-1.0.jar \
        game/sam/build/libs/sam-1.0.jar \
        game/binh/build/libs/binh-1.0.jar \
        game/lieng/build/libs/lieng-1.0.jar \
        game/baicao/build/libs/baicao-1.0.jar \
        game/bacayServer/build/libs/bacayServer-1.0.jar \
        game/xizach/build/libs/xizach-1.0.jar \
        game/caro/build/libs/caro-1.0.jar \
        game/cotuong/build/libs/cotuong-1.0.jar \
        game/coup/build/libs/coup-1.0.jar \
        game/xocdia/build/libs/xocdia-1.0.jar \
        game/xocdiamd5/build/libs/xocdiamd5-1.0.jar \
    ; do cp $jar /dist/libs/app/ 2>/dev/null || true; done && \
    \
    # --- Legacy precompiled fat JARs (VinGameBase, VinOTP, VinPayment) ---
    # These are fat JARs bundling old copies of all modules. They're only needed
    # for classes that don't exist in any source module (e.g. legacy payment providers).
    # Strip out dal/vbee/usercore packages to avoid classpath conflicts with source-built JARs.
    for strip_jar in precompiled/VinGameBase.jar precompiled/VinOTP.jar precompiled/VinPayment.jar; do \
        if [ -f "$strip_jar" ]; then \
            bn=$(basename "$strip_jar"); \
            mkdir -p /tmp/ap_strip && cd /tmp/ap_strip && \
            jar xf /build/$strip_jar && \
            rm -rf com/vinplay/dal com/vinplay/vbee com/vinplay/usercore com/vinplay/dichvuthe \
                   com/payment com/gamebase bitzero 2>/dev/null; \
            jar cf /dist/libs/api-precompiled/$bn -C . . && \
            cd /build && rm -rf /tmp/ap_strip && \
            echo "Stripped and copied api-precompiled/$bn"; \
        fi; \
    done && \
    \
    # --- GameConfig conflict ---
    # Both Minigame.jar and SlotMachine-1.0.jar contain game.GameConfig.GameConfig
    # with different fields. Entrypoint.sh resolves this per-container at startup.
    echo "Build complete: $(ls /dist/libs/app/ | wc -l) app JARs, $(ls /dist/libs/runtime/ | wc -l) runtime JARs"

# --- Stage 2: Runtime ---
FROM eclipse-temurin:8-jre

# Re-declare ARGs so LABEL/ENV can reference them in this stage
ARG GIT_SHA=unknown
ARG BUILD_TIME=unknown

LABEL source.sha="${GIT_SHA}"
LABEL build.time="${BUILD_TIME}"
ENV SOURCE_SHA=${GIT_SHA}
ENV BUILD_TIME=${BUILD_TIME}

# zip needed for GameConfig conflict resolution in entrypoint
RUN apt-get update -qq && apt-get install -y -qq zip curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy compiled JARs from builder
COPY --from=builder /dist/libs /app/libs

# Copy entrypoint
COPY entrypoint.sh /app/entrypoint.sh

# Copy shared config
COPY config/ /app/config/

# Copy per-service configs
COPY api/VinPlayPortal/config/ /app/api/VinPlayPortal/config/
COPY api/VinPlayBackend/config/ /app/api/VinPlayBackend/config/
COPY api/vbee/config/ /app/api/vbee/config/

# Copy game configs + conf directories
COPY game/slot/config/ /app/game/slot/config/
COPY game/slot/conf/ /app/game/slot/conf/
COPY game/Minigame/config/ /app/game/Minigame/config/
COPY game/Minigame/conf/ /app/game/Minigame/conf/
COPY game/poker/config/ /app/game/poker/config/
COPY game/poker/conf/ /app/game/poker/conf/
COPY game/pokertour/config/ /app/game/pokertour/config/
COPY game/pokertour/conf/ /app/game/pokertour/conf/
COPY game/tlmn/config/ /app/game/tlmn/config/
COPY game/tlmn/conf/ /app/game/tlmn/conf/
COPY game/xocdia/config/ /app/game/xocdia/config/
COPY game/xocdia/conf/ /app/game/xocdia/conf/
COPY game/xocdia/config/ /app/game/xocdiatulinh/config/
COPY game/xocdia/conf-tulinh/ /app/game/xocdiatulinh/conf/
COPY game/bacayServer/config/ /app/game/bacayServer/config/
COPY game/bacayServer/conf/ /app/game/bacayServer/conf/
COPY game/binh/config/ /app/game/binh/config/
COPY game/binh/conf/ /app/game/binh/conf/
COPY game/lieng/config/ /app/game/lieng/config/
COPY game/lieng/conf/ /app/game/lieng/conf/
COPY game/sam/config/ /app/game/sam/config/
COPY game/sam/conf/ /app/game/sam/conf/
COPY game/baicao/config/ /app/game/baicao/config/
COPY game/baicao/conf/ /app/game/baicao/conf/
COPY game/xizach/config/ /app/game/xizach/config/
COPY game/xizach/conf/ /app/game/xizach/conf/
COPY game/caro/config/ /app/game/caro/config/
COPY game/caro/conf/ /app/game/caro/conf/
COPY game/cotuong/config/ /app/game/cotuong/config/
COPY game/cotuong/conf/ /app/game/cotuong/conf/
COPY game/coup/config/ /app/game/coup/config/
COPY game/coup/conf/ /app/game/coup/conf/
COPY game/thirdParty/config/ /app/game/thirdParty/config/

RUN chmod +x /app/entrypoint.sh

ENTRYPOINT ["/app/entrypoint.sh"]
