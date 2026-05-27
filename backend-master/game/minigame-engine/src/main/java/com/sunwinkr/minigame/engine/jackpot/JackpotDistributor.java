package com.sunwinkr.minigame.engine.jackpot;

import com.sunwinkr.minigame.engine.bet.PotState;
import com.sunwinkr.minigame.engine.bet.TransactionTaiXiuDetail;

import java.util.ArrayList;
import java.util.List;

/**
 * Allocates the jackpot pool across winning-side contributors using the
 * legacy share formula (TXR:778, 798):
 * <pre>jp_share = tienDuocTinh * jackpotAccumulate / sum(tienDuocTinh)</pre>
 *
 * <p>Insertion order is preserved — late contributors with partial
 * {@code tienDuocTinh} match the legacy cross-pot iteration.
 *
 * <h3>Integer truncation</h3>
 * Each share is computed in {@code long} via integer division, so the
 * sum of shares may be less than the pool by up to {@code winners} VIN.
 * This matches the legacy behavior (spec INV-11 — "mod integer
 * truncation drift ≤ winner_count").
 *
 * <p>Plan §2.5 row J2 / spec INV-11.
 */
public final class JackpotDistributor {

    /**
     * One winner's allocation. Returned in insertion order.
     */
    public static final class JackpotShare {
        public final String username;
        public final long tienDuocTinh;
        public final long jpAmount;
        public final boolean isBot;
        public final int userId;

        JackpotShare(String username, long tienDuocTinh, long jpAmount, boolean isBot, int userId) {
            this.username = username;
            this.tienDuocTinh = tienDuocTinh;
            this.jpAmount = jpAmount;
            this.isBot = isBot;
            this.userId = userId;
        }
    }

    /**
     * Compute jackpot shares.
     *
     * @param winningPot         pot whose side carries the jackpot
     *                            ({@code potTai} when {@code isJpTai},
     *                            else {@code potXiu})
     * @param tongTienHopLe      cross-pot legal amount
     *                            ({@code CrossPotBalancer.legalAmount})
     * @param jackpotAccumulate  current jackpot pool value (VIN units)
     * @param botUsernames       set of usernames flagged as bots (for skip
     *                            on notify); never null
     * @return list of shares in insertion order; never null
     */
    public List<JackpotShare> distribute(PotState winningPot,
                                         long tongTienHopLe,
                                         long jackpotAccumulate,
                                         java.util.Set<String> botUsernames) {
        if (winningPot == null) {
            throw new NullPointerException("winningPot");
        }
        if (botUsernames == null) {
            throw new NullPointerException("botUsernames");
        }
        List<JackpotShare> out = new ArrayList<>();
        List<TransactionTaiXiuDetail> contributors = winningPot.contributors();

        // Denominator: total tienDuocTinh on the winning side per the
        // running-pot cross-pot rule (TXR:769-777).
        long denominator = 0L;
        long running = 0L;
        long[] tienDuocTinhArr = new long[contributors.size()];
        for (int i = 0; i < contributors.size(); i++) {
            TransactionTaiXiuDetail tran = contributors.get(i);
            long tienDuocTinh = tran.betValue;
            if (running + tran.betValue > tongTienHopLe) {
                tienDuocTinh = tongTienHopLe - running;
            }
            if (tienDuocTinh < 0L) {
                tienDuocTinh = 0L;
            }
            running += tienDuocTinh;
            tienDuocTinhArr[i] = tienDuocTinh;
            denominator += tienDuocTinh;
        }
        if (denominator <= 0L || jackpotAccumulate <= 0L) {
            // No winners (or no pool) → empty distribution.
            for (int i = 0; i < contributors.size(); i++) {
                TransactionTaiXiuDetail tran = contributors.get(i);
                out.add(new JackpotShare(tran.username, tienDuocTinhArr[i], 0L,
                    botUsernames.contains(tran.username), tran.userId));
            }
            return out;
        }
        for (int i = 0; i < contributors.size(); i++) {
            TransactionTaiXiuDetail tran = contributors.get(i);
            long tienDuocTinh = tienDuocTinhArr[i];
            // Integer truncation drift per spec INV-11.
            long share = tienDuocTinh * jackpotAccumulate / denominator;
            out.add(new JackpotShare(tran.username, tienDuocTinh, share,
                botUsernames.contains(tran.username), tran.userId));
        }
        return out;
    }
}
