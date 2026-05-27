package com.sunwinkr.lottery.engine.bet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Bet validation tests — exercises shape, min-bet, mode whitelist.
 *
 * <p>Per {@code docs/plans/lottery-extraction-plan.md §2.3 B2} every
 * mode has a fixed input shape. Validator rejects everything else with
 * {@code 0005 invalid_number} (or {@code 0004 unknown_mode} for mode
 * range failures).
 */
class BetValidatorTest {

    /**
     * Per-mode happy-path shape — parameterised covers all 10 modes.
     */
    @ParameterizedTest
    @CsvSource({
            // SUN-1366 — Bao lô 2 số (1) 2-digit; max 300 điểm per number.
            "1, 42, 1, 300",
            // Bao lô 3 số (2) 3-digit; no limit.
            "2, 123, 1, 10000",
            // Xiên 2/3/4 — CSV; no limit.
            "3, '11,22', 2, 10000",
            "4, '11,22,33', 3, 10000",
            "5, '11,22,33,44', 4, 10000",
            // Đề Giải Nhất (6) / Đề Đặc Biệt (7) — 2-digit; max 500_000 điểm.
            "6, 55, 1, 500000",
            "7, 42, 1, 500000",
            // 3 Càng Đặc Biệt (8) — 3-digit; no limit.
            "8, 342, 1, 10000",
            // SUN-1342 Lô Trượt Xiên 10/12/14 — CSV; no limit.
            "9,  '01,02,03,04,05,06,07,08,09,10', 10, 10000",
            "10, '01,02,03,04,05,06,07,08,09,10,11,12', 12, 10000",
            "11, '01,02,03,04,05,06,07,08,09,10,11,12,13,14', 14, 10000"
    })
    void modeShapes(int modeId, String ticket, int expectedPicks, long betValue) {
        BetRequest req = new BetRequest("user1", 100L, modeId, ticket, betValue, "n1");
        List<String> picks = BetValidator.validate(req);
        assertThat(picks).hasSize(expectedPicks);
    }

    @Test
    void mode1ExceedsCap_Rejected_0006() {
        // SUN-1366: Bao lô 2 số per-number cap = 300 điểm.
        BetRequest req = new BetRequest("u", 1L, 1, "42", 1000L, "n");
        assertThatThrownBy(() -> BetValidator.validate(req))
                .isInstanceOf(BetValidator.InvalidBetException.class)
                .extracting(e -> ((BetValidator.InvalidBetException) e).getErrorCode())
                .isEqualTo("0006");
    }

    @Test
    void modeDeExceedsCap_Rejected_0006() {
        // SUN-1366: Đề (modes 6/7) per-number cap = 500_000 điểm.
        BetRequest req = new BetRequest("u", 1L, 7, "42", 600_000L, "n");
        assertThatThrownBy(() -> BetValidator.validate(req))
                .isInstanceOf(BetValidator.InvalidBetException.class)
                .extracting(e -> ((BetValidator.InvalidBetException) e).getErrorCode())
                .isEqualTo("0006");
    }

    @Test
    void invalidModeRejected() {
        BetRequest req = new BetRequest("user1", 100L, 42, "42", 10_000L, "n1");
        assertThatThrownBy(() -> BetValidator.validate(req))
                .isInstanceOf(BetValidator.InvalidBetException.class)
                .extracting(e -> ((BetValidator.InvalidBetException) e).getErrorCode())
                .isEqualTo("0004");
    }

    @Test
    void belowMinRejected() {
        // BetValidator.MIN_BET = 1 (SUN-1366). 0 → 0005.
        BetRequest req = new BetRequest("user1", 100L, 1, "42", 0L, "n1");
        assertThatThrownBy(() -> BetValidator.validate(req))
                .isInstanceOf(BetValidator.InvalidBetException.class);
    }

    @Test
    void modeShapeMismatch() {
        // mode 1 wants 2-digit single; "1,2" is csv → reject
        BetRequest req = new BetRequest("u", 1L, 1, "1,2", 10_000L, "n");
        assertThatThrownBy(() -> BetValidator.validate(req))
                .isInstanceOf(BetValidator.InvalidBetException.class)
                .extracting(e -> ((BetValidator.InvalidBetException) e).getErrorCode())
                .isEqualTo("0005");
    }

    @Test
    void emptyTicketRejected() {
        BetRequest req = new BetRequest("u", 1L, 1, "", 10_000L, "n");
        assertThatThrownBy(() -> BetValidator.validate(req))
                .isInstanceOf(BetValidator.InvalidBetException.class);
    }

    @Test
    void nonDigitRejected() {
        BetRequest req = new BetRequest("u", 1L, 1, "ab", 10_000L, "n");
        assertThatThrownBy(() -> BetValidator.validate(req))
                .isInstanceOf(BetValidator.InvalidBetException.class);
    }

    @Test
    void csvDuplicateRejected() {
        // mode 3 picks must be distinct — duplicate in csv → 0005
        BetRequest req = new BetRequest("u", 1L, 3, "11,11", 10_000L, "n");
        assertThatThrownBy(() -> BetValidator.validate(req))
                .isInstanceOf(BetValidator.InvalidBetException.class);
    }
}
