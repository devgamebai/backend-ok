package com.sunwinkr.minigame.engine.sicbo.bet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe pot accumulator for one Sicbo round.
 *
 * <p>Tracks all accepted bets across all 52 {@link SicboBetType} sides.
 * Unlike TaiXiu (which has exactly two sides — TAI and XIU), Sicbo allows
 * players to bet on any of 52 bet types in the same round. The pot does not
 * partition by side; it holds a flat list of all accepted entries and
 * exposes aggregation queries (total real-user value for RTP, snapshot for
 * prize calc).
 *
 * <h3>Source mapping</h3>
 * <ul>
 *   <li>{@code addBet()} ← {@code MGRoomSicbo.potTai.bet(transTX, isBot)} /
 *       {@code potXiu.bet(transTX, isBot)} (SBR:601-605). The legacy code
 *       routes to potTai when betSide==1 else potXiu. In the extracted engine
 *       the single flat list replaces both; the betSideId on each entry drives
 *       prize routing in PR-3.</li>
 *   <li>{@code totalValueBetUser()} ← {@code MGRoomSicbo.totalValueBetUser}
 *       field incremented at SBR:529 (real users only).</li>
 *   <li>{@code listUserBet()} ← {@code MGRoomSicbo.listUserBet} snapshot used
 *       by {@code generateResultWithHouseEdge} and {@code reward()} (SBR:735,
 *       1173).</li>
 * </ul>
 *
 * <h3>Thread-safety</h3>
 * Uses {@link CopyOnWriteArrayList} so concurrent reads (snapshot, size)
 * never block on the writer. {@code totalValueBetUser} is updated under
 * {@code synchronized} to keep it consistent with the list add.
 */
public final class SicboPotState {

    private final CopyOnWriteArrayList<SicboPotEntry> entries = new CopyOnWriteArrayList<>();

    /** Sum of betValue for real (non-bot) users only. Drives RTP balancer. */
    private long totalValueBetUser = 0L;

    /** Lock for the totalValueBetUser counter. */
    private final Object lock = new Object();

    /**
     * Add an accepted bet to the pot.
     *
     * @param entry  fully-constructed pot entry (post-debit, with txId/txCode)
     * @param isBot  if {@code true}, betValue is NOT added to {@link #totalValueBetUser}
     *               (mirrors SBR:528-529 {@code if (!isBot) totalValueBetUser += betValue})
     */
    public void addBet(SicboPotEntry entry, boolean isBot) {
        entries.add(entry);
        if (!isBot) {
            synchronized (lock) {
                totalValueBetUser += entry.betValue;
            }
        }
    }

    /**
     * Total wagered value from real users only (bots excluded).
     * Used by the RTP balancer in PR-3 ({@code SicboRtpDiceGenerator}).
     *
     * <p>Mirrors {@code MGRoomSicbo.totalValueBetUser} (SBR:529).
     */
    public long totalValueBetUser() {
        synchronized (lock) {
            return totalValueBetUser;
        }
    }

    /**
     * Immutable snapshot of all accepted bets (real users + bots).
     * Safe to iterate after betting closes; does not block new {@link #addBet} calls.
     *
     * <p>Mirrors {@code MGRoomSicbo.listUserBet} used at SBR:735 + 1173.
     */
    public List<SicboPotEntry> listUserBet() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    /** Number of entries currently in the pot (real users + bots). */
    public int size() {
        return entries.size();
    }

    /**
     * Find the entry whose {@link SicboPotEntry#perBetTxId} matches {@code txId}.
     *
     * @param txId the per-bet transaction id assigned by {@link SicboTxIdGenerator}
     * @return the matching entry, or {@code null} if not found
     */
    public SicboPotEntry findByTxId(long txId) {
        for (SicboPotEntry e : entries) {
            if (e.perBetTxId == txId) {
                return e;
            }
        }
        return null;
    }

    /** Reset pot for a new round — clears all entries and the RTP counter. */
    public void reset() {
        entries.clear();
        synchronized (lock) {
            totalValueBetUser = 0L;
        }
    }
}
