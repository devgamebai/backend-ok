package com.vinplay.usercore.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * SUN-1026 — post-init audit of GameCommon's `cacheConfig` IMap.
 *
 * The existing {@link GameCommon#init()} swallows each per-section parse
 * failure with a {@code logger.warning("Failed to load config section X")}
 * and continues. That makes startup "succeed" even when payment gateways,
 * OTP routing, or mission rules silently degraded — a recurring class of
 * self-healing failure (the container is green but the feature is broken).
 *
 * This class runs immediately after {@code init()} populates the map and:
 *   1. Verifies every key in {@link #CRITICAL_KEYS} is present and
 *      non-empty (these are the keys that, if missing, cause a
 *      user-facing feature to silently fail).
 *   2. Logs SEVERE for each missing key — promoted from WARNING so the
 *      platform owner sees it in dashboards/alerts.
 *   3. Optionally fails startup (throws RuntimeException) when the
 *      environment variable {@code CONFIG_STRICT=1} is set. Default is
 *      non-strict so existing staging deployments are unaffected.
 *
 * This is the v1 hardening. A longer-term follow-up (tracked in SUN-1026)
 * is to introduce tolerant per-section loaders that log the offending
 * {@code game_config} row id, so DB fixes can be targeted.
 */
public final class ConfigSanityCheck {

    private static final Logger logger = Logger.getLogger("ConfigSanityCheck");

    /**
     * Keys that MUST be populated by {@link GameCommon#init()} for the
     * platform to serve user traffic correctly. A missing key here points
     * at a failed parse in the corresponding game_config row.
     *
     *   ESMS_API_KEY        → OTP delivery (esms)
     *   vcoin_url           → VTC Pay gateway (vtc_vcoin)
     *   RECHARGE_GATE_PRIMARY → payment gate routing (priority_partner)
     *   BRAND_NAME_ID       → SMS brand ID (from brandname DAO)
     *   STATUS_GAME         → master on/off switch (web)
     */
    private static final List<String> CRITICAL_KEYS = Arrays.asList(
            "ESMS_API_KEY",
            "vcoin_url",
            "RECHARGE_GATE_PRIMARY",
            "BRAND_NAME_ID",
            "STATUS_GAME"
    );

    private ConfigSanityCheck() {
        // utility class
    }

    /**
     * Audit the populated config IMap. Never throws under default config.
     * Set {@code CONFIG_STRICT=1} to promote missing-key errors to
     * startup-aborting RuntimeExceptions.
     */
    public static void auditInitState(Map<?, ?> configMap) {
        List<String> missing = new ArrayList<>();
        for (String key : CRITICAL_KEYS) {
            Object v = configMap == null ? null : configMap.get(key);
            if (v == null || String.valueOf(v).isEmpty()) {
                missing.add(key);
            }
        }

        if (missing.isEmpty()) {
            logger.info("ConfigSanityCheck: all " + CRITICAL_KEYS.size()
                    + " critical config keys loaded from game_config");
            return;
        }

        logger.severe("=====================================================");
        logger.severe("ConfigSanityCheck: " + missing.size() + "/"
                + CRITICAL_KEYS.size() + " critical config keys MISSING");
        for (String k : missing) {
            logger.severe("  MISSING: " + k);
        }
        logger.severe("This points at a failed game_config row parse. See the");
        logger.severe("preceding 'Failed to load config section' WARNINGs to");
        logger.severe("identify which DB rows need to be corrected.");
        logger.severe("=====================================================");

        if (isStrict()) {
            throw new RuntimeException(
                    "ConfigSanityCheck failed in strict mode: "
                            + missing.size() + " critical keys missing ("
                            + String.join(", ", missing) + ")");
        }
    }

    private static boolean isStrict() {
        String v = System.getenv("CONFIG_STRICT");
        return v != null && (v.equals("1") || v.equalsIgnoreCase("true"));
    }
}
