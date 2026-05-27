package com.vinplay.vbee.common.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * AWC (Asia Win Connect) environment configuration.
 *
 * Kill switch: AWC_ENABLED=0 disables all AWC features (default OFF).
 * All values come from environment variables — switch staging/prod via .env.
 */
public final class AwcConfig {

    private AwcConfig() {}

    /**
     * Per-nickname overrides for the AWC userId mapping. AWC locks the
     * currency at member-creation time and provides no delete endpoint, so
     * when an account gets poisoned (created under wrong currency or
     * without betLimit) the only repair is to mint a brand-new AWC userId
     * for the same internal player.
     *
     * Key: our internal nickname (users.nick_name).
     * Value: the string fed into toAwcUserId in place of the nickname.
     *        Final AWC userId = prefix() + value (lowercased, alnum-only).
     *
     * Reverse-lookup (incoming callbacks) is handled by
     * NICKNAME_OVERRIDES_REVERSE — keep both maps in sync.
     *
     * 2026-05-16: sunkrlaviai + sunkrzuestang2 stuck on KRW currency on
     * AWC's side after a misconfigured createMember window. AWC support
     * confirmed currency can't be changed post-creation. These overrides
     * route to fresh AWC userIds without touching nick_name (which the
     * agent commission tree, wallet history, and other providers depend
     * on for player identity).
     */
    private static final Map<String, String> NICKNAME_OVERRIDES;
    private static final Map<String, String> NICKNAME_OVERRIDES_REVERSE;
    static {
        Map<String, String> fwd = new HashMap<>();
        fwd.put("laviai",      "laviai2");
        fwd.put("zuestang222", "zuestang2v2");
        NICKNAME_OVERRIDES = Collections.unmodifiableMap(fwd);
        Map<String, String> rev = new HashMap<>();
        for (Map.Entry<String, String> e : fwd.entrySet()) {
            rev.put(e.getValue().toLowerCase().replaceAll("[^0-9a-z]", ""), e.getKey());
        }
        NICKNAME_OVERRIDES_REVERSE = Collections.unmodifiableMap(rev);
    }

    public static boolean isEnabled() {
        String v = getenv("AWC_ENABLED", "0");
        return "1".equals(v) || "true".equalsIgnoreCase(v);
    }

    public static String agentId() {
        return getenv("AWC_AGENT_ID", "lime01");
    }

    public static String cert() {
        return getenv("AWC_CERT", "PigVq2D07hNL");
    }

    public static String prefix() {
        return getenv("AWC_PREFIX", "lime");
    }

    public static String apiUrl() {
        return getenv("AWC_API_URL", "https://awc-api.apihub888.com");
    }

    public static String callbackUrl() {
        return getenv("AWC_CALLBACK_URL", "");
    }

    public static String currency() {
        return getenv("AWC_CURRENCY", "VND");
    }

    public static String language() {
        return getenv("AWC_LANGUAGE", "en");
    }

    /**
     * Build AWC userId from our internal username.
     * Format: prefix + lowercase username, max 21 chars, [0-9a-z] only.
     * Applies per-nickname overrides (see NICKNAME_OVERRIDES) before
     * prefixing — used to bypass currency-locked stuck accounts on AWC.
     */
    public static String toAwcUserId(String username) {
        if (username == null) return prefix();
        String effective = NICKNAME_OVERRIDES.getOrDefault(username, username);
        String clean = (prefix() + effective).toLowerCase().replaceAll("[^0-9a-z]", "");
        if (clean.length() > 21) clean = clean.substring(0, 21);
        return clean;
    }

    /**
     * Reverse of toAwcUserId for the callback path. Given the candidate
     * value left after stripping prefix() from an incoming awcUserId,
     * return the original internal nickname so the existing
     * `WHERE nick_name = ?` lookups continue to find the player.
     */
    public static String reverseNicknameOverride(String candidate) {
        if (candidate == null) return null;
        String key = candidate.toLowerCase().replaceAll("[^0-9a-z]", "");
        return NICKNAME_OVERRIDES_REVERSE.getOrDefault(key, candidate);
    }

    /**
     * Verify an AWC callback key matches our cert.
     */
    public static boolean verifyCallbackKey(String key) {
        return cert().equals(key);
    }

    private static String getenv(String key, String fallback) {
        String v = System.getenv(key);
        return (v == null || v.isEmpty()) ? fallback : v;
    }
}
