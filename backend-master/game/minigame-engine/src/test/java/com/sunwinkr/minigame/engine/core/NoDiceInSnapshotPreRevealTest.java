package com.sunwinkr.minigame.engine.core;

import com.sunwinkr.minigame.engine.port.CachePort;
import com.sunwinkr.minigame.engine.snapshot.TaiXiuSnapshot;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.StringLength;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plan §3.5: jqwik property test.
 *
 * <p>For every {@link RevealPhase} that is NOT diceVisible (i.e. anything
 * other than REVEALED / SETTLED), and for every username string, the
 * snapshot MUST return zero dice and result = -1, even when the round
 * has pendingDice and revealedDice already seeded.
 *
 * <p>This is the core anti-leak invariant from
 * {@code docs/specs/taixiu-sicbo-anticheat-audit.md §5} — pre-reveal
 * snapshots must never expose dice values, regardless of internal state.
 */
class NoDiceInSnapshotPreRevealTest {

    private static final CachePort NO_OP_CACHE = new CachePort() {
        @Override public void setAllowBetting(long refId, boolean v) { }
        @Override public void removeAllowBetting(long refId) { }
        @Override public void setCurrentReference(long refId) { }
    };

    @Provide
    Arbitrary<RevealPhase> nonRevealedPhases() {
        return Arbitraries.of(RevealPhase.OPEN, RevealPhase.LOCKED,
            RevealPhase.GENERATING, RevealPhase.CLEANUP);
    }

    @Property
    void noDicePreReveal(@ForAll("nonRevealedPhases") RevealPhase phase,
                        @ForAll @StringLength(min = 1, max = 30) String user) {
        TaiXiuRound r = newRoundAtPhase(phase);
        // Seed dice anyway — the snapshot must censor regardless.
        r.setPendingDice(new short[]{4, 5, 6});
        r.setRevealedDice(new short[]{4, 5, 6});
        TaiXiuSnapshot s = r.snapshotForClient(user);
        assertThat(s.dice1).isZero();
        assertThat(s.dice2).isZero();
        assertThat(s.dice3).isZero();
        assertThat(s.result).isEqualTo((short) -1);
    }

    @Property
    void diceVisibleAfterReveal(@ForAll @StringLength(min = 1, max = 30) String user) {
        TaiXiuRound r = newRoundAtPhase(RevealPhase.REVEALED);
        r.setRevealedDice(new short[]{4, 5, 6});
        TaiXiuSnapshot s = r.snapshotForClient(user);
        assertThat(s.dice1).isEqualTo((short) 4);
        assertThat(s.dice2).isEqualTo((short) 5);
        assertThat(s.dice3).isEqualTo((short) 6);
        // 4+5+6 = 15 > 10 → result=1 (TAI)
        assertThat(s.result).isEqualTo((short) 1);
    }

    /** Walk the round forward through legal transitions to the target phase. */
    private static TaiXiuRound newRoundAtPhase(RevealPhase target) {
        TaiXiuRound r = new TaiXiuRound(new RevealClock.SimpleRevealClock(), NO_OP_CACHE);
        switch (target) {
            case OPEN:
                return r;
            case LOCKED:
                r.advance(RevealPhase.LOCKED);
                return r;
            case GENERATING:
                r.advance(RevealPhase.LOCKED);
                r.advance(RevealPhase.GENERATING);
                return r;
            case REVEALED:
                r.advance(RevealPhase.LOCKED);
                r.advance(RevealPhase.GENERATING);
                r.advance(RevealPhase.REVEALED);
                return r;
            case SETTLED:
                r.advance(RevealPhase.LOCKED);
                r.advance(RevealPhase.GENERATING);
                r.advance(RevealPhase.REVEALED);
                r.advance(RevealPhase.SETTLED);
                return r;
            case CLEANUP:
                r.advance(RevealPhase.LOCKED);
                r.advance(RevealPhase.GENERATING);
                r.advance(RevealPhase.REVEALED);
                r.advance(RevealPhase.SETTLED);
                r.advance(RevealPhase.CLEANUP);
                return r;
            default:
                throw new IllegalArgumentException("unhandled: " + target);
        }
    }
}
