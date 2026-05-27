package com.sunwinkr.minigame.engine.port;

/**
 * Engine-facing port for the user wallet. Adapter implementations
 * (PR-4) wrap {@code UserServiceImpl.updateMoney}; the engine itself
 * is wallet-agnostic.
 *
 * <p>Two callable shapes for debit and credit — both with default
 * implementations that delegate into the other. Concrete adapters and
 * test stubs override the form that matches their needs.
 *
 * <ul>
 *   <li>9-arg form ({@code debit/credit(user, amount, moneyType, source,
 *       gameId, desc, fee, txId, kind)}) — TaiXiu canonical with full
 *       audit trail. Production adapters override this.</li>
 *   <li>5-arg form ({@code debit/credit(user, amount, moneyType, txId,
 *       kind)}) — Sicbo PR-3 minimal form. Sicbo test stubs override
 *       this.</li>
 * </ul>
 *
 * <p>Engine bet paths call whichever form fits — both resolve to the
 * subclass's override path via cross-delegation.
 *
 * <p>The {@code getBalance} method is the only true abstract member.
 *
 * <p>Plan §4.1 / row B4-B5.
 */
public interface WalletPort {

    /**
     * Read-only balance lookup. Mirrors {@code UserService
     * .getMoneyUserCache(nickname, moneyType)} (TXR:404). Returns 0 when
     * the user is unknown.
     */
    long getBalance(String user, String moneyType);

    /**
     * Debit {@code amount} from the user's wallet (full 9-arg form).
     * Default forwards to the 5-arg form for Sicbo-style adapters.
     */
    default MoneyResult debit(String user,
                               long amount,
                               String moneyType,
                               String source,
                               long gameId,
                               String desc,
                               long fee,
                               long txId,
                               TransKind transKind) {
        return debit(user, amount, moneyType, txId, transKind);
    }

    /**
     * Credit {@code amount} to the user's wallet (full 9-arg form).
     * Default forwards to the 5-arg form.
     */
    default MoneyResult credit(String user,
                                long amount,
                                String moneyType,
                                String source,
                                long gameId,
                                String desc,
                                long fee,
                                long txId,
                                TransKind transKind) {
        return credit(user, amount, moneyType, txId, transKind);
    }

    /**
     * Sicbo PR-3 5-arg overload. Default forwards into the 9-arg form
     * with adapter-friendly defaults. Sicbo test stubs override this
     * method directly.
     */
    default MoneyResult debit(String user, long amount, String moneyType, long txId, TransKind kind) {
        return debit(user, amount, moneyType, "Sicbo", 0L,
            "sicbo bet debit txId=" + txId, 0L, txId, kind);
    }

    /**
     * Sicbo PR-3 5-arg overload for refunds. Default forwards to the
     * 9-arg form.
     */
    default MoneyResult credit(String user, long amount, String moneyType, long txId, TransKind kind) {
        return credit(user, amount, moneyType, "SicboHoanTien", 0L,
            "sicbo refund txId=" + txId, 0L, txId, kind);
    }

    /**
     * Write a 0-exchange settle-confirmation ledger row (SUN-1306 pattern).
     * Called for losing bets so the wallet log is symmetric.
     *
     * <p>Default no-op — adapters that support this pattern (e.g.
     * {@code JdbcLotteryWalletPort}) override it. Adapters that do not
     * (e.g. test stubs, Sicbo) silently omit the row.
     */
    default MoneyResult writeSettleConfirmationRow(String user,
                                                    String moneyType,
                                                    String source,
                                                    String gameId,
                                                    String description,
                                                    String actionName,
                                                    long txId) {
        return MoneyResult.success(0L); // no-op
    }
}
