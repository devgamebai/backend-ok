package com.sunwinkr.minigame.engine.sicbo.bet;

import com.sunwinkr.minigame.engine.port.BetRecorder;
import com.sunwinkr.minigame.engine.port.MoneyResult;
import com.sunwinkr.minigame.engine.port.TransKind;
import com.sunwinkr.minigame.engine.port.WalletPort;
import com.sunwinkr.minigame.engine.sicbo.core.SicboRound;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * INV-21: transactionCode format and uniqueness.
 *
 * Format: {@code referenceId + "-" + betIndex}
 * — mirrors SBR:413 {@code this.referenceId + "-" + this.betIndex}.
 *
 * All codes must be unique within a single round.
 */
class SicboTransactionCodeInvariantTest {

    /** First bet produces "refId-1", second "refId-2", etc. */
    @Test
    void formatAndUniqueness() {
        long refId = 5001L;
        SicboRound round     = new SicboRound(refId);
        SicboPotState pot    = new SicboPotState();
        SicboTxIdGenerator gen = new SicboTxIdGenerator(refId);
        SicboBetService service = new SicboBetService();
        WalletPort wallet    = new UnlimitedWallet();
        BetRecorder recorder = new BetRecorder() {
            @Override
            public void record(long ref, String nick, long bv, int it, int bs, int mt) {
                /* no-op */
            }
        };

        // Place 10 bets on different sides
        String[] sides = {
            "TAI", "XIU", "CHAN", "LE", "POINT_4", "POINT_5",
            "POINT_6", "ONE_DICE_1", "DOUBLE_DICES_1_2", "TRIPLE_DICES_3"
        };
        for (String side : sides) {
            SicboBetRequest req = new SicboBetRequest(
                "player1", 1, 500L, (short) 10, (short) 1, side, false);
            SicboBetAcceptResult r = service.accept(req, round, gen, pot, wallet, recorder);
            assertThat(r.isSuccess()).as("bet on " + side + " should succeed").isTrue();
        }

        // Collect transaction codes from pot entries
        List<SicboPotEntry> entries = pot.listUserBet();
        assertThat(entries).hasSize(sides.length);

        Set<String> codes = new HashSet<>();
        for (int i = 0; i < entries.size(); i++) {
            String code = entries.get(i).transactionCode;
            String expected = refId + "-" + (i + 1);
            assertThat(code).as("entry[" + i + "] transactionCode").isEqualTo(expected);
            assertThat(codes.add(code)).as("duplicate transactionCode: " + code).isTrue();
        }
    }

    /** Returned SicboBetAcceptResult.transactionCode matches what's stored in pot. */
    @Test
    void resultTransactionCodeMatchesPotEntry() {
        long refId = 7777L;
        SicboRound round     = new SicboRound(refId);
        SicboPotState pot    = new SicboPotState();
        SicboTxIdGenerator gen = new SicboTxIdGenerator(refId);
        SicboBetService service = new SicboBetService();
        WalletPort wallet    = new UnlimitedWallet();
        BetRecorder recorder = new BetRecorder() {
            @Override
            public void record(long ref, String nick, long bv, int it, int bs, int mt) {
                /* no-op */
            }
        };

        SicboBetRequest req = new SicboBetRequest("alice", 1, 200L, (short) 5, (short) 1, "TAI", false);
        SicboBetAcceptResult result = service.accept(req, round, gen, pot, wallet, recorder);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.transactionCode).isEqualTo(refId + "-1");
        assertThat(pot.listUserBet().get(0).transactionCode).isEqualTo(result.transactionCode);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Wallet stub with unlimited balance that always succeeds. */
    private static class UnlimitedWallet implements WalletPort {
        private long balance = Long.MAX_VALUE / 2;

        @Override public long getBalance(String n, String t) { return balance; }

        @Override public MoneyResult debit(String n, long amt, String t, long txId, TransKind k) {
            balance -= amt;
            return MoneyResult.ok(balance);
        }

        @Override public MoneyResult credit(String n, long amt, String t, long txId, TransKind k) {
            balance += amt;
            return MoneyResult.ok(balance);
        }
    }
}
