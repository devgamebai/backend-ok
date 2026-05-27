package com.sunwinkr.minigame.engine.bet;

/**
 * Holds the two per-side {@link PotState} accumulators for one TaiXiu
 * round (Tài vs Xỉu). Routes bets to the right pot based on
 * {@link BetRequest#betSide}.
 *
 * <p>Routing rule per TXR:391, 465: {@code betSide == 1} → potTai;
 * all other values → potXiu. The fall-through for unknown betSide
 * values is preserved as a behavior quirk (callers should validate
 * before reaching the ledger, but the ledger does NOT throw).
 *
 * <p>Plan §2.2 rows B7/B8.
 */
public final class BetLedger {

    private final PotState potTai = new PotState();
    private final PotState potXiu = new PotState();

    public PotState potTai() {
        return potTai;
    }

    public PotState potXiu() {
        return potXiu;
    }

    /** Returns the pot for the given side (1 → Tài, otherwise → Xỉu). */
    public PotState potForSide(int betSide) {
        return betSide == 1 ? potTai : potXiu;
    }

    /**
     * Add a real-player bet to the appropriate side. Plan B7.
     *
     * @param betSide 1 = Tài, anything else = Xỉu (legacy fall-through)
     */
    public void addReal(int betSide, TransactionTaiXiuDetail trans) {
        potForSide(betSide).addReal(trans);
    }

    /** Add a bot bet to the appropriate side. */
    public void addBot(int betSide, TransactionTaiXiuDetail trans) {
        potForSide(betSide).addBot(trans);
    }

    /** Has the user placed any real bet on the opposite side? (INV-4 helper.) */
    public boolean userHasBetOpposite(String username, int proposedSide) {
        PotState opposite = proposedSide == 1 ? potXiu : potTai;
        return opposite.totalByUser(username) > 0L;
    }

    /** Reset both pots for a new round (INV-16). */
    public void renew() {
        potTai.renew();
        potXiu.renew();
    }
}
