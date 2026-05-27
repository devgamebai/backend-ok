package com.sunwinkr.minigame.engine.bet;

import com.sunwinkr.minigame.engine.core.RevealPhase;
import com.sunwinkr.minigame.engine.core.TaiXiuRound;
import com.sunwinkr.minigame.engine.port.BetRecorder;
import com.sunwinkr.minigame.engine.port.MoneyResult;
import com.sunwinkr.minigame.engine.port.TransKind;
import com.sunwinkr.minigame.engine.port.WalletPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Engine-side bet acceptance entry point. Reproduces the legacy logic
 * of {@code MGRoomTaiXiu.betTaiXiu} (TXR:388-503) without the BitZero
 * coupling. Plan §2.2 row B1.
 *
 * <h3>Behavior preservation</h3>
 * Error-code priority (TXR:405-498) — codes are checked in this order:
 * <ol>
 *   <li>{@code 2} — round not OPEN ({@link RevealPhase#acceptsBets()})</li>
 *   <li>{@code 4} — {@link #MIN_BET} guard (TXR:407, INV-13)</li>
 *   <li>{@code 3} — insufficient balance (TXR:408)</li>
 *   <li>{@code 5} — cross-side bet attempt (TXR:413-414, INV-4)</li>
 *   <li>wallet debit attempt (TXR:432)</li>
 *   <li>race re-check after debit (TXR:437) — if {@code enableBetting}
 *       flipped false mid-call, refund via {@link WalletPort#credit}
 *       and return code {@code 1}</li>
 *   <li>{@code 0} — pot append, history record, success</li>
 * </ol>
 *
 * <h3>TaiXiu bot wallet quirk (preserved)</h3>
 * TXR:431 skips the wallet debit when {@code !isBot || isLivestream}.
 * Sicbo (SBR:570) always debits. This inconsistency is preserved
 * here as an AMBIGUOUS quirk per the rules-spec.
 *
 * <p>TODO(SUN-WALLET-INCONS): TaiXiu bots SKIP wallet debit, Sicbo
 * bots always debit. Resolve in a follow-up MR after extraction lands.
 */
public final class BetAcceptor {

    private static final Logger LOG = LoggerFactory.getLogger(BetAcceptor.class);

    /** Minimum bet value (MC:30 — {@code MIN_BET_TAI_XIU_VALUE}). */
    public static final long MIN_BET = 100L;

    /** Description tag for the {@code money_gateway_log} source on bet debit. */
    private static final String SRC_BET = "TaiXiu";

    /** Description tag for the race-refund credit (TXR:440). */
    private static final String SRC_REFUND = "TaiXiuHoanTien";

    /** Legacy {@code Games.TAI_XIU.getId()} — wired here as a constant. */
    private static final long GAME_ID_TAI_XIU = 2L;

    /**
     * Accept (or reject) a bet.
     *
     * @param req      validated bet request
     * @param round    target round; phase + ledger live here
     * @param ledger   per-side accumulator
     * @param wallet   adapter to the user wallet
     * @param recorder adapter for bet-history rows; nullable to allow
     *                 stripped-down deployments
     * @return result with error code (0 = OK) and post-call balance
     */
    public BetAcceptResult accept(BetRequest req,
                                  TaiXiuRound round,
                                  BetLedger ledger,
                                  WalletPort wallet,
                                  BetRecorder recorder) {
        if (req == null) {
            throw new NullPointerException("req");
        }
        if (round == null) {
            throw new NullPointerException("round");
        }
        if (ledger == null) {
            throw new NullPointerException("ledger");
        }
        if (wallet == null) {
            throw new NullPointerException("wallet");
        }

        // Snapshot the user's current wallet balance up-front (TXR:404).
        long currentMoney = wallet.getBalance(req.nickname, req.moneyTypeStr());

        // ---- code 7: bet window closed by wall-clock (SUN-1339 §A2) ----
        // Timestamp guard runs BEFORE the phase check so FE clock skew
        // cannot bypass the server-enforced deadline.
        // closesAt==0 means the round has not been started yet (constructor
        // default) or lockBetting() was called — in the lockBetting() case
        // the phase check below (code 2) also fires, but we prefer code 7
        // so callers get a consistent error. We skip the guard when closesAt==0
        // AND phase==OPEN (pre-start corner case) to avoid rejecting bets on
        // a freshly constructed but not-yet-started round.
        long closesAt = round.bettingClosesAt();
        if (closesAt > 0L && System.currentTimeMillis() >= closesAt) {
            return BetAcceptResult.error(7, currentMoney);
        }

        // ---- code 2: round not OPEN ---------------------------------
        // Legacy code (TXR:405-406) gates the entire bet flow on
        // enableBetting; if false, the result stays at its initialized
        // value of 2.
        if (!round.phase().acceptsBets()) {
            return BetAcceptResult.error(2, currentMoney);
        }

        // ---- code 4: below MIN_BET (INV-13) -------------------------
        if (req.betValue < MIN_BET) {
            return BetAcceptResult.error(4, currentMoney);
        }

        // ---- code 3: insufficient balance (TXR:408) -----------------
        if (req.betValue > currentMoney) {
            return BetAcceptResult.error(3, currentMoney);
        }

        // ---- code 5: cross-side attempt (TXR:413-414, INV-4) --------
        if (ledger.userHasBetOpposite(req.nickname, req.betSide)) {
            return BetAcceptResult.error(5, currentMoney);
        }

        // ---- wallet debit (TXR:432) ---------------------------------
        long perBetTxId = TxIdGenerator.nextBetTxId(round.referenceId());

        TransactionTaiXiuDetail trans = new TransactionTaiXiuDetail(
            round.referenceId(),
            req.userId,
            req.nickname,
            req.betValue,
            req.betSide,
            req.inputTime,
            req.moneyType,
            currentMoney,
            perBetTxId);

        boolean shouldDebit = shouldDebitWallet(req);
        MoneyResult debitRes;
        if (shouldDebit) {
            debitRes = wallet.debit(
                req.nickname,
                req.betValue,
                req.moneyTypeStr(),
                SRC_BET,
                GAME_ID_TAI_XIU,
                buildBetDescription(round.referenceId(), req),
                0L,
                perBetTxId,
                TransKind.START);
        } else {
            // Bot path that skips the wallet (TXR:431) — synthesize a
            // successful response so the downstream code path is
            // identical to a real-player debit.
            debitRes = MoneyResult.success(currentMoney);
        }

        if (!debitRes.isSuccess()) {
            // TXR:491 — wallet failure → code 1.
            return BetAcceptResult.error(1, currentMoney);
        }

        // ---- race re-check (TXR:437) --------------------------------
        if (!round.phase().acceptsBets()) {
            // Mid-call disable: roll back the debit via TaiXiuHoanTien
            // credit, reusing the SAME perBetTxId so audit correlates
            // the rollback to its bet.
            if (shouldDebit) {
                try {
                    wallet.credit(
                        req.nickname,
                        req.betValue,
                        req.moneyTypeStr(),
                        SRC_REFUND,
                        GAME_ID_TAI_XIU,
                        buildRefundDescription(round.referenceId()),
                        0L,
                        perBetTxId,
                        TransKind.END);
                } catch (Throwable t) {
                    LOG.warn("Auto-refund credit failed for user=" + req.nickname
                        + " ref=" + round.referenceId() + " txId=" + perBetTxId, t);
                }
            }
            return BetAcceptResult.error(1, debitRes.getCurrentMoney(), perBetTxId);
        }

        // ---- pot append (TXR:465-468) -------------------------------
        // The non-bot resolution path mirrors TXR:443 — livestream
        // accounts route through the bot branch in the pot, real players
        // route through the real branch. We do NOT recompute isBot
        // here; the caller supplies the authoritative flag.
        boolean isBotForPot = req.isLivestream || req.isBot;
        if (isBotForPot) {
            ledger.addBot(req.betSide, trans);
        } else {
            ledger.addReal(req.betSide, trans);
        }

        long postBalance = debitRes.getCurrentMoney();

        // ---- history row (TXR:477-488) ------------------------------
        if (!req.isBot && recorder != null) {
            try {
                recorder.recordBet(new BetRecorder.BetRecord(
                    round.referenceId(),
                    req.nickname,
                    req.inputTime,
                    req.betSide,
                    req.betValue,
                    postBalance,
                    req.moneyType));
            } catch (Throwable t) {
                // Per SUN-1xxx: do NOT silently swallow. Surface and continue.
                LOG.warn("BetRecorder.recordBet failed for user=" + req.nickname
                    + " ref=" + round.referenceId() + " betValue=" + req.betValue
                    + " betSide=" + req.betSide, t);
            }
        }

        return BetAcceptResult.ok(postBalance, perBetTxId, trans);
    }

    /**
     * TaiXiu-specific bot wallet routing (TXR:431). Real players AND
     * livestream-routed accounts always go through the wallet; pure
     * bot accounts skip it.
     *
     * <p>TODO(SUN-WALLET-INCONS): Sicbo path always debits — preserve
     * for now, resolve in follow-up.
     */
    static boolean shouldDebitWallet(BetRequest req) {
        return !req.isBot || req.isLivestream;
    }

    private static String buildBetDescription(long refId, BetRequest req) {
        return "TaiXiu bet refId=" + refId
            + " inputTime=" + req.inputTime
            + " betSide=" + req.betSide;
    }

    private static String buildRefundDescription(long refId) {
        return "TaiXiu auto-refund refId=" + refId;
    }
}
