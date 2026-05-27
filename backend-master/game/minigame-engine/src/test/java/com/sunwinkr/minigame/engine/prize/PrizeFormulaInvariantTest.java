package com.sunwinkr.minigame.engine.prize;

import com.sunwinkr.minigame.engine.bet.PotState;
import com.sunwinkr.minigame.engine.bet.TransactionTaiXiuDetail;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.FloatRange;
import net.jqwik.api.constraints.LongRange;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Spec INV-7: winning prize formula (TXR:865, 913). */
class PrizeFormulaInvariantTest {

    @Property(tries = 200)
    void winningFormula(@ForAll @LongRange(min = 100L, max = 100_000_000L) long bet,
                        @ForAll @FloatRange(min = 0.0f, max = 10.0f) float tax) {
        // INV-7: prize = (long)(bet * (100 - tax) / 100) + bet.
        long expected = (long) ((float) bet * (100.0f - tax) / 100.0f) + bet;
        assertThat(PrizeCalculator.winningPrize(bet, tax)).isEqualTo(expected);
    }

    @Test
    void balanceGateQuirkPreserved() {
        // TODO(SUN-BAL-INV): when balanceGate=true (TXR:861), the
        // winning-side calc bypasses the cross-pot trim AND PAYS THE
        // FULL betValue. Preserve this inverted-looking semantics here.
        PotState potTai = new PotState();
        potTai.addReal(mkTran("winner", 1, 10_000L));
        PotState potXiu = new PotState();
        potXiu.addReal(mkTran("loser", 2, 1_000L));
        // Cross-pot would trim Tai's winner to 1000 — but balanceGate=true
        // skips trim and pays out on full bet.
        RoundSnapshot snap = new RoundSnapshot(
            1L, potTai, potXiu, /*result*/ (short) 1, 5.0f, /*balanceGate*/ true);
        SettleResult rs = PrizeCalculator.calculate(snap);
        // Full bet=10_000, tax=5% → prize = 10_000*(95)/100 + 10_000 = 19_500.
        assertThat(rs.sumTai.get("winner").totalPrize).isEqualTo(19_500L);
        // Refund=0 because tienDuocTinh = full bet.
        assertThat(rs.sumTai.get("winner").totalRefund).isEqualTo(0L);
    }

    @Test
    void losingSideZeroPrizeFullRefund() {
        // Losing-side contributors get refund only (TXR:889/915). With
        // balanceGate=false (default), cross-pot trim applies.
        PotState potTai = new PotState();
        potTai.addReal(mkTran("loser", 1, 10_000L));
        PotState potXiu = new PotState();
        potXiu.addReal(mkTran("winner", 2, 1_000L));
        RoundSnapshot snap = new RoundSnapshot(
            1L, potTai, potXiu, /*result*/ (short) 0, /*tax*/ 5.0f, /*balanceGate*/ false);
        SettleResult rs = PrizeCalculator.calculate(snap);
        // Loser on Tai side: matched 1000, refund 9000, prize 0.
        assertThat(rs.sumTai.get("loser").totalPrize).isEqualTo(0L);
        assertThat(rs.sumTai.get("loser").totalRefund).isEqualTo(9_000L);
        // Winner on Xiu side: matched 1000, prize = 1000*95/100 + 1000 = 1950.
        assertThat(rs.sumXiu.get("winner").totalPrize).isEqualTo(1_950L);
    }

    private static TransactionTaiXiuDetail mkTran(String user, int id, long value) {
        return new TransactionTaiXiuDetail(
            1L, id, user, value, 1, 10, 1, value * 100, 1_000_000L + id);
    }
}
