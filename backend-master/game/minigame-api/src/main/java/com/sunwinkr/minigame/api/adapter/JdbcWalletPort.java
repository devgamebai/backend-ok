package com.sunwinkr.minigame.api.adapter;

import com.sunwinkr.minigame.engine.port.MoneyResult;
import com.sunwinkr.minigame.engine.port.TransKind;
import com.sunwinkr.minigame.engine.port.WalletPort;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.statics.TransType;
import com.vinplay.vbee.common.response.MoneyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Adapter binding the engine {@link WalletPort} to the legacy
 * {@link UserServiceImpl#updateMoney} API (TXR:432, TXR:1301/1321/1337).
 *
 * <h3>Behavior preservation</h3>
 * <ul>
 *   <li>Debit calls negate {@code amount} before delegating — legacy
 *       {@code updateMoney(... long money ...)} uses sign to encode
 *       direction.</li>
 *   <li>{@link TransKind} maps 1:1 to the legacy
 *       {@link TransType} enum:
 *       START→{@code START_TRANS}, IN→{@code IN_TRANS}, END→{@code END_TRANS}.</li>
 *   <li>{@code MoneyResponse.isSuccess()} propagates as
 *       {@link MoneyResult#isSuccess()}; failure flows include the
 *       backend error code so the bridge can surface code=1 to clients.</li>
 * </ul>
 *
 * <p>Plan §4.1.
 *
 * <p>{@code @Primary}: resolves NoUniqueBeanDefinitionException when the
 * stale compiled EngineConfig {@code walletPort} @Bean wrapper coexists
 * with this @Component. This is the canonical WalletPort.
 */
@Primary
@Component
public class JdbcWalletPort implements WalletPort {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcWalletPort.class);

    private final UserService userService;

    /** Default ctor uses {@code UserServiceImpl} singleton-style instantiation. */
    public JdbcWalletPort() {
        this(new UserServiceImpl());
    }

    /** Test seam — supply a stub UserService. */
    public JdbcWalletPort(UserService userService) {
        this.userService = userService;
    }

    @Override
    public MoneyResult debit(String user,
                              long amount,
                              String moneyType,
                              String source,
                              long gameId,
                              String desc,
                              long fee,
                              long txId,
                              TransKind transKind) {
        return updateMoney(user, -Math.abs(amount), moneyType, source, gameId, desc, fee, txId, transKind);
    }

    @Override
    public MoneyResult credit(String user,
                               long amount,
                               String moneyType,
                               String source,
                               long gameId,
                               String desc,
                               long fee,
                               long txId,
                               TransKind transKind) {
        return updateMoney(user, Math.abs(amount), moneyType, source, gameId, desc, fee, txId, transKind);
    }

    @Override
    public long getBalance(String user, String moneyType) {
        try {
            return userService.getMoneyUserCache(user, moneyType);
        } catch (Throwable t) {
            LOG.warn("getBalance failed for user=" + user + " moneyType=" + moneyType, t);
            return 0L;
        }
    }

    /** Translate {@link TransKind} → legacy {@link TransType}. */
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

    private MoneyResult updateMoney(String user,
                                     long signedAmount,
                                     String moneyType,
                                     String source,
                                     long gameId,
                                     String desc,
                                     long fee,
                                     long txId,
                                     TransKind transKind) {
        try {
            MoneyResponse res = userService.updateMoney(
                user,
                signedAmount,
                moneyType,
                source,
                String.valueOf(gameId),
                desc,
                fee,
                txId,
                mapTransType(transKind));
            if (res == null) {
                return MoneyResult.failure(getBalance(user, moneyType), "1001");
            }
            return new MoneyResult(res.isSuccess(), res.getCurrentMoney(), res.getErrorCode());
        } catch (Throwable t) {
            LOG.warn("JdbcWalletPort updateMoney failed user=" + user
                + " amount=" + signedAmount + " moneyType=" + moneyType
                + " src=" + source + " txId=" + txId, t);
            return MoneyResult.failure(getBalance(user, moneyType), "1099");
        }
    }
}
