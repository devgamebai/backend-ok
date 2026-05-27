package com.sunwinkr.lottery.engine.prize;

import com.sunwinkr.lottery.engine.model.LotteryResult;
import com.sunwinkr.lottery.engine.model.LotteryTicket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Pure prize calculation — 10 modes, ported from
 * {@code LotteryModule.computePrize} (JLM:284-369).
 *
 * <p>All methods are static. The class holds no state — the same input
 * always produces the same output (the testable property is
 * {@code INV-LOTTERY-06} mode 1 closed form, exercised by jqwik).
 *
 * <p><b>SUN-1295 snapshot semantics.</b> The {@code rateAtPurchase} +
 * {@code prizeMul} inputs MUST be read from the {@link LotteryTicket}'s
 * stamped columns ({@code rate_at_purchase} + {@code prize_multiplier}),
 * NEVER from the live {@code LotteryMode} enum at settle time. The
 * dispatcher {@link #calculate(LotteryResult, LotteryTicket)} threads
 * the stored snapshot through.
 *
 * <p><b>Mode 5 canonical rule = 4/4 (C# parity).</b> Per binding decision
 * D2 in the extraction plan, Mode 5 (Lô Xiên 4) now requires ALL 4
 * picks to match — not 3/4 as the legacy Java decompile did
 * ({@code LotteryModule.computePrize} case 5 used {@code isVictory5 < 3}).
 * Env flag {@code LOTTERY_MODE5_LEGACY_3OF4=1} restores the 3/4 rule for
 * one-release rollback. See
 * {@code TODO(SUN-MODE5-RECONCILE)} on {@link #mode5}.
 *
 * <p><b>Mode 8 legacy divisor (AMBIGUOUS #9).</b> The legacy
 * {@code LotteryModule.computePrize} case 8 does
 * {@code prize = betValue * prizeMul / DUOI.getRate()} where
 * {@code DUOI.getRate()==1}. That's a no-op today but preserved
 * byte-exact in {@link #mode8} for the shadow-replay byte-for-byte
 * regression harness.
 *
 * <p><b>Mode 9, Mode 11 SUN-1295 multipliers.</b> Mode 9 prizeMul lowered
 * 95→85; Mode 11 lowered 900→450. These changes ride on the snapshot —
 * tickets purchased before SUN-1295 settle at their stored snapshot
 * value (e.g. 95) while tickets purchased after settle at 85.
 */
public final class PrizeCalculator {

    /** Env flag name — when set to {@code "1"}, Mode 5 reverts to legacy 3/4 rule. */
    public static final String LEGACY_MODE5_3OF4_ENV = "LOTTERY_MODE5_LEGACY_3OF4";

    private PrizeCalculator() {
        // utility
    }

    /** @return {@code true} iff the env flag overrides Mode 5 back to 3/4. */
    public static boolean isMode5LegacyThreeOfFour() {
        String v = System.getenv(LEGACY_MODE5_3OF4_ENV);
        return "1".equals(v) || "true".equalsIgnoreCase(v);
    }

    // ---------- Dispatcher ----------

    /**
     * Dispatch by {@code ticket.modeId}. Returns
     * {@link PrizeBreakdown#ZERO} for unknown modes — the bet validator
     * is supposed to have rejected those at purchase, but settle stays
     * defensive (legacy code silently fell through the switch).
     */
    public static PrizeBreakdown calculate(LotteryResult rs, LotteryTicket ticket) {
        if (rs == null || rs.getResults() == null || ticket == null) {
            return PrizeBreakdown.ZERO;
        }
        List<String> rs27 = rs.getResults().get27();
        List<String> rs24 = rs.getResults().get24();
        List<String> db = rs.getResults().getĐB();
        List<String> g1 = rs.getResults().getG1();

        int rate = ticket.getRateAtPurchase();
        int prizeMul = ticket.getPrizeMultiplier();
        long userBet = ticket.getBetValue() / Math.max(rate, 1); // restore userBet from stored finalBetValue
        long storedBet = ticket.getBetValue(); // = userBet * rate (closed-form denom cancels)
        String num = ticket.getTicket();

        try {
            switch (ticket.getModeId()) {
                case 1:
                    return mode1(rs27, num, userBet, prizeMul);
                case 2:
                    return mode2(rs24, num, userBet, prizeMul);
                case 3:
                    return mode3(rs27, num, storedBet, prizeMul);
                case 4:
                    return mode4(rs27, num, storedBet, prizeMul);
                case 5:
                    return mode5(rs27, num, storedBet, prizeMul, isMode5LegacyThreeOfFour());
                case 6:
                    // SUN-1366 — Đề Giải Nhất: last 2 digits of G1 == picked 2-digit num.
                    return mode6(safeFirst(g1), num, storedBet, prizeMul);
                case 7:
                    // SUN-1366 — Đề Đặc Biệt: last 2 digits of ĐB == picked 2-digit num.
                    return mode7(safeFirst(db), num, storedBet, prizeMul);
                case 8:
                    // SUN-1366 — 3 Càng Đặc Biệt: last 3 digits of ĐB only == picked 3-digit num.
                    return mode8(safeFirst(db), num, storedBet, prizeMul);
                case 9:
                    return mode9(safeFirst(db), num, storedBet, prizeMul);
                case 11:
                    return mode11(db, num, storedBet, prizeMul);
                default:
                    return PrizeBreakdown.ZERO;
            }
        } catch (RuntimeException e) {
            // Mirror legacy catch-all in LotteryModule.computePrize (JLM:366-368)
            // — any malformed payload yields zero prize, not a partial credit.
            return PrizeBreakdown.ZERO;
        }
    }

    // ---------- Per-mode pure functions ----------

    /**
     * Mode 1 — Lô 2 Số. Per-match across the 27-line prize pool.
     * Closed form: {@code prize = matches * userBet * 80}.
     * INV-LOTTERY-06 exercises this.
     */
    public static PrizeBreakdown mode1(List<String> rs27, String num, long userBet, int prizeMul) {
        int matches = countEndsWith(rs27, num);
        if (matches <= 0) return PrizeBreakdown.ZERO;
        long prize = (long) matches * userBet * (long) prizeMul;
        return new PrizeBreakdown(prize, matches, 0L);
    }

    /**
     * Mode 2 — Lô 3 Số. Per-match across the 24-line prize pool (excl G7).
     * Closed form: {@code prize = matches * userBet * prizeMul}.
     */
    public static PrizeBreakdown mode2(List<String> rs24, String num, long userBet, int prizeMul) {
        int matches = countEndsWith(rs24, num);
        if (matches <= 0) return PrizeBreakdown.ZERO;
        long prize = (long) matches * userBet * (long) prizeMul;
        return new PrizeBreakdown(prize, matches, 0L);
    }

    /**
     * Mode 3 — Lô Xiên 2. Flat payout iff BOTH picks match. Per legacy
     * (case 3), {@code prize = betValue * prizeMul} — {@code betValue} is
     * the stored (already rate-multiplied) value.
     */
    public static PrizeBreakdown mode3(List<String> rs27, String numCsv, long storedBet, int prizeMul) {
        List<String> nums = splitCsv(numCsv);
        int hits = countCsvHits(rs27, nums);
        if (hits < 2) return PrizeBreakdown.ZERO;
        return new PrizeBreakdown(storedBet * (long) prizeMul, hits, 0L);
    }

    /**
     * Mode 4 — Lô Xiên 3. Flat payout iff ALL 3 picks match.
     */
    public static PrizeBreakdown mode4(List<String> rs27, String numCsv, long storedBet, int prizeMul) {
        List<String> nums = splitCsv(numCsv);
        int hits = countCsvHits(rs27, nums);
        if (hits < 3) return PrizeBreakdown.ZERO;
        return new PrizeBreakdown(storedBet * (long) prizeMul, hits, 0L);
    }

    /**
     * Mode 5 — Lô Xiên 4. <b>CANONICAL = 4/4</b> (matches C# behaviour).
     *
     * <p>TODO(SUN-MODE5-RECONCILE): default canonical 4/4 since
     * 2026-05-14; legacy 3/4 path retained for rollback. Drop the
     * {@code legacy3of4} param after 2 weeks of healthy production.
     *
     * @param legacy3of4  if {@code true}, restores legacy Java
     *                    {@code isVictory5 < 3} rule (3/4 wins)
     */
    public static PrizeBreakdown mode5(List<String> rs27, String numCsv, long storedBet, int prizeMul,
                                       boolean legacy3of4) {
        List<String> nums = splitCsv(numCsv);
        int hits = countCsvHits(rs27, nums);
        int threshold = legacy3of4 ? 3 : 4;
        if (hits < threshold) return PrizeBreakdown.ZERO;
        return new PrizeBreakdown(storedBet * (long) prizeMul, hits, 0L);
    }

    /**
     * Mode 6 — Đề Giải Nhất (SUN-1366). Player picks a 2-digit number; wins
     * iff last 2 digits of the Giải Nhất prize equal the pick.
     */
    public static PrizeBreakdown mode6(String g1, String num, long storedBet, int prizeMul) {
        if (g1 == null || num == null || g1.length() < 2 || num.length() != 2) return PrizeBreakdown.ZERO;
        if (!g1.substring(g1.length() - 2).equals(num)) return PrizeBreakdown.ZERO;
        return new PrizeBreakdown(storedBet * (long) prizeMul, 1, 0L);
    }

    /**
     * Mode 7 — Đề Đặc Biệt (SUN-1366). Player picks a 2-digit number; wins
     * iff last 2 digits of the ĐB (Đặc Biệt) prize equal the pick.
     */
    public static PrizeBreakdown mode7(String db, String num, long storedBet, int prizeMul) {
        if (db == null || num == null || db.length() < 2 || num.length() != 2) return PrizeBreakdown.ZERO;
        if (!db.substring(db.length() - 2).equals(num)) return PrizeBreakdown.ZERO;
        return new PrizeBreakdown(storedBet * (long) prizeMul, 1, 0L);
    }

    /**
     * Mode 8 — 3 Càng Đặc Biệt (SUN-1366). Player picks a 3-digit number; wins
     * iff last 3 digits of the ĐB prize equal the pick. ĐB-only — not other
     * prize lines.
     */
    public static PrizeBreakdown mode8(String db, String num, long storedBet, int prizeMul) {
        if (db == null || num == null || db.length() < 3 || num.length() != 3) return PrizeBreakdown.ZERO;
        if (!db.substring(db.length() - 3).equals(num)) return PrizeBreakdown.ZERO;
        return new PrizeBreakdown(storedBet * (long) prizeMul, 1, 0L);
    }

    /**
     * Mode 9 — Đề Đặc Biệt. {@code de == num} (last 2 chars of pick).
     *
     * <p>SUN-1295 lowered the multiplier to 85 (from 95) — but the value
     * here flows from the ticket snapshot, so legacy rows settle at
     * their stored multiplier (95) while post-fix rows settle at 85.
     */
    public static PrizeBreakdown mode9(String db, String num, long storedBet, int prizeMul) {
        if (db == null || db.length() < 2 || num == null || num.length() < 2) {
            return PrizeBreakdown.ZERO;
        }
        String de = db.substring(db.length() - 2);
        if (!de.equals(num.substring(0, 2))) return PrizeBreakdown.ZERO;
        return new PrizeBreakdown(storedBet * (long) prizeMul, 1, 0L);
    }

    /**
     * Mode 11 — Ba Càng. Win iff any ĐB line ends with the 3-digit num.
     * SUN-1295 lowered multiplier 900→450 (via snapshot).
     */
    public static PrizeBreakdown mode11(List<String> db, String num, long storedBet, int prizeMul) {
        if (!anyEndsWith(db, num)) return PrizeBreakdown.ZERO;
        return new PrizeBreakdown(storedBet * (long) prizeMul, 1, 0L);
    }

    // ---------- Helpers ----------

    private static int countEndsWith(List<String> pool, String num) {
        if (pool == null || num == null || num.isEmpty()) return 0;
        int c = 0;
        for (String s : pool) {
            if (s != null && s.endsWith(num)) c++;
        }
        return c;
    }

    private static boolean anyEndsWith(List<String> pool, String num) {
        if (pool == null || num == null || num.isEmpty()) return false;
        for (String s : pool) {
            if (s != null && s.endsWith(num)) return true;
        }
        return false;
    }

    private static List<String> splitCsv(String csv) {
        if (csv == null) return new ArrayList<>();
        // FE Cocos sends Xiên picks as "14-15-22" while admin tooling and
        // legacy rows use "14,15,22" — TicketNumberParser accepts both
        // separators and the stored ticket column preserves the original
        // form, so settle must also split on both.
        String[] parts = csv.split("[,\\-]");
        List<String> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    /**
     * Count how many of {@code picks} have ANY line in {@code pool}
     * ending with that pick. This is the per-pick membership-style
     * count used by Modes 3/4/5 (legacy {@code anyMatch} stream).
     */
    private static int countCsvHits(List<String> pool, List<String> picks) {
        int hits = 0;
        for (String pick : picks) {
            if (anyEndsWith(pool, pick)) hits++;
        }
        return hits;
    }

    private static String safeFirst(List<String> xs) {
        return (xs == null || xs.isEmpty()) ? null : xs.get(0);
    }

    /** Bridge for tests that want to mass-construct rs27 from G-lists. */
    public static List<String> concat(List<String>... lists) {
        List<String> out = new ArrayList<>();
        for (List<String> l : lists) {
            if (l != null) out.addAll(l);
        }
        return out;
    }

    /** Tiny utility used by tests to compose a draw without Gson. */
    public static List<String> asList(String... xs) {
        return new ArrayList<>(Arrays.asList(xs));
    }
}
