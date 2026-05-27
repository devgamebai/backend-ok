package com.sunwinkr.minigame.engine.sicbo.bet;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-round transaction-ID generator for Sicbo bets.
 *
 * <p>Formula: {@code referenceId * 1_000_000L + sequence.incrementAndGet()}
 * where {@code sequence} is an {@link AtomicLong} that resets to 0 at the
 * start of each round (INV-12).
 *
 * <h3>INV-12 — distinct from TaiXiu</h3>
 * TaiXiu uses {@code System.nanoTime() % 1_000_000} (nanoTime approach).
 * Sicbo uses a per-room monotonic {@code AtomicLong txnSequence} that is
 * incremented for every bet, bot or real (SBR:568-569). This is the
 * idempotency solution added in SUN-880 to prevent the
 * {@code (txId, source, user_id)} unique-key collision when a round sees
 * both bot bets and player bets on the same referenceId.
 *
 * <h3>Thread-safety</h3>
 * {@link AtomicLong#incrementAndGet()} is lock-free and safe for concurrent
 * calls from the game-loop thread and bot-scheduler thread.
 *
 * <h3>Uniqueness guarantee within a round</h3>
 * With a 1_000_000 multiplier, up to 999_999 bets per round are collision-free.
 * Sicbo's max observed bets per 40-second window is well under 10_000.
 */
public final class SicboTxIdGenerator {

    private final long referenceId;
    private final AtomicLong sequence = new AtomicLong(0L);

    /**
     * Create a generator tied to {@code referenceId}.
     * Reset to a new generator (via the factory in {@code SicboPotState})
     * at the start of each round.
     */
    public SicboTxIdGenerator(long referenceId) {
        this.referenceId = referenceId;
    }

    /**
     * Generate the next unique transaction ID for this round.
     *
     * @return {@code referenceId * 1_000_000L + incrementedSequence}
     */
    public long next() {
        return referenceId * 1_000_000L + sequence.incrementAndGet();
    }

    /** The reference ID this generator was created for. */
    public long getReferenceId() {
        return referenceId;
    }

    /** Current sequence value (read-only; for tests/diagnostics). */
    public long currentSequence() {
        return sequence.get();
    }
}
