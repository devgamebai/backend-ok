package com.sunwinkr.minigame.engine.bet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Engine-side accumulator for one side of a TaiXiu round (Tài or Xỉu).
 * Mirrors {@code PotTaiXiu} (PT:32-50) but without the BitZero / DAL
 * coupling — pure Java + synchronized accessors.
 *
 * <p>Each successful bet adds a {@link TransactionTaiXiuDetail} to
 * {@link #contributors} in insertion order (used later by
 * {@code PrizeCalculator} cross-pot balancing — spec INV-6). The
 * {@link #users} set dedups; {@link #realUsers} excludes bots; bot stats
 * are tracked separately.
 *
 * <h3>Thread-safety</h3>
 * Legacy code (PT:34-42) synchronizes on the individual list/set
 * references. We preserve that discipline with synchronized blocks on
 * the same monitors. Tests under {@code PotStateTest.threadSafeAdd}
 * pin the invariant by stress-driving 8 threads.
 *
 * <p>Plan §2.2 row B8 / spec INV-16, INV-22.
 */
public final class PotState {

    private final List<TransactionTaiXiuDetail> contributors = new ArrayList<>();
    /** Insertion-ordered username dedup set (LinkedHashSet for stable iteration). */
    private final LinkedHashSet<String> users = new LinkedHashSet<>();
    private final HashSet<String> realUsers = new HashSet<>();

    private volatile long totalValue;
    private volatile long totalBotBet;
    private volatile int numBot;

    /**
     * Add a real-player contributor. Increments {@link #totalValue} and
     * tracks the username in {@link #users} + {@link #realUsers}.
     */
    public void addReal(TransactionTaiXiuDetail trans) {
        addContributor(trans, false);
    }

    /**
     * Add a bot contributor. Increments {@link #totalValue},
     * {@link #totalBotBet}, and {@link #numBot}; tracks the username in
     * {@link #users} only (NOT {@link #realUsers}).
     */
    public void addBot(TransactionTaiXiuDetail trans) {
        addContributor(trans, true);
    }

    /**
     * Core append path. Synchronized on the {@code contributors} list to
     * match PT:34 and the {@code users} set to match PT:38.
     *
     * @param trans contributor record
     * @param isBot whether to route through bot tracking
     */
    public void addContributor(TransactionTaiXiuDetail trans, boolean isBot) {
        if (trans == null) {
            throw new NullPointerException("trans");
        }
        synchronized (contributors) {
            contributors.add(trans);
        }
        synchronized (users) {
            users.add(trans.username);
        }
        // totalValue is read on the snapshot/broadcast thread; keep the
        // update synchronized on contributors so concurrent addContributor
        // calls cannot tear the read-modify-write.
        synchronized (contributors) {
            this.totalValue += trans.betValue;
            if (isBot) {
                this.totalBotBet += trans.betValue;
                this.numBot += 1;
            } else {
                synchronized (realUsers) {
                    realUsers.add(trans.username);
                }
            }
        }
    }

    /** Total bet value across both real players and bots. */
    public long totalValue() {
        return totalValue;
    }

    /** Sum of bet values for the given username; 0 if user has not bet. */
    public long totalByUser(String username) {
        if (username == null) {
            return 0L;
        }
        long sum = 0L;
        synchronized (contributors) {
            for (TransactionTaiXiuDetail t : contributors) {
                if (username.equals(t.username)) {
                    sum += t.betValue;
                }
            }
        }
        return sum;
    }

    /** Dedup count across real players + bots (matches {@code getNumBet()}). */
    public short numBet() {
        synchronized (users) {
            return (short) users.size();
        }
    }

    /** Dedup count of real (non-bot) players. */
    public short realNumBet() {
        synchronized (realUsers) {
            return (short) realUsers.size();
        }
    }

    /** Real (non-bot) pot total. Matches legacy {@code getRealUserBet()}. */
    public long realTotal() {
        return totalValue - totalBotBet;
    }

    /** {@code true} if a real player has placed any bet (matches {@code hasBet}). */
    public boolean hasBet(String username) {
        if (username == null) {
            return false;
        }
        synchronized (realUsers) {
            return realUsers.contains(username);
        }
    }

    /** Read-only snapshot of contributors in insertion order. */
    public List<TransactionTaiXiuDetail> contributors() {
        synchronized (contributors) {
            return Collections.unmodifiableList(new ArrayList<>(contributors));
        }
    }

    /** Read-only snapshot of all participating usernames (real + bot). */
    public Set<String> users() {
        synchronized (users) {
            return Collections.unmodifiableSet(new LinkedHashSet<>(users));
        }
    }

    /** Bot-only stats holder. */
    public BotStats botStats() {
        return new BotStats(totalBotBet, numBot);
    }

    /** Plain pair for bot stats. */
    public static final class BotStats {
        public final long totalBotBet;
        public final int numBot;

        BotStats(long totalBotBet, int numBot) {
            this.totalBotBet = totalBotBet;
            this.numBot = numBot;
        }
    }

    /**
     * Reset for a new round (INV-16). Clears contributors, users,
     * realUsers, totalValue, totalBotBet, numBot.
     */
    public void renew() {
        synchronized (contributors) {
            contributors.clear();
            totalValue = 0L;
            totalBotBet = 0L;
            numBot = 0;
        }
        synchronized (users) {
            users.clear();
        }
        synchronized (realUsers) {
            realUsers.clear();
        }
    }
}
