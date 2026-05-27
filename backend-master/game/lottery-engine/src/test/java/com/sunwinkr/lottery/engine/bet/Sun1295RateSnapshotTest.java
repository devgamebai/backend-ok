package com.sunwinkr.lottery.engine.bet;

import com.sunwinkr.lottery.engine.model.LotteryMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * INV-LOTTERY-05 — SUN-1295. Mutating {@link LotteryMode} state after
 * a bet has been placed must NOT change the stored snapshot's
 * computed prize.
 *
 * <p>{@link LotteryMode} no longer exposes setters (audit hardening,
 * see {@code LotteryModeTest.settersRemoved}). The strongest test we
 * can write in pure Java is: take a snapshot, prove the snapshot is
 * stable across enum lookups, and prove the snapshot fields match the
 * enum at the captured instant. The settle-side INV is exercised by
 * {@code PrizeCalculatorTest} reading from a {@link
 * com.sunwinkr.lottery.engine.model.LotteryTicket} that carries the
 * snapshot.
 */
class Sun1295RateSnapshotTest {

    @Test
    void snapshotPreservesRateAndPrizeMultiplier() {
        LotteryMode mode = LotteryMode.byId(1).orElseThrow(IllegalStateException::new);
        BetSnapshot snap = BetSnapshot.of(mode);

        assertThat(snap.getRateAtPurchase()).isEqualTo(mode.getRate());
        assertThat(snap.getPrizeMultiplierAtPurchase()).isEqualTo(mode.getPrizeMultiplier());

        // Snapshot is detached — subsequent enum reads do not change it.
        LotteryMode reread = LotteryMode.byId(1).orElseThrow(IllegalStateException::new);
        assertThat(snap.getRateAtPurchase()).isEqualTo(reread.getRate());
    }

    @Test
    void snapshotIsImmutable() {
        // BetSnapshot has no setters. Defence-in-depth — fields stay value-type.
        BetSnapshot snap = BetSnapshot.of(LotteryMode.BAO_LO_2_SO);
        int beforeRate = snap.getRateAtPurchase();
        int beforePrize = snap.getPrizeMultiplierAtPurchase();

        // Take a fresh snapshot from a different mode — original must not move.
        BetSnapshot other = BetSnapshot.of(LotteryMode.DE_DAC_BIET);
        assertThat(snap.getRateAtPurchase()).isEqualTo(beforeRate);
        assertThat(snap.getPrizeMultiplierAtPurchase()).isEqualTo(beforePrize);
        // Confirm the other really has different values (sanity).
        assertThat(other.getPrizeMultiplierAtPurchase())
                .isNotEqualTo(snap.getPrizeMultiplierAtPurchase());
    }
}
