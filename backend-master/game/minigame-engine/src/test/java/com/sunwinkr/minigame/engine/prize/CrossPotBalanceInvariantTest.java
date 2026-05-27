package com.sunwinkr.minigame.engine.prize;

import com.sunwinkr.minigame.engine.bet.PotState;
import com.sunwinkr.minigame.engine.bet.TransactionTaiXiuDetail;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.constraints.Size;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Spec INV-5 + INV-6: cross-pot balance invariants. */
class CrossPotBalanceInvariantTest {

    @Property(tries = 200)
    void hopLeMatchesMinPot(@ForAll("smallBets") List<Long> taiBets,
                            @ForAll("smallBets") List<Long> xiuBets) {
        // INV-5: tongTienHopLe = min(potTai.total, potXiu.total).
        PotState potTai = mkPot(taiBets, "tai");
        PotState potXiu = mkPot(xiuBets, "xiu");
        long hopLe = CrossPotBalancer.legalAmount(potTai, potXiu);
        assertThat(hopLe).isEqualTo(Math.min(potTai.totalValue(), potXiu.totalValue()));
    }

    @Test
    void refundCompleteness() {
        // INV-6: every contributor → refund + tienDuocTinh == betValue.
        PotState pot = new PotState();
        pot.addReal(mkTran("u1", 1, 300L));
        pot.addReal(mkTran("u2", 2, 700L));
        pot.addReal(mkTran("u3", 3, 500L));
        // Cap at 1000 — last contributor partially refunded.
        long hopLe = 1000L;
        List<CrossPotBalancer.Allocation> allocs = CrossPotBalancer.allocate(pot, hopLe);
        assertThat(allocs).hasSize(3);
        for (CrossPotBalancer.Allocation a : allocs) {
            assertThat(a.tienDuocTinh + a.refund)
                .as("u=%s", a.tran.username)
                .isEqualTo(a.tran.betValue);
        }
        // Sum tienDuocTinh = hopLe.
        long sum = 0L;
        for (CrossPotBalancer.Allocation a : allocs) {
            sum += a.tienDuocTinh;
        }
        assertThat(sum).isEqualTo(hopLe);
    }

    @Test
    void emptyPotProducesEmptyAllocations() {
        PotState empty = new PotState();
        assertThat(CrossPotBalancer.allocate(empty, 0L)).isEmpty();
    }

    @Provide
    Arbitrary<List<Long>> smallBets() {
        return Arbitraries.longs().between(100L, 10_000L)
            .list().ofMinSize(0).ofMaxSize(10);
    }

    private static PotState mkPot(List<Long> bets, String prefix) {
        PotState pot = new PotState();
        for (int i = 0; i < bets.size(); i++) {
            pot.addReal(mkTran(prefix + i, i + 1, bets.get(i)));
        }
        return pot;
    }

    private static TransactionTaiXiuDetail mkTran(String user, int id, long value) {
        return new TransactionTaiXiuDetail(
            1L, id, user, value, 1, 10, 1, value * 100, 1_000_000L + id);
    }
}
