-- SUN-AWC-DECIMAL: per-user fractional residue tracker for AWC sub-VND amounts
-- AWC sends amounts with up to 3 decimal places (e.g. winAmount: 19.5).
-- users.vin is BIGINT (integer VND) so 0.500 fractions were silently dropped.
-- This table tracks the 0-999 milli-VND residue per user for AWC-only.

CREATE TABLE IF NOT EXISTS vinplay.awc_user_residue (
    user_id BIGINT NOT NULL PRIMARY KEY,
    residue_milli_vnd INT NOT NULL DEFAULT 0
        COMMENT 'Sub-VND fractional residue, 0-999. 500 = 0.5 VND.',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                                 ON UPDATE CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
