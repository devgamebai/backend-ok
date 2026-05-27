package com.sunwinkr.minigame.engine.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guards against pre-reveal disclosure of dice. Wraps every legacy logging
 * site that prints dice values so we never accidentally surface them to a
 * log appender or stdout before {@link RevealPhase#REVEALED}.
 *
 * <p>Per {@code docs/plans/taixiu-extraction-plan.md §3.4}, the three
 * legacy sites being protected by this class are:
 * <ol>
 *   <li>{@code TaiXiuModule.java:487}  — "GENERATE RESULT" sysout</li>
 *   <li>{@code MGRoomTaiXiu.java:618}  — "Result End" sysout</li>
 *   <li>{@code TaiXiuModule.java:281-282} — "ForceResultTaiXiu" admin sysout</li>
 * </ol>
 *
 * <p>The contract is intentionally noisy: any call to
 * {@link #traceDice(RevealPhase, short[], String)} from a non-revealed
 * phase throws {@link IllegalStateException} rather than silently masking.
 * If silent masking is desired in a hot path, the caller should branch on
 * {@code phase.diceVisible()} before calling.
 *
 * <p>PR-1 scope: defines guard. Wiring into legacy call sites happens in
 * PR-3 alongside dice extraction.
 */
public final class RevealGuard {

    private static final Logger LOG = LoggerFactory.getLogger(RevealGuard.class);

    private RevealGuard() {
        // utility
    }

    /**
     * Emit a dice trace line. Throws if the phase has not yet revealed.
     *
     * @param phase   current round phase
     * @param dice    dice values (length 3 for TaiXiu; length 3 for Sicbo)
     * @param message human-readable context tag, e.g. "GENERATE RESULT"
     * @throws IllegalStateException if {@code phase.diceVisible()} is false
     * @throws NullPointerException  if {@code dice} or {@code message} is null
     */
    public static void traceDice(RevealPhase phase, short[] dice, String message) {
        if (phase == null) {
            throw new NullPointerException("phase");
        }
        if (dice == null) {
            throw new NullPointerException("dice");
        }
        if (message == null) {
            throw new NullPointerException("message");
        }
        if (!phase.diceVisible()) {
            throw new IllegalStateException(
                "RevealGuard: dice trace attempted in pre-reveal phase "
                    + phase + " for message=" + message);
        }
        if (LOG.isInfoEnabled()) {
            StringBuilder sb = new StringBuilder(48);
            sb.append(message).append(" dice=[");
            for (int i = 0; i < dice.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(dice[i]);
            }
            sb.append(']');
            LOG.info(sb.toString());
        }
    }

    /**
     * Emit an admin-context dice trace (replacement for legacy
     * {@code System.out.println("ForceResultTaiXiu...")} at TXM:281-282).
     * Admin context is allowed to read pending dice values because they
     * originated from the admin's own force-result command, but we still
     * tag the caller role for audit.
     *
     * <p>PR-1 stub. Auth resolution lands in PR-3's Spring controller
     * (plan §3.6) — the engine itself remains role-blind.
     *
     * @param role   role tag from caller (e.g. "MINIGAME_ADMIN")
     * @param dice   dice values
     * @param tag    audit tag, e.g. "ForceResult"
     */
    public static void adminTrace(String role, short[] dice, String tag) {
        if (role == null) {
            throw new NullPointerException("role");
        }
        if (dice == null) {
            throw new NullPointerException("dice");
        }
        if (tag == null) {
            throw new NullPointerException("tag");
        }
        if (LOG.isInfoEnabled()) {
            StringBuilder sb = new StringBuilder(64);
            sb.append("ADMIN ").append(tag).append(" role=").append(role).append(" dice=[");
            for (int i = 0; i < dice.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(dice[i]);
            }
            sb.append(']');
            LOG.info(sb.toString());
        }
    }
}
