package com.sunwinkr.minigame.engine.prize;

import com.sunwinkr.minigame.engine.bet.PotState;
import com.sunwinkr.minigame.engine.bet.TransactionTaiXiuDetail;

import java.util.ArrayList;
import java.util.List;

/**
 * Cross-pot balance allocator (TaiXiu). Implements the
 * {@code tongTienHopLe = min(potTai, potXiu)} legal-amount rule
 * (spec §5 / INV-5) and the insertion-order per-contributor
 * {@code tienDuocTinh + refund} accounting (TXR:559-567, INV-6).
 *
 * <h3>Allocation rule</h3>
 * For each contributor on a side in insertion order:
 * <pre>
 *   tienDuocTinh = bet
 *   if (running + bet &gt; tongTienHopLe)
 *       tienDuocTinh = tongTienHopLe - running
 *   running += tienDuocTinh
 *   refund   = bet - tienDuocTinh
 * </pre>
 *
 * <p>This is a pure-function utility — no mutation of the supplied
 * {@link PotState}.
 *
 * <p>Plan §2.4 rows P1, P2.
 */
public final class CrossPotBalancer {

    private CrossPotBalancer() {
        // utility
    }

    /**
     * {@code tongTienHopLe = min(potTai.total, potXiu.total)} — the
     * matchable amount per side.
     */
    public static long legalAmount(PotState potTai, PotState potXiu) {
        if (potTai == null || potXiu == null) {
            return 0L;
        }
        return Math.min(potTai.totalValue(), potXiu.totalValue());
    }

    /** Per-contributor allocation. */
    public static final class Allocation {
        public final TransactionTaiXiuDetail tran;
        public final long tienDuocTinh;
        public final long refund;

        Allocation(TransactionTaiXiuDetail tran, long tienDuocTinh, long refund) {
            this.tran = tran;
            this.tienDuocTinh = tienDuocTinh;
            this.refund = refund;
        }
    }

    /**
     * Allocate {@code tienDuocTinh} + {@code refund} per contributor in
     * insertion order.
     *
     * @param pot         pot to allocate from
     * @param tongTienHopLe  legal amount cap
     * @return list of allocations in insertion order; never null
     */
    public static List<Allocation> allocate(PotState pot, long tongTienHopLe) {
        if (pot == null) {
            return new ArrayList<>();
        }
        List<TransactionTaiXiuDetail> contributors = pot.contributors();
        List<Allocation> out = new ArrayList<>(contributors.size());
        long running = 0L;
        for (TransactionTaiXiuDetail tran : contributors) {
            long tienDuocTinh = tran.betValue;
            if (running + tran.betValue > tongTienHopLe) {
                tienDuocTinh = tongTienHopLe - running;
            }
            if (tienDuocTinh < 0L) {
                tienDuocTinh = 0L;
            }
            running += tienDuocTinh;
            long refund = tran.betValue - tienDuocTinh;
            out.add(new Allocation(tran, tienDuocTinh, refund));
        }
        return out;
    }
}
