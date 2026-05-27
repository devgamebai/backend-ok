package com.sunwinkr.minigame.api.adapter.sicbo;

import com.sunwinkr.minigame.engine.port.MoneyResult;
import com.sunwinkr.minigame.engine.port.TransKind;
import com.sunwinkr.minigame.engine.port.WalletPort;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Sicbo wallet adapter binding the engine {@link WalletPort} to the
 * legacy {@link UserServiceImpl#updateMoney} API.
 *
 * <h3>Sicbo-specific behaviour</h3>
 * <ul>
 *   <li>Bots ALWAYS debit (SUN-880, SBR:568-577) — distinct from TaiXiu
 *       which skips wallet debit for bots. The adapter does not enforce
 *       this; the engine's {@code SicboBetService} unconditionally calls
 *       {@code debit()} on every accepted bet regardless of {@code isBot}.</li>
 *   <li>{@code source = "Sicbo"} for bet debit, {@code source =
 *       "SicboHoanTien"} for race-refunds — matches SBR:574 / 1395.</li>
 * </ul>
 *
 * <p>The {@code @Component("sicboWalletPort")} qualifier lets the Sicbo
 * engine config inject this bean specifically (TaiXiu uses the plain
 * {@code JdbcWalletPort} bean).
 *
 * <p>Plan §6 / B4-B5 (Sicbo analog).
 */
@Component("sicboWalletPort")
public class JdbcSicboWalletPort implements WalletPort {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcSicboWalletPort.class);

    private final UserService userService;

    /** Default ctor uses {@code UserServiceImpl} singleton-style instantiation. */
    public JdbcSicboWalletPort() {
        this(new UserServiceImpl());
    }

    /** Test seam — supply a stub UserService. */
    public JdbcSicboWalletPort(UserService userService) {
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
            case START:
            case BET_DEBIT:   return TransType.START_TRANS;
            case IN:          return TransType.IN_TRANS;
            case END:
            case REFUND_CREDIT: return TransType.END_TRANS;
            default:          return TransType.START_TRANS;
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
            LOG.warn("JdbcSicboWalletPort updateMoney failed user=" + user
                + " amount=" + signedAmount + " moneyType=" + moneyType
                + " src=" + source + " txId=" + txId, t);
            return MoneyResult.failure(getBalance(user, moneyType), "1099");
        }
    }
}
