package com.sunwinkr.lottery.api.adapter;

import com.sunwinkr.lottery.engine.port.MoneyResult;
import com.sunwinkr.lottery.engine.port.TransKind;
import com.sunwinkr.lottery.engine.port.WalletPort;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Wallet adapter — binds engine {@link WalletPort} to the legacy
 * {@link UserServiceImpl#updateMoney} (LotteryModule:227, 129).
 *
 * <h3>Lottery-specific contract (AMBIGUOUS #10)</h3>
 * <ul>
 *   <li>{@code moneyType} hard-coded to {@code "vin"} by the engine
 *       (BetAcceptor ctor default; LotterySettleService ctor default).
 *       Kept parametric to allow a future XU lottery without a port
 *       change.</li>
 *   <li>{@code source} = {@code "LoDe"}, {@code gameId} = {@code "Lô Đề"}
 *       — preserves the legacy tags so downstream
 *       transaction history / commission queries continue to match.</li>
 *   <li>Lottery uses {@link TransKind#START} for BOTH debit and credit —
 *       legacy {@code TransType.START_TRANS} is the only kind written
 *       (LotteryModule JLM:129 + JLM:227).</li>
 * </ul>
 *
 * <h3>Sign convention</h3>
 * The engine passes POSITIVE amounts. The adapter negates internally on
 * {@link #debit}; {@link #credit} passes the amount through positive.
 * Mirrors {@code JdbcWalletPort} pattern from :game:minigame-api.
 *
 * <p>Plan §6 / §2.5 S4.
 *
 * <p>{@code @Primary}: resolves NoUniqueBeanDefinitionException when the
 * stale compiled EngineConfig {@code wallet} @Bean wrapper coexists with
 * this @Component. This is the canonical WalletPort.
 */
@Primary
@Component
public class JdbcLotteryWalletPort implements WalletPort {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcLotteryWalletPort.class);

    private final UserService userService;

    /** Default ctor — uses singleton-style {@code UserServiceImpl} instantiation. */
    public JdbcLotteryWalletPort() {
        this(new UserServiceImpl());
    }

    /** Test seam — supply a stub UserService. */
    public JdbcLotteryWalletPort(UserService userService) {
        this.userService = userService;
    }

    @Override
    public MoneyResult debit(String nickname,
                              long amount,
                              String moneyType,
                              String source,
                              String gameId,
                              String description,
                              long fee,
                              long txId,
                              TransKind kind) {
        return call(nickname, -Math.abs(amount), moneyType, source, gameId, description, fee, txId, kind);
    }

    @Override
    public MoneyResult credit(String nickname,
                               long amount,
                               String moneyType,
                               String source,
                               String gameId,
                               String description,
                               long fee,
                               long txId,
                               TransKind kind) {
        return call(nickname, Math.abs(amount), moneyType, source, gameId, description, fee, txId, kind);
    }

    /**
     * SUN-1306 Phase D — write a 0-exchange ledger row directly to
     * {@code queue_log_money} so losing Lô Đề tickets get a settle-confirmation
     * record in {@code log_money_user_vin}. UserServiceImpl.updateMoney has a
     * short-circuit when {@code money == 0L} that skips the RMQ publish, so
     * we publish ourselves here.
     */
    @Override
    public MoneyResult writeSettleConfirmationRow(String nickname,
                                                    String moneyType,
                                                    String source,
                                                    String gameId,
                                                    String description,
                                                    String actionName,
                                                    long txId) {
        try {
            com.vinplay.vbee.common.models.cache.UserCacheModel u = userService.getUser(nickname);
            long currentBalance = userService.getMoneyUserCache(nickname, moneyType);
            com.vinplay.vbee.common.messages.LogMoneyUserMessage settleLog =
                    new com.vinplay.vbee.common.messages.LogMoneyUserMessage(
                            u != null ? u.getId() : 0,
                            nickname,
                            actionName,
                            source,
                            currentBalance,
                            0L,
                            moneyType,
                            description,
                            0L,
                            false,
                            false);
            com.vinplay.vbee.common.messagebus.MessageBusFactory.get("queue_log_money")
                    .publish("queue_log_money", settleLog, 601);
            return MoneyResult.ok(currentBalance);
        } catch (Throwable t) {
            LOG.warn("JdbcLotteryWalletPort.writeSettleConfirmationRow failed user={} src={} txId={}",
                    nickname, source, txId, t);
            return MoneyResult.fail("0001");
        }
    }

    /** Translate engine {@link TransKind} → legacy {@link TransType}. */
    static TransType mapTransType(TransKind kind) {
        if (kind == null) {
            return TransType.START_TRANS;
        }
        switch (kind) {
            case START: return TransType.START_TRANS;
            case IN:    return TransType.IN_TRANS;
            case END:   return TransType.END_TRANS;
            default:    return TransType.START_TRANS;
        }
    }

    private MoneyResult call(String nickname,
                              long signedAmount,
                              String moneyType,
                              String source,
                              String gameId,
                              String description,
                              long fee,
                              long txId,
                              TransKind kind) {
        try {
            MoneyResponse res = userService.updateMoney(
                nickname,
                signedAmount,
                moneyType,
                source,
                gameId,
                description,
                fee,
                txId,
                mapTransType(kind));
            if (res == null) {
                return MoneyResult.fail("0001");
            }
            if (!res.isSuccess()) {
                // Legacy MoneyResponse errorCode "1003" == insufficient funds.
                // Map to engine "0003" so BetAcceptor surfaces the right code.
                String legacyCode = res.getErrorCode();
                if ("1003".equals(legacyCode)) {
                    return MoneyResult.fail("0003");
                }
                return MoneyResult.fail("0001");
            }
            return MoneyResult.ok(res.getCurrentMoney());
        } catch (Throwable t) {
            LOG.warn("JdbcLotteryWalletPort failed user={} signedAmount={} moneyType={} src={} txId={}",
                nickname, signedAmount, moneyType, source, txId, t);
            return MoneyResult.fail("0001");
        }
    }
}
