package com.sunwinkr.lottery.engine.bet;

import com.sunwinkr.lottery.engine.model.LotteryMode;

import java.util.List;

/**
 * Composite bet validator — runs every server-side check needed before
 * the wallet debit. No I/O, no state — pure function over
 * {@link BetRequest}.
 *
 * <p>Checks in order:
 * <ol>
 *   <li>{@code modeId} resolves via
 *       {@link LotteryMode#byId(int)} → {@code 0004 unknown_mode} on miss</li>
 *   <li>{@code betValue} ≥ {@link #MIN_BET} → throws on under-min</li>
 *   <li>{@link TicketNumberParser#parse} → {@code 0005 invalid_number}
 *       on shape failure</li>
 * </ol>
 *
 * <p>Legacy {@code LotteryModule.buyTicket} only checked {@code num !=
 * empty} — it had no min-bet floor. We add a {@link #MIN_BET}=1000 floor
 * to match the rest of the casino product family (TaiXiu/Sicbo all clamp
 * ≥ 1000) and document the contract in
 * {@code docs/plans/lottery-extraction-plan.md §2.3 B2}. Behaviour
 * change: requested by SUN-LOTTERY-MIN-BET.
 */
public final class BetValidator {

    /**
     * Minimum accepted bet (in đơn vị / điểm, NOT vin). SUN-1366 set the
     * Bao lô 2 số per-number cap at 300 điểm — the previous MIN_BET=1000
     * blocked every valid mode-1 bet. The wire DTO already enforces
     * {@code @Min(1)} on betValue so this floor is mostly defensive.
     */
    public static final long MIN_BET = 1L;

    /**
     * SUN-1366 — per-number maximum bet (in điểm/units, NOT vin).
     *
     * <p>Limits operate on the user-input bet value (đơn vị / unit count)
     * before the per-mode rate is applied. Example: Bao lô 2 số with
     * limit 300 means the player can stake at most 300 units per number;
     * at rate 22 that's 6,600 vin per number.
     *
     * <p>Modes not listed are unlimited (Bao lô 3 số = 2, 3 Càng = 8,
     * Xiên 2/3/4 = 3/4/5, Lô Trượt Xiên 10/12/14 = 9/10/11).
     */
    public static long maxBetForMode(int modeId) {
        switch (modeId) {
            case 1: return 300L;       // Bao lô 2 số: 300 điểm mỗi số
            case 6: return 500_000L;   // Đề Giải Nhất: 500k điểm mỗi số
            case 7: return 500_000L;   // Đề Đặc Biệt:  500k điểm mỗi số
            default: return Long.MAX_VALUE;
        }
    }

    private BetValidator() {
        // utility
    }

    /**
     * Run all validation steps. Throws {@link InvalidBetException} on
     * the first failure — the caller maps {@code .errorCode} to the
     * outbound {@link BetAcceptResult}.
     *
     * @return the parsed ticket numbers (per-pick list)
     */
    public static List<String> validate(BetRequest req) {
        // (1) mode must exist
        LotteryMode mode = LotteryMode.byId(req.getModeId()).orElse(null);
        if (mode == null) {
            throw new InvalidBetException("0004", "unknown mode " + req.getModeId());
        }
        // (2) min bet floor
        if (req.getBetValue() < MIN_BET) {
            throw new InvalidBetException("0005",
                    "bet below minimum " + MIN_BET + ": " + req.getBetValue());
        }
        // (3) ticket number shape per mode — checked before cap so shape errors
        // still return 0005 and only the cap returns the new 0006.
        List<String> picks;
        try {
            picks = TicketNumberParser.parse(req.getModeId(), req.getTicket());
        } catch (IllegalArgumentException e) {
            throw new InvalidBetException("0005", e.getMessage());
        }
        // (4) SUN-1366 — per-mode max bet per number (điểm/unit, pre-rate).
        long maxBet = maxBetForMode(req.getModeId());
        if (req.getBetValue() > maxBet) {
            throw new InvalidBetException("0006",
                    "bet exceeds per-number cap for mode " + req.getModeId()
                            + " (max=" + maxBet + ", got=" + req.getBetValue() + ")");
        }
        return picks;
    }

    /**
     * Thrown by {@link #validate} on any rejection. {@code errorCode}
     * matches the {@link BetAcceptResult} factory — caller pivots on it.
     */
    public static final class InvalidBetException extends RuntimeException {
        private final String errorCode;

        InvalidBetException(String errorCode, String msg) {
            super(msg);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }
}
