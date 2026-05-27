package com.vinplay.vbee.common.utils;

import java.util.regex.Pattern;

/**
 * SUN-851: centralised password policy — single source of truth for every
 * backend entry point that accepts a raw (pre-MD5) password from FE.
 *
 * <p>Rule (per QC, 2026-04-13):
 * <pre>
 *   ^[a-zA-Z0-9!@#$%^&amp;*(),./;'\[\]{}|\\`~]{6,32}$
 * </pre>
 *
 * Allowed: upper/lower letters, digits, and the standard special set
 * {@code !@#$%^&*(),./;'[]{}|\`~}. Length 6–32 characters.
 *
 * <p>Behaviour:
 * <ul>
 *   <li>{@code null} / empty → rejected</li>
 *   <li>Pre-hashed MD5 (exactly 32 hex chars) → accepted as-is. Clients that
 *       md5-hash client-side will land here; the raw password has already
 *       been validated in the FE form before hashing. This preserves the
 *       existing flow and avoids double-regex against a hex string.</li>
 *   <li>Raw password that fails the regex → rejected with Vietnamese error.</li>
 * </ul>
 *
 * <p>Wired into: {@code ChangePasswordProcessor} (c=131), {@code CreateUserProcessor}
 * via {@code AdminUserSupport.validatePassword}, and {@code QuickRegisterProcessor}.
 */
public final class PasswordPolicy {

    /** Minimum raw password length. */
    public static final int MIN_LENGTH = 6;

    /** Maximum raw password length. */
    public static final int MAX_LENGTH = 32;

    /** Allowed character set — letters, digits, and the standard special set. */
    private static final Pattern ALLOWED_CHARS =
            Pattern.compile("^[a-zA-Z0-9!@#$%^&*(),./;'\\[\\]{}|\\\\`~]+$");

    /** Pre-hashed MD5 — 32 lowercase hex chars. */
    private static final Pattern MD5_HEX =
            Pattern.compile("^[a-fA-F0-9]{32}$");

    private PasswordPolicy() {}

    /**
     * Validate a password candidate.
     *
     * @param password raw password from FE (or an MD5 hex already hashed)
     * @return {@code null} if OK, otherwise a Vietnamese error message suitable
     *         for returning in API {@code message} field.
     */
    public static String validate(String password) {
        if (password == null || password.isEmpty()) {
            return "Mật khẩu không được để trống";
        }
        String trimmed = password.trim();

        // FE typically MD5s the password before sending. Accept those as-is —
        // the raw password was validated client-side before hashing.
        if (trimmed.length() == 32 && MD5_HEX.matcher(trimmed).matches()) {
            return null;
        }

        if (trimmed.length() < MIN_LENGTH) {
            return "Mật khẩu tối thiểu " + MIN_LENGTH + " ký tự";
        }
        if (trimmed.length() > MAX_LENGTH) {
            return "Mật khẩu tối đa " + MAX_LENGTH + " ký tự";
        }
        if (!ALLOWED_CHARS.matcher(trimmed).matches()) {
            return "Mật khẩu chứa ký tự không hợp lệ (chỉ cho phép A-Z, a-z, 0-9 và ký tự đặc biệt !@#$%^&*(),./;'[]{}|\\`~)";
        }
        return null;
    }

    /** Convenience: {@code true} if {@link #validate(String)} returns {@code null}. */
    public static boolean isValid(String password) {
        return validate(password) == null;
    }
}
