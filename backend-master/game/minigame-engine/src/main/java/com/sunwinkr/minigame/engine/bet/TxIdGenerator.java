package com.sunwinkr.minigame.engine.bet;

/**
 * Per-bet transaction id generator. Reproduces the
 * {@code referenceId * 1_000_000L + (System.nanoTime() &amp; 0xFFFFFL)}
 * encoding established by SUN-1290 (TXR:429), which keeps multiple
 * same-user / same-side bets in one round from colliding on the
 * {@code (tx_id, source, user_id)} dedup key in {@code money_gateway_log}.
 *
 * <p>Encoding rationale (preserved verbatim from TXR:425-430):
 * <ul>
 *   <li>{@code refId * 1e6} keeps the round visible in audit so
 *       operators can trace a row back to its round.</li>
 *   <li>{@code nanoTime() &amp; 0xFFFFFL} (20-bit suffix, ≈ 1.04M-cycle)
 *       guarantees uniqueness within a JVM since nanoTime is monotonic
 *       per process.</li>
 *   <li>The complementary {@code TaiXiuHoanTien} race refund reuses
 *       this same id (TXR:440) so the rollback correlates to its bet.</li>
 * </ul>
 *
 * <p>Invariant pinned by {@link com.sunwinkr.minigame.engine.bet
 *  .TxIdGeneratorTest}: 1M iterations within a single round produce
 * zero collisions (spec §9 INV-12).
 *
 * <p>Plan §2.2 row B2.
 */
public final class TxIdGenerator {

    private TxIdGenerator() {
        // utility
    }

    /**
     * @param refId round reference id (must be &gt; 0)
     * @return unique-per-round bet txId encoded as
     *         {@code refId * 1_000_000L + (nanoTime &amp; 0xFFFFFL)}
     */
    public static long nextBetTxId(long refId) {
        return refId * 1_000_000L + (System.nanoTime() & 0xFFFFFL);
    }
}
