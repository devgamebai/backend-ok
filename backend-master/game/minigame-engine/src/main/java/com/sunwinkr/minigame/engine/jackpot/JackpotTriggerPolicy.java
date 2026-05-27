package com.sunwinkr.minigame.engine.jackpot;

import com.sunwinkr.minigame.engine.port.JackpotForcePort;

import java.util.Optional;

/**
 * Jackpot side-override policy. Direct port of TXR:594-615.
 *
 * <h3>Logic</h3>
 * Reads the queued jackpot side from {@link JackpotForcePort}; iff the
 * per-side {@code numBet % 5 == 0} gate passes (TXR:596, 599), forces
 * the dice to a triple of the side value and flags the round for
 * jackpot distribution.
 *
 * <p>Encoding:
 * <ul>
 *   <li>{@code 6} = Tài (triple-6 → sum=18 → Tài) — gate: {@code potTai.numBet % 5 == 0}</li>
 *   <li>{@code 1} = Xỉu (triple-1 → sum=3 → Xỉu) — gate: {@code potXiu.numBet % 5 == 0}</li>
 *   <li>any other value → no override</li>
 * </ul>
 *
 * <p>Side effects: when override fires the policy sets
 * {@link #isJpTai()} / {@link #isJpXiu()} / {@link #isResetJp()} for the
 * caller's settlement pass. Caller must inspect these AFTER calling
 * {@link #apply}.
 *
 * <p>Plan §2.3 row D5 + §2.5 / spec INV-10.
 */
public final class JackpotTriggerPolicy {

    private final JackpotForcePort port;

    private volatile boolean isJpTai;
    private volatile boolean isJpXiu;
    private volatile boolean resetJp;

    public JackpotTriggerPolicy(JackpotForcePort port) {
        this.port = port;
    }

    /**
     * Apply the jackpot override if conditions are met. Returns the
     * (possibly rewritten) dice array.
     *
     * @param dice           current dice (must not be null, length &gt;= 3)
     * @param potTaiNumBet   {@code potTai.numBet}
     * @param potXiuNumBet   {@code potXiu.numBet}
     */
    public short[] apply(short[] dice, long potTaiNumBet, long potXiuNumBet) {
        if (dice == null || dice.length < 3) {
            throw new IllegalArgumentException("dice must be length >= 3");
        }
        // Reset transient flags — they only stick if THIS pass triggers.
        this.isJpTai = false;
        this.isJpXiu = false;
        this.resetJp = false;

        if (port == null) {
            return dice;
        }
        Optional<Short> sideOpt = port.peekJackpotSide();
        if (!sideOpt.isPresent()) {
            return dice;
        }
        short checkJackpot = sideOpt.get();

        // Gate per TXR:594-601: numBet must be a multiple of 5 on the
        // matching side, else suppress.
        if (checkJackpot == 6) {
            if (potTaiNumBet % 5 != 0) {
                checkJackpot = 0;
            }
        } else if (checkJackpot == 1) {
            if (potXiuNumBet % 5 != 0) {
                checkJackpot = 0;
            }
        } else {
            checkJackpot = 0;
        }

        if (checkJackpot == 0) {
            return dice;
        }
        // Trigger: rewrite dice to triple + flag.
        this.resetJp = true;
        short[] forced = new short[] { checkJackpot, checkJackpot, checkJackpot };
        if (checkJackpot == 6) {
            this.isJpTai = true;
            this.isJpXiu = false;
        } else {
            this.isJpXiu = true;
            this.isJpTai = false;
        }
        return forced;
    }

    public boolean isJpTai() {
        return isJpTai;
    }

    public boolean isJpXiu() {
        return isJpXiu;
    }

    public boolean isResetJp() {
        return resetJp;
    }
}
