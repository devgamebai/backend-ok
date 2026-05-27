package com.vinplay.vbee.common.security;

import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;

/**
 * Internal-only captcha / 2FA bypass for QC + ops smoke tests.
 *
 * <p>Activated by setting the {@code SMOKE_TEST_BYPASS_KEY} environment
 * variable to a non-empty random secret. With the env var unset (the
 * default in production), every {@link #isAllowed} call returns
 * {@code false} and the bypass is fully disabled.
 *
 * <p>When enabled, a caller must satisfy <em>all three</em> conditions:
 * <ol>
 *   <li>Submit the {@code X-Smoke-Test-Key} HTTP header</li>
 *   <li>The header value must match the env-var secret (constant-time
 *       compare to defeat timing oracles)</li>
 *   <li>The client IP (X-Forwarded-For first hop, or remote addr) must
 *       fall in an internal RFC 1918 / loopback range — covering the
 *       Docker compose network and any host on the same VPN</li>
 * </ol>
 *
 * <p>Failing any condition: the bypass is denied silently (no error,
 * no log spam) so brute-force attempts can't enumerate the env var.
 * On success the call is logged at WARN so an audit-trail entry exists
 * for every bypass use.
 *
 * <h2>Threat model</h2>
 * <ul>
 *   <li>Public attacker: blocked at the IP filter — they can't reach an
 *       internal range without first compromising the network perimeter.</li>
 *   <li>Compromised internal host: blocked unless they also know the
 *       env-var secret. The secret should be long random + rotated
 *       per QC sweep + deleted from the env after the sweep ends.</li>
 *   <li>Insider with cluster access: explicitly in scope — this is a
 *       smoke-test tool, not a security boundary. Don't enable in
 *       production for routine operation.</li>
 * </ul>
 *
 * <h2>Suggested operational pattern</h2>
 * <pre>
 *   # On the smoke-test host (inside the cluster network):
 *   export SMOKE_TEST_BYPASS_KEY=$(openssl rand -hex 32)
 *   # Restart the relevant API service so it picks up the env var.
 *   # Run smoke tests with header X-Smoke-Test-Key: $SMOKE_TEST_BYPASS_KEY
 *   # When done:
 *   unset SMOKE_TEST_BYPASS_KEY
 *   # Restart again — bypass disabled.
 * </pre>
 */
public final class SmokeTestBypass {

    private static final Logger logger = Logger.getLogger("backend");

    static final String ENV_NAME = "SMOKE_TEST_BYPASS_KEY";
    static final String HEADER_NAME = "X-Smoke-Test-Key";

    private SmokeTestBypass() { /* static utility */ }

    /**
     * @param request the inbound request (may be null — returns false)
     * @param purpose short tag for the audit log line, e.g. {@code
     *                "admin-login"}, {@code "agent-login"}
     * @return {@code true} iff env var set + header matches + client IP
     *         is internal. {@code false} otherwise (including the
     *         disabled case where the env var is unset).
     */
    public static boolean isAllowed(HttpServletRequest request, String purpose) {
        if (request == null) return false;

        String expected;
        try {
            expected = System.getenv(ENV_NAME);
        } catch (Throwable ignored) {
            return false;
        }
        if (expected == null || expected.isEmpty()) {
            // Bypass disabled — production default.
            return false;
        }

        String supplied = request.getHeader(HEADER_NAME);
        if (supplied == null || !constantTimeEquals(expected, supplied)) {
            return false;
        }

        String clientIp = clientIp(request);
        if (!isInternalIp(clientIp)) {
            // Valid key from a public IP — explicit denial + audit-log
            // line so the ops team can investigate (potential leak of
            // the env-var secret).
            logger.warn("SmokeTestBypass[" + purpose + "]: valid key from EXTERNAL ip="
                    + clientIp + " — REJECTED");
            return false;
        }

        logger.warn("SmokeTestBypass[" + purpose + "]: bypass GRANTED from internal ip="
                + clientIp);
        return true;
    }

    /**
     * Constant-time string comparison — defeats timing-side-channel
     * attempts to learn the env-var secret one character at a time.
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }

    /**
     * Extract the client IP. Honors {@code X-Forwarded-For} (first hop)
     * since the API runs behind nginx; falls back to
     * {@link HttpServletRequest#getRemoteAddr} for direct connections.
     */
    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            int comma = xff.indexOf(',');
            String first = comma < 0 ? xff : xff.substring(0, comma);
            return first.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * RFC 1918 / loopback / Docker default network allowlist.
     *
     * <ul>
     *   <li>{@code 127.0.0.1}, {@code ::1} — loopback</li>
     *   <li>{@code 10.0.0.0/8} — RFC 1918 large</li>
     *   <li>{@code 192.168.0.0/16} — RFC 1918 home/lab</li>
     *   <li>{@code 172.16.0.0 - 172.31.255.255} — RFC 1918 mid (covers
     *       Docker default {@code 172.17/16} bridge and compose networks)</li>
     *   <li>{@code fe80::/10} — IPv6 link-local</li>
     * </ul>
     *
     * <p>Public IPv4 ranges are deliberately excluded.
     */
    static boolean isInternalIp(String ip) {
        if (ip == null || ip.isEmpty()) return false;

        // IPv6 loopback / link-local.
        if (ip.equals("::1")) return true;
        if (ip.startsWith("fe80")) return true;

        // IPv4 loopback.
        if (ip.equals("127.0.0.1") || ip.startsWith("127.")) return true;

        // RFC 1918 — 10.0.0.0/8.
        if (ip.startsWith("10.")) return true;

        // RFC 1918 — 192.168.0.0/16.
        if (ip.startsWith("192.168.")) return true;

        // RFC 1918 — 172.16.0.0 to 172.31.255.255 (incl. Docker default).
        if (ip.startsWith("172.")) {
            int dot1 = ip.indexOf('.', 4);
            if (dot1 > 4) {
                try {
                    int second = Integer.parseInt(ip.substring(4, dot1));
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }

        return false;
    }
}
