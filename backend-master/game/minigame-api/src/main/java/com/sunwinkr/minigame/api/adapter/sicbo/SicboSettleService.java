package com.sunwinkr.minigame.api.adapter.sicbo;

import com.sunwinkr.minigame.engine.port.TransKind;
import com.sunwinkr.minigame.engine.port.WalletPort;
import com.sunwinkr.minigame.engine.sicbo.prize.SicboPrizePerBet;
import com.sunwinkr.minigame.engine.sicbo.prize.SicboSettleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Settle-idempotency service for Sicbo (SUN-1339 §B1).
 *
 * <h3>Settle path</h3>
 * For each bet in {@link SicboSettleResult#perBet}:
 * <ol>
 *   <li>Call {@link JdbcSicboBetStore#markSettled} with the gross prize.
 *       The UPDATE is guarded by {@code WHERE settle_status='PENDING'}, so
 *       if the row is already SETTLED the update returns 0 rows and we log
 *       + skip the wallet credit — full idempotency.</li>
 *   <li>If markSettled returns {@code true} (new settle), credit the user's
 *       wallet via {@link WalletPort#credit} with
 *       {@code source="Sicbo"}, {@code gameId="Xí Ngầu"} semantics
 *       (5-arg overload resolves to 9-arg via interface default).</li>
 * </ol>
 *
 * <h3>Duplicate settle protection</h3>
 * The composite UNIQUE key {@code uk_sicbo_bet_round_ticket(round_id, id)}
 * added in migration A1 provides a second line of defence at the DB layer.
 * The service guard above is the first line and prevents double-crediting
 * the wallet even when the row update races.
 *
 * <p>Plan SUN-1339 §B1.
 */
@Service
public class SicboSettleService {

    private static final Logger LOG = LoggerFactory.getLogger(SicboSettleService.class);

    private final JdbcSicboBetStore betStore;
    private final WalletPort wallet;
    private final LegacySicboHistoryPort legacyHistory;

    public SicboSettleService(JdbcSicboBetStore betStore,
                               @Qualifier("sicboWalletPort") WalletPort wallet,
                               LegacySicboHistoryPort legacyHistory) {
        this.betStore       = betStore;
        this.wallet         = wallet;
        this.legacyHistory  = legacyHistory;
    }

    /**
     * Settle all bets in {@code result} for round {@code roundId}.
     *
     * <p>Each per-bet slot is settled independently — one failure does not
     * abort the rest. Errors are logged at ERROR level but not rethrown.
     *
     * @param roundId the canonical round identifier (written to sicbo_bet.round_id at insert time)
     * @param result  the prize breakdown from {@link com.sunwinkr.minigame.engine.sicbo.prize.SicboPrizeCalculator}
     */
    public void settle(long roundId, SicboSettleResult result) {
        if (result == null || result.perBet == null) {
            LOG.warn("SicboSettleService.settle: null result for roundId={}", roundId);
            return;
        }
        List<SicboPrizePerBet> perBet = result.perBet;
        LOG.info("SicboSettleService.settle: roundId={} betCount={} totalPayout={} exploitGuard={}",
                 roundId, perBet.size(), result.totalPayout, result.exploitGuardFired);

        for (SicboPrizePerBet entry : perBet) {
            try {
                settleOne(roundId, entry);
            } catch (Throwable t) {
                LOG.error("SicboSettleService.settle: unexpected error settling bet for nickname={} roundId={}",
                          entry.bet.nickname, roundId, t);
            }
        }

        // SUN-1341 F2: mirror settle results into legacy log_sicbo + transaction_tai_xiu_sicbo
        // so c=303 (GameHistoryService.fetchSicbo) surfaces these bets.
        // Failures are warn-logged inside updateAllSettled — never break the wallet path.
        try {
            legacyHistory.updateAllSettled(roundId, perBet);
        } catch (Throwable t) {
            LOG.warn("SicboSettleService.settle: legacyHistory.updateAllSettled failed roundId={}", roundId, t);
        }
    }

    // -----------------------------------------------------------------------
    // Per-bet settle
    // -----------------------------------------------------------------------

    private void settleOne(long roundId, SicboPrizePerBet entry) {
        String nickname  = entry.bet.nickname;
        long   betValue  = entry.bet.betValue;
        long   perBetTxId = entry.bet.perBetTxId;
        String moneyType = entry.bet.moneyType == 1 ? "vin" : "xu";

        if (entry.refund) {
            // Exploit-guard path: refund full betValue
            boolean marked = betStore.markSettled(perBetTxId, 0L);
            if (!marked) {
                LOG.warn("SicboSettleService.settleOne: already settled (refund) nickname={} txId={}", nickname, perBetTxId);
                return;
            }
            try {
                wallet.credit(nickname, betValue, moneyType, perBetTxId, TransKind.REFUND_CREDIT);
                LOG.info("SicboSettleService.settleOne: refund credited nickname={} betValue={} txId={}",
                         nickname, betValue, perBetTxId);
            } catch (Throwable t) {
                LOG.error("SicboSettleService.settleOne: refund credit FAILED nickname={} betValue={} txId={}",
                          nickname, betValue, perBetTxId, t);
            }
            return;
        }

        long grossPrize = entry.prize; // 0 for losers
        boolean marked = betStore.markSettled(perBetTxId, grossPrize);
        if (!marked) {
            // Already SETTLED — idempotency: skip wallet credit
            LOG.warn("SicboSettleService.settleOne: already settled nickname={} txId={} — skipping wallet credit",
                     nickname, perBetTxId);
            return;
        }

        if (grossPrize > 0L) {
            try {
                wallet.credit(nickname, grossPrize, moneyType, perBetTxId, TransKind.END);
                LOG.info("SicboSettleService.settleOne: prize credited nickname={} prize={} fee={} txId={}",
                         nickname, grossPrize, entry.fee, perBetTxId);
            } catch (Throwable t) {
                LOG.error("SicboSettleService.settleOne: prize credit FAILED nickname={} prize={} txId={}",
                          nickname, grossPrize, perBetTxId, t);
            }
        }
        // Losers: betStore row is SETTLED with prize=0, no credit needed
    }
}
