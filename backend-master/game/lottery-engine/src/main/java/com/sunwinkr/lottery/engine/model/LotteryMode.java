package com.sunwinkr.lottery.engine.model;

import java.util.Optional;

/**
 * Lottery bet mode catalog — 10 modes (ids 1..9 + 11). Ported from
 * {@code game.modules.minigame.model.LotteryMode} (BitZero Minigame).
 *
 * <p><b>SUN-1295 snapshot semantics.</b> Both {@link #getRate()} and
 * {@link #getPrizeMultiplier()} are stamped onto the {@code lode} row at
 * purchase time so a future rate change cannot retroactively alter
 * pending bets. The settle path MUST read from the stored row's snapshot,
 * not from this enum. This enum is the source of truth at PURCHASE TIME
 * ONLY.
 *
 * <p><b>Anti-cheat hardening (audit §5 — LotteryMode setters).</b> The
 * decompiled legacy enum exposed public {@code setRate} /
 * {@code setPrizeMultiplier} setters that would mutate a JVM-singleton
 * static. They had no production callers but were a latent leak surface.
 * <b>They are deliberately REMOVED here.</b> Static integrity is now
 * structural. The {@code LotteryModeTest.settersRemoved} reflection
 * check enforces this in CI.
 *
 * <p>Constants verified against
 * {@code docs/specs/lottery-rules-spec.md §3.1} — SUN-1295 lowered
 * mode 9 prizeMul to 85 (was 95) and mode 11 to 450 (was 900).
 */
public enum LotteryMode {

    /** Bao lô 2 số — per-match across 27 prize lines. */
    BAO_LO_2_SO(1, "BAO LÔ 2 SỐ", "Chọn 1 số 2 chữ số", 22, 80),

    /** Bao lô 3 số — per-match across 23 prize lines (excludes G7). */
    BAO_LO_3_SO(2, "BAO LÔ 3 SỐ", "Chọn 1 số 3 chữ số", 23, 600),

    /** Xiên 2 — flat payout if 2/2 match across the 27 prize lines. */
    XIEN_2(3, "XIÊN 2", "Chọn 2 số 2 chữ số", 1, 12),

    /** Xiên 3 — flat payout if 3/3 match. */
    XIEN_3(4, "XIÊN 3", "Chọn 3 số 2 chữ số", 1, 48),

    /**
     * Xiên 4 — flat payout. Canonical rule = 4/4 (C# parity).
     * Legacy Java was 3/4 — feature flag {@code LOTTERY_MODE5_LEGACY_3OF4}
     * preserves the legacy rule for 1-release rollback. Wired in PR-2.
     */
    XIEN_4(5, "XIÊN 4", "Chọn 4 số 2 chữ số", 1, 160),

    /** Đề Giải Nhất — last 2 digits of Giải Nhất prize. */
    DE_GIAI_NHAT(6, "ĐỀ GIẢI NHẤT", "Chọn 1 số 2 chữ số (G1)", 1, 85),

    /** Đề Đặc Biệt — last 2 digits of ĐB. */
    DE_DAC_BIET(7, "ĐỀ ĐẶC BIỆT", "Chọn 1 số 2 chữ số (ĐB)", 1, 85),

    /** 3 Càng Đặc Biệt — last 3 digits of ĐB. */
    BA_CANG_DAC_BIET(8, "3 CÀNG ĐẶC BIỆT", "Chọn 1 số 3 chữ số (ĐB)", 1, 450),

    /**
     * Lô trượt xiên 10 — player picks 10 distinct 2-digit numbers; wins
     * if NONE appears across the 27 prizes (all 10 "trượt"/slip past).
     * Bet ticket format: 10 comma-separated 2-digit numbers (e.g. "01,02,...,10").
     */
    LO_TRUOT_XIEN_10(9, "LÔ TRƯỢT XIÊN 10", "Chọn 10 số 2 chữ số", 1, 10),

    /** Lô trượt xiên 12 — same as 10 but with 12 numbers. */
    LO_TRUOT_XIEN_12(10, "LÔ TRƯỢT XIÊN 12", "Chọn 12 số 2 chữ số", 1, 16),

    /** Lô trượt xiên 14 — same as 10 but with 14 numbers. */
    LO_TRUOT_XIEN_14(11, "LÔ TRƯỢT XIÊN 14", "Chọn 14 số 2 chữ số", 1, 20);

    private final int id;
    private final String name;
    private final String description;
    private final int rate;
    private final int prizeMultiplier;

    LotteryMode(int id, String name, String description, int rate, int prizeMultiplier) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.rate = rate;
        this.prizeMultiplier = prizeMultiplier;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * @return bet-cost multiplier. Snapshot semantics: read from
     *         {@code lode.rate_at_purchase} during settle, NEVER from this
     *         enum (SUN-1295).
     */
    public int getRate() {
        return rate;
    }

    /**
     * @return per-match payout multiplier. Snapshot semantics: read from
     *         {@code lode.prize_multiplier} during settle, NEVER from this
     *         enum (SUN-1295).
     */
    public int getPrizeMultiplier() {
        return prizeMultiplier;
    }

    /**
     * Look up a mode by stable id. Returns empty for unknown ids — callers
     * MUST handle this (legacy {@code findLotteryModeById} returned null
     * and exploded at the use site).
     *
     * @param id stable mode id (1..9 or 11)
     * @return matching mode, or {@link Optional#empty()} for unknown
     */
    public static Optional<LotteryMode> byId(int id) {
        for (LotteryMode m : values()) {
            if (m.id == id) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }
}
