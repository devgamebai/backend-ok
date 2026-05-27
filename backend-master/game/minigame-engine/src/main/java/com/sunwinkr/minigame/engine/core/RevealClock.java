package com.sunwinkr.minigame.engine.core;

/**
 * Drives the round through {@link RevealPhase} transitions in response to
 * 1-Hz ticks. Pure Java — no scheduler, no Spring; the caller (Spring
 * {@code @Scheduled} or BitZero {@code taskScheduler}) supplies the heartbeat.
 *
 * <p>Tick-to-phase mapping per
 * {@code docs/plans/taixiu-extraction-plan.md §3.2}:
 * <pre>
 *   count 0..44 -> OPEN
 *   count 45     -> LOCKED       (disableBetting; 6s lock window starts)
 *   count 48     -> refund-calc  (still LOCKED)
 *   count 50     -> finish-flag  (still LOCKED)
 *   count 51     -> GENERATING   (dice -> pendingDice; NOT broadcast)
 *   count 52     -> REVEALED     (publish dice; +1s gap from §3 hardening)
 *   count 56     -> SETTLED      (calculatePrize)
 *   count 60     -> CLEANUP
 *   count 68     -> OPEN         (++refId; new round)
 * </pre>
 *
 * <p>Total round length = 68 ticks @ 1Hz = 68s, matching
 * {@code TaiXiuModule.gameLoop} (TXM:422-468). The 51-&gt;52 split adds a
 * 1-tick gap so {@code pendingDice} can be written before the broadcast
 * lifecycle ever surfaces them. The lock window is 6 ticks
 * (count 45..50) ≥ the 6s hardening floor.
 *
 * <p>PR-1 scope: phase emission only. Bet acceptance, dice generation,
 * prize calc, jackpot, bots are out-of-scope here — the
 * {@link TaiXiuRound} consumer reacts to {@link Event}s in later PRs.
 */
public interface RevealClock {

    /** Snapshot tick count for the current round. Resets to 0 on new round. */
    int count();

    /** Current phase as last emitted. */
    RevealPhase phase();

    /**
     * Advance the clock by one tick. Returns the event (if any) that the
     * caller should react to. The contract is: returning a non-null event
     * implies the {@link #phase()} has already been mutated to the new phase
     * (or, for {@link Event#NEW_ROUND}, the count has been reset).
     *
     * @return event for the new tick, or {@code null} if this is an OPEN/LOCKED idle tick
     */
    Event advance();

    /**
     * Reset to count=0 / OPEN — invoked by the round when starting a new
     * reference. Idempotent if already at start state.
     */
    void resetForNewRound();

    /** Driver events emitted on phase boundary ticks. */
    enum Event {
        /** count 45: lock betting; legacy {@code disableBetting()}. */
        LOCK_BETTING,
        /** count 48: refund precalc (no dice). */
        REFUND_CALC,
        /** count 50: finish-flag; remove HZ keys. */
        FINISH_FLAG,
        /** count 51: dice generation tick. {@code pendingDice} is written. */
        GENERATE_DICE,
        /** count 52: reveal tick. {@code revealedDice} is published. */
        REVEAL_DICE,
        /** count 56: prize calc / settle. */
        SETTLE,
        /** count 60: bot cleanup / reschedule. */
        CLEANUP_BOTS,
        /** count 68: increment refId and start next round. */
        NEW_ROUND
    }

    /**
     * Reference implementation. Single-threaded by contract — caller must
     * invoke {@link #advance()} from a single scheduler thread, matching
     * BitZero's {@code taskScheduler.scheduleAtFixedRate(..., 1, SECONDS)}
     * pattern (TXM:156).
     *
     * <p>Not thread-safe for concurrent {@code advance()} calls. The
     * {@link #count} field is intentionally non-volatile — readers from
     * other threads should pull state from {@link TaiXiuRound#snapshotForClient}
     * which provides a happens-before barrier via its own volatile fields.
     */
    final class SimpleRevealClock implements RevealClock {
        private int count = 0;
        private RevealPhase phase = RevealPhase.OPEN;

        @Override
        public int count() {
            return count;
        }

        @Override
        public RevealPhase phase() {
            return phase;
        }

        @Override
        public Event advance() {
            count++;
            switch (count) {
                case 45:
                    RevealPhase.requireLegalTransition(phase, RevealPhase.LOCKED);
                    phase = RevealPhase.LOCKED;
                    return Event.LOCK_BETTING;
                case 48:
                    // still LOCKED — informational event only
                    return Event.REFUND_CALC;
                case 50:
                    return Event.FINISH_FLAG;
                case 51:
                    RevealPhase.requireLegalTransition(phase, RevealPhase.GENERATING);
                    phase = RevealPhase.GENERATING;
                    return Event.GENERATE_DICE;
                case 52:
                    RevealPhase.requireLegalTransition(phase, RevealPhase.REVEALED);
                    phase = RevealPhase.REVEALED;
                    return Event.REVEAL_DICE;
                case 56:
                    RevealPhase.requireLegalTransition(phase, RevealPhase.SETTLED);
                    phase = RevealPhase.SETTLED;
                    return Event.SETTLE;
                case 60:
                    RevealPhase.requireLegalTransition(phase, RevealPhase.CLEANUP);
                    phase = RevealPhase.CLEANUP;
                    return Event.CLEANUP_BOTS;
                case 68:
                    // Caller must call resetForNewRound() after handling.
                    return Event.NEW_ROUND;
                default:
                    return null;
            }
        }

        @Override
        public void resetForNewRound() {
            count = 0;
            phase = RevealPhase.OPEN;
        }
    }
}
