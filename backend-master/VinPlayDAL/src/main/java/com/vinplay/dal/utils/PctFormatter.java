package com.vinplay.dal.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * SUN-1098 / SUN-1095: emit DECIMAL percentage columns as 2-decimal
 * strings, not JSON Numbers.
 *
 * <p>The DB schema is correct — DECIMAL(5,2) stores 1.10 as 1.10. The
 * defect is at the Java→JSON serialisation boundary: when a percentage
 * is read with {@code rs.getDouble(...)} or wrapped in {@code Double},
 * {@code org.json.JSONObject.numberToString(Number)} (and Gson with
 * default Number serializer) unconditionally trims trailing zeros and
 * the player / agent CMS sees "1.2" or "0.1" or "0".
 *
 * <p>Use this helper at every site that emits a percentage to a
 * client-facing payload. Internal arithmetic that consumes the value
 * as a primitive double can keep using {@code rs.getDouble}.
 */
public final class PctFormatter {

    private PctFormatter() {}

    /**
     * Read column as BigDecimal and return "X.YY". Returns "0.00" on
     * SQL NULL or column-not-found (nullable column / older row /
     * spelling mismatch — same recovery behaviour as the previous
     * private helper in RebateService).
     */
    public static String formatRs(ResultSet rs, String column) {
        try {
            BigDecimal v = rs.getBigDecimal(column);
            if (v == null) return "0.00";
            return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
        } catch (SQLException ignored) {
            return "0.00";
        }
    }

    /**
     * Format any BigDecimal as "X.YY". Null safe.
     */
    public static String format(BigDecimal v) {
        if (v == null) return "0.00";
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Format a primitive double as "X.YY". Used when the source value
     * has already been read as a double (e.g. cached on a POJO field
     * for arithmetic) and we need to emit it. Note the IEEE-754 caveat:
     * if the double went through any subtraction the drift is already
     * baked in; prefer {@link #formatRs(ResultSet, String)} when
     * reading directly from a ResultSet.
     */
    public static String format(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * SUN-1255: render a money column (DECIMAL(20,4) on storage, e.g.
     * rebate_amount, commission_earned) as a 3-decimal string with
     * <em>truncation</em>, never round-up. The agency tester reported
     * 150 × 1.05% = 1.575 displayed as 1.58 — operator policy is to
     * trim, not round, so the player's perceived earnings stay
     * conservative. Trailing precision past 3 decimals is dropped
     * because the player UI can only show 3 decimals reliably.
     *
     * <p>Mathematically: scale=3 + RoundingMode.DOWN. 1.5750 → "1.575",
     * 1.999 → "1.999", 0.0001 → "0.000".
     *
     * <p>Pad to exactly 3 decimals so 1.5 → "1.500" — caller never
     * has to decide between "trim trailing zero" and "fixed width".
     */
    public static String formatMoney(BigDecimal v) {
        if (v == null) return "0.000";
        return v.setScale(3, RoundingMode.DOWN).toPlainString();
    }
}
