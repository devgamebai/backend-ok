package com.sunwinkr.lottery.engine.bet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Pure function: validate-and-split a raw ticket number string per
 * {@link com.sunwinkr.lottery.engine.model.LotteryMode mode}.
 *
 * <p>Per-mode shapes (per
 * {@code docs/specs/lottery-rules-spec.md §3.1}):
 * <ul>
 *   <li>Modes 1, 8, 9 — single 2-digit</li>
 *   <li>Modes 2, 11 — single 3-digit</li>
 *   <li>Mode 3 — CSV of 2 distinct numbers</li>
 *   <li>Mode 4 — CSV of 3 distinct numbers</li>
 *   <li>Mode 5 — CSV of 4 distinct numbers</li>
 *   <li>Modes 6, 7 — single 1-digit</li>
 * </ul>
 *
 * <p>The legacy {@code LotteryModule.buyTicket} did NO shape validation
 * past {@code TextUtils.isEmpty(num)} — any garbage made it to the
 * settle loop, which then quietly paid zero or NPE'd at a length check
 * (e.g. {@code de.substring(0,1)} on a 1-char input).
 *
 * <p>This parser closes that hole — throws
 * {@link IllegalArgumentException} on invalid shape. The caller
 * ({@link BetValidator}) maps the exception to error code {@code 0005}.
 */
public final class TicketNumberParser {

    private TicketNumberParser() {
        // utility
    }

    /**
     * Parse and validate a raw ticket string.
     *
     * @param modeId  numeric mode id (1..9 or 11)
     * @param raw     raw client-side ticket number string
     * @return per-pick list of numbers (already split for CSV modes)
     * @throws IllegalArgumentException if shape is invalid for {@code modeId}
     */
    public static List<String> parse(int modeId, String raw) {
        if (raw == null || raw.isEmpty()) {
            throw new IllegalArgumentException("ticket required");
        }
        // SUN-1366 + SUN-1342 — per-mode ticket shape:
        //   1  Bao lô 2 số:      single 2-digit
        //   2  Bao lô 3 số:      single 3-digit
        //   3  Xiên 2:           csv 2 × 2-digit
        //   4  Xiên 3:           csv 3 × 2-digit
        //   5  Xiên 4:           csv 4 × 2-digit
        //   6  Đề Giải Nhất:     single 2-digit
        //   7  Đề Đặc Biệt:      single 2-digit
        //   8  3 Càng Đặc Biệt:  single 3-digit
        //   9  Lô Trượt Xiên 10: csv 10 × 2-digit  (settle logic TBD — SUN-1342 phase 2)
        //   10 Lô Trượt Xiên 12: csv 12 × 2-digit  (settle logic TBD — SUN-1342 phase 2)
        //   11 Lô Trượt Xiên 14: csv 14 × 2-digit  (settle logic TBD — SUN-1342 phase 2)
        switch (modeId) {
            // SUN-LOTTERY-MULTIPICK (2026-05-18): single-number modes also
            // accept comma-separated picks. Each pick is an independent
            // bet at the per-pick cap; total stake = userBet × rate ×
            // picks.size(). Settle iterates picks (see
            // {@link com.sunwinkr.lottery.engine.prize.PrizeCalculator}).
            // Xiên modes (3/4/5/9/10/11) stay fixed-count — semantics
            // differ (all-N-must-hit vs per-pick independent).
            case 1:  return singleOrCsv(raw, 2);
            case 2:  return singleOrCsv(raw, 3);
            case 3:  return csv(raw, 2, 2);
            case 4:  return csv(raw, 3, 2);
            case 5:  return csv(raw, 4, 2);
            case 6:  return singleOrCsv(raw, 2);
            case 7:  return singleOrCsv(raw, 2);
            case 8:  return singleOrCsv(raw, 3);
            case 9:  return csv(raw, 10, 2);
            case 10: return csv(raw, 12, 2);
            case 11: return csv(raw, 14, 2);
            default:
                throw new IllegalArgumentException("mode " + modeId + " unknown");
        }
    }

    /** Hard cap on per-bet pick count for the single-or-csv modes. */
    private static final int MAX_PICKS_PER_BET = 100;

    /**
     * Accept either a single N-digit number or a list of distinct
     * N-digit numbers separated by {@code ,} or {@code -}. Capped at
     * {@link #MAX_PICKS_PER_BET} picks so a malicious client cannot push
     * a 10k-element CSV through.
     */
    private static List<String> singleOrCsv(String raw, int digits) {
        if (raw.indexOf(',') < 0 && raw.indexOf('-') < 0) {
            return single(raw, digits);
        }
        String[] parts = raw.split(CSV_SEPARATORS);
        if (parts.length > MAX_PICKS_PER_BET) {
            throw new IllegalArgumentException(
                    "too many picks (max " + MAX_PICKS_PER_BET + "): " + parts.length);
        }
        ArrayList<String> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            String t = p.trim();
            if (t.length() != digits) {
                throw new IllegalArgumentException(
                        "each pick must be " + digits + "-digit, got '" + t + "'");
            }
            for (int i = 0; i < t.length(); i++) {
                char c = t.charAt(i);
                if (c < '0' || c > '9') {
                    throw new IllegalArgumentException(
                            "pick must be all digits, got '" + t + "'");
                }
            }
            if (out.contains(t)) {
                throw new IllegalArgumentException("duplicate pick: '" + t + "'");
            }
            out.add(t);
        }
        if (out.isEmpty()) {
            throw new IllegalArgumentException("ticket required");
        }
        return Collections.unmodifiableList(out);
    }

    /** Validate a single N-digit numeric string and return a singleton list. */
    private static List<String> single(String raw, int digits) {
        if (raw.length() != digits) {
            throw new IllegalArgumentException(
                    "ticket must be " + digits + "-digit, got '" + raw + "'");
        }
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException(
                        "ticket must be all digits, got '" + raw + "'");
            }
        }
        return Collections.singletonList(raw);
    }

    /** Separator regex — accepts both {@code ,} and {@code -} per FE convention. */
    static final String CSV_SEPARATORS = "[,\\-]";

    /**
     * Validate a CSV of {@code expectedCount} N-digit numeric strings,
     * deduplicated. Order preserved. Accepts both {@code ,} and {@code -}
     * as separators — FE (Cocos client, Zeus 2026-05-18) sends Xiên
     * picks as {@code 14-15-22} while admin tooling historically used
     * {@code 14,15,22}.
     */
    private static List<String> csv(String raw, int expectedCount, int digits) {
        String[] parts = raw.split(CSV_SEPARATORS);
        if (parts.length != expectedCount) {
            throw new IllegalArgumentException(
                    "expected " + expectedCount + " numbers separated by ',' or '-', got "
                            + parts.length + " in '" + raw + "'");
        }
        ArrayList<String> out = new ArrayList<>(expectedCount);
        for (String p : parts) {
            String t = p.trim();
            if (t.length() != digits) {
                throw new IllegalArgumentException(
                        "each ticket must be " + digits + "-digit, got '" + t + "'");
            }
            for (int i = 0; i < t.length(); i++) {
                char c = t.charAt(i);
                if (c < '0' || c > '9') {
                    throw new IllegalArgumentException(
                            "ticket must be all digits, got '" + t + "'");
                }
            }
            if (out.contains(t)) {
                throw new IllegalArgumentException(
                        "duplicate number in csv: '" + t + "'");
            }
            out.add(t);
        }
        return Collections.unmodifiableList(new ArrayList<>(Arrays.asList(out.toArray(new String[0]))));
    }
}
