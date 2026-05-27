package com.vinplay.vbee.common.agent;

/**
 * Shared format validator for agent vanity referral codes.
 *
 * Rules:
 *   - 4 to 12 characters
 *   - alphanumeric only (A-Z, 0-9); stored UPPERCASE
 *   - first char must be a letter (avoid confusion with numeric user IDs)
 *
 * Uniqueness + reserved-word checks are DB-backed and done by the caller.
 */
public final class AgentCodeValidator {
    public static final int MIN_LEN = 4;
    public static final int MAX_LEN = 10;

    private AgentCodeValidator() {}

    public static class Result {
        public final boolean ok;
        public final String errorCode;
        public final String message;
        public final String normalized;
        private Result(boolean ok, String errorCode, String message, String normalized) {
            this.ok = ok; this.errorCode = errorCode; this.message = message; this.normalized = normalized;
        }
        public static Result ok(String normalized) { return new Result(true, "0", "ok", normalized); }
        public static Result err(String code, String msg) { return new Result(false, code, msg, null); }
    }

    public static Result validate(String desiredCode) {
        if (desiredCode == null) return Result.err("4001", "desired_code required");
        String s = desiredCode.trim().toUpperCase();
        if (s.isEmpty()) return Result.err("4001", "desired_code required");
        if (s.length() < MIN_LEN || s.length() > MAX_LEN) {
            return Result.err("4002", "length must be between " + MIN_LEN + " and " + MAX_LEN);
        }
        if (!s.matches("^[A-Z0-9]+$")) {
            return Result.err("4002", "only A-Z and 0-9 allowed");
        }
        if (!Character.isLetter(s.charAt(0))) {
            return Result.err("4003", "must start with a letter");
        }
        return Result.ok(s);
    }

    /** Checks if normalized code matches any reserved pattern (exact or prefix-wildcard). */
    public static boolean matchesReserved(String normalizedCode, java.util.List<String> reservedPatterns) {
        if (normalizedCode == null || reservedPatterns == null) return false;
        for (String p : reservedPatterns) {
            if (p == null) continue;
            String up = p.toUpperCase();
            if (up.endsWith("*")) {
                if (normalizedCode.startsWith(up.substring(0, up.length() - 1))) return true;
            } else {
                if (normalizedCode.equals(up)) return true;
            }
        }
        return false;
    }
}
