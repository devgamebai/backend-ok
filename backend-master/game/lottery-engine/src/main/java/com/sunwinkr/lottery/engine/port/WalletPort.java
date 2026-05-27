package com.sunwinkr.lottery.engine.port;

/**
 * Wallet boundary — debit at bet acceptance, credit at settle.
 *
 * <p>Concrete adapter (PR-3) wraps
 * {@code com.vinplay.usercore.service.impl.UserServiceImpl#updateMoney}. The
 * legacy call site:
 * <pre>
 *   userService.updateMoney(user.getName(), -finalBetValue, "vin", "LoDe",
 *       "Lô Đề", "Cược ..." , 0L, now, TransType.START_TRANS);
 * </pre>
 * Per {@code docs/specs/lottery-rules-spec.md §5.1} and AMBIGUOUS #10,
 * {@code moneyType="vin"} is hard-coded — kept as a parameter here so a
 * future XU lottery (if introduced) does not require a port change.
 *
 * <p>{@code TransKind.START} is the only value used today (legacy uses
 * START for BOTH debit and credit — JLM:129 + JLM:227).
 */
public interface WalletPort {

    /**
     * Debit {@code amount} from {@code nickname}'s wallet. Lottery uses this
     * at bet acceptance — the caller passes a POSITIVE amount; the adapter
     * negates internally when calling {@code updateMoney}.
     *
     * @param nickname        wallet owner
     * @param amount          positive bet amount (engine-side sign is +)
     * @param moneyType       hard-coded {@code "vin"} today; param for future
     * @param source          legacy "source" tag — lottery uses {@code "LoDe"}
     * @param gameId          legacy game key — lottery uses {@code "Lô Đề"}
     * @param description     free-text trans description
     * @param fee             trans fee (lottery uses 0)
     * @param txId            external trans correlation id (epoch ms ok)
     * @param kind            see {@link TransKind} — lottery uses START
     * @return success / errorCode / currentMoney
     */
    MoneyResult debit(String nickname,
                      long amount,
                      String moneyType,
                      String source,
                      String gameId,
                      String description,
                      long fee,
                      long txId,
                      TransKind kind);

    /**
     * Credit {@code amount} to {@code nickname}'s wallet (settle path).
     *
     * <p>Signature matches {@link #debit} so the adapter implementation
     * can route both to the same legacy {@code updateMoney} method,
     * differentiated only by the sign of {@code amount} at the JDBC layer.
     *
     * @return success / errorCode / currentMoney
     */
    MoneyResult credit(String nickname,
                       long amount,
                       String moneyType,
                       String source,
                       String gameId,
                       String description,
                       long fee,
                       long txId,
                       TransKind kind);

    /**
     * Write a settle-confirmation ledger row WITHOUT moving balance — used
     * to fulfill SUN-1306's "2-record split" for losing Lô Đề tickets.
     *
     * <p>The default implementation is a no-op so existing adapter tests
     * don't break. Production adapter ({@code JdbcLotteryWalletPort})
     * publishes a {@code LogMoneyUserMessage} with
     * {@code money_exchange=0} + current balance directly to
     * {@code queue_log_money} (the same queue {@code UserServiceImpl}
     * normally publishes to, but bypassing its
     * {@code if (money != 0L)} short-circuit).
     *
     * @param nickname    wallet owner (lookup id via Hazelcast cacheToken)
     * @param moneyType   hard-coded "vin" today
     * @param source      lottery uses {@code "LoDe"}
     * @param gameId      legacy game key — lottery uses {@code "Lô Đề"}
     * @param description settle reason — e.g. "Thua cược Lô Đề (LÔ 2 SỐ — số 12)"
     * @param actionName  for ledger filter — e.g. "Kết quả Lô Đề"
     * @param txId        epoch ms correlation
     * @return success / errorCode (no balance change)
     */
    default MoneyResult writeSettleConfirmationRow(String nickname,
                                                    String moneyType,
                                                    String source,
                                                    String gameId,
                                                    String description,
                                                    String actionName,
                                                    long txId) {
        // Default: no-op. JdbcLotteryWalletPort override does the real publish.
        return MoneyResult.ok(0L);
    }
}
