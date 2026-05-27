package com.sunwinkr.minigame.engine.sicbo.bet;

import com.sunwinkr.minigame.engine.port.BetRecorder;
import com.sunwinkr.minigame.engine.port.MoneyResult;
import com.sunwinkr.minigame.engine.port.TransKind;
import com.sunwinkr.minigame.engine.port.WalletPort;
import com.sunwinkr.minigame.engine.sicbo.core.SicboRound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bet-acceptance service for Sicbo. Stateless — all mutable state lives in
 * {@link SicboPotState} and {@link SicboTxIdGenerator}, which are supplied
 * by the caller (one instance per round).
 *
 * <h3>Accept logic — mirrors {@code MGRoomSicbo.betTaiXiu(String,...)}
 * (SBR:526-638)</h3>
 * <ol>
 *   <li>Pre-check {@code round.isBetting()} → {@code false} → code 2</li>
 *   <li>Decode {@code betSideName} via {@link SicboBetType#byName} →
 *       unknown name → code 6 (replaces legacy NPE — AMBIGUOUS #6)</li>
 *   <li>Min-bet check: {@code betValue >= 100L} → {@code false} → code 4
 *       (INV-13, SBR:545)</li>
 *   <li>Read pre-debit balance from {@link WalletPort#getBalance}; if
 *       {@code betValue > balance} → code 3 (mirrors SBR:546)</li>
 *   <li><b>PRESERVED-DEAD-CODE (AMBIGUOUS #3)</b>: cross-side guard copied
 *       verbatim from SBR:551. The condition {@code betSide==0 || betSide==1}
 *       can NEVER match PotSicbo IDs (1..52). Sicbo intentionally allows
 *       multi-side bets per user. DO NOT "fix" this — it is an intentional
 *       no-op preserved for source fidelity. See AMBIGUOUS #3 in
 *       {@code docs/plans/sicbo-extraction-plan.md §2.3}.</li>
 *   <li>Wallet debit via {@link WalletPort#debit}. Bots ALWAYS debit
 *       (SUN-880, SBR:568-577) — distinct from TaiXiu (AMBIGUOUS #6 /
 *       INV-22 inverse). On failure → code 3 if insufficient, else code 1.</li>
 *   <li>Race re-check: re-read {@code round.isBetting()} after debit; if
 *       {@code false} → auto-refund via {@link WalletPort#credit} → code 1</li>
 *   <li>Add entry to {@link SicboPotState}.</li>
 *   <li>Build {@code transactionCode = referenceId + "-" + betIndex}
 *       (INV-21, SBR:413).</li>
 *   <li>Emit to {@link BetRecorder} (fire-and-forget; exceptions swallowed
 *       by the adapter).</li>
 *   <li>Return {@link SicboBetAcceptResult#success}.</li>
 * </ol>
 */
public final class SicboBetService {

    private static final Logger LOG = LoggerFactory.getLogger(SicboBetService.class);

    /** Minimum bet value in VND units (INV-13 / SBR:545). */
    static final long MIN_BET = 100L;

    /**
     * Accept a bet for the given round.
     *
     * @param request    bet parameters
     * @param round      current round (queried for isBetting() state)
     * @param txGen      per-round TX ID generator (shared across all bets in the round)
     * @param pot        per-round pot accumulator
     * @param wallet     wallet port for debit / refund / balance queries
     * @param recorder   bet-log recorder (fire-and-forget)
     * @return result carrying error code + post-debit balance + txId + txCode
     */
    public SicboBetAcceptResult accept(SicboBetRequest request,
                                       SicboRound round,
                                       SicboTxIdGenerator txGen,
                                       SicboPotState pot,
                                       WalletPort wallet,
                                       BetRecorder recorder) {

        // Step 1 — pre-check betting window
        if (!round.isBetting()) {
            return SicboBetAcceptResult.error(2);
        }

        // Step 2 — decode bet side name (AMBIGUOUS #6: replace NPE with code 6)
        SicboBetType betType;
        try {
            betType = SicboBetType.byName(request.betSideName);
        } catch (IllegalArgumentException e) {
            return SicboBetAcceptResult.error(6);
        }
        short betSideId = (short) betType.getId();

        // Step 3 — minimum bet (INV-13 / SBR:545)
        if (request.betValue < MIN_BET) {
            return SicboBetAcceptResult.error(4);
        }

        // Step 4 — read pre-debit balance to catch insufficient-funds early
        long currentMoney = wallet.getBalance(request.nickname, moneyTypeStr(request.moneyType));
        if (request.betValue > currentMoney) {
            return SicboBetAcceptResult.error(3);
        }

        // ---- code 7: bet window closed by wall-clock (SUN-1339 §A3) ----
        // Timestamp guard runs AFTER balance read (so we carry currentMoney)
        // but BEFORE the debit, so no refund path is needed.
        // closesAt==0 means lockBetting() was called — in that case the phase
        // check above (code 2) may also fire on the next bet, but we prefer
        // code 7 for clock-based rejection consistency.
        // We skip the guard when closesAt==0 AND phase==OPEN (pre-start corner
        // case) to avoid rejecting bets on a freshly constructed round.
        long closesAt = round.bettingClosesAt();
        if (closesAt > 0L && System.currentTimeMillis() >= closesAt) {
            return SicboBetAcceptResult.error(7, currentMoney);
        }

        // -----------------------------------------------------------------------
        // PRESERVED-DEAD-CODE (AMBIGUOUS #3): cross-side guard verbatim from SBR:551.
        //
        // Original condition:
        //   betSide == 1 && potXiu.getTotalBetByUsername(nickname) > 0
        //   || betSide == 0 && potTai.getTotalBetByUsername(nickname) > 0
        //
        // This condition NEVER matches because PotSicbo IDs are 1..52;
        // betSideId is never 0 or 1 in a valid Sicbo bet type context
        // (TAI=48, XIU=49). Sicbo intentionally allows multi-side bets
        // per user — the cross-side restriction exists only in TaiXiu.
        //
        // DO NOT remove or "fix" this block. It is preserved for source
        // fidelity and to document that the behaviour (multi-side allowed)
        // is intentional. Future contributors: see sicbo-extraction-plan.md
        // §2.3 AMBIGUOUS #3 before touching this.
        if (betSideId == 1 && getPotXiuTotalByUsername(pot, request.nickname) > 0L
                || betSideId == 0 && getPotTaiTotalByUsername(pot, request.nickname) > 0L) {
            return SicboBetAcceptResult.error(5);
        }
        // -----------------------------------------------------------------------

        // Step 5 — wallet debit (bots ALWAYS debit — SUN-880, AMBIGUOUS #6 / INV-22)
        long perBetTxId = txGen.next();
        MoneyResult debitResult = wallet.debit(
            request.nickname,
            request.betValue,
            moneyTypeStr(request.moneyType),
            perBetTxId,
            TransKind.BET_DEBIT);

        if (!debitResult.isSuccess()) {
            // Distinguish insufficient (errorCode 3 from adapter) vs other
            if (debitResult.getErrorCodeInt() == 3) {
                return SicboBetAcceptResult.error(3);
            }
            return SicboBetAcceptResult.error(1);
        }

        currentMoney = debitResult.getCurrentMoney();

        // Step 6 — race re-check: round may have closed between pre-check and debit
        if (!round.isBetting()) {
            // Auto-refund
            long refundTxId = txGen.next();
            try {
                wallet.credit(request.nickname, request.betValue,
                              moneyTypeStr(request.moneyType), refundTxId, TransKind.REFUND_CREDIT);
            } catch (Throwable t) {
                LOG.error("SicboBetService: race-refund failed for nickname={} betValue={} perBetTxId={}",
                          request.nickname, request.betValue, perBetTxId, t);
            }
            return SicboBetAcceptResult.error(1);
        }

        // Step 7 — build bet index and transaction code (INV-21 / SBR:413)
        // betIndex incremented in caller (MGRoomSicbo.betTaiXiu SBR:410); here
        // we use pot.size() + 1 (pre-add) to reproduce "betIndex after increment"
        int betIndex = pot.size() + 1;
        String transactionCode = round.getReferenceId() + "-" + betIndex;

        // Step 8 — add to pot
        SicboPotEntry entry = new SicboPotEntry(
            request.nickname, request.userId, request.betValue, betSideId,
            request.inputTime, request.moneyType, perBetTxId, transactionCode,
            request.isBot, currentMoney);
        pot.addBet(entry, request.isBot);

        // Step 9 — record to Mongo (fire-and-forget; adapter swallows exceptions)
        try {
            recorder.record(round.getReferenceId(), request.nickname, request.betValue,
                            request.inputTime, betSideId, request.moneyType);
        } catch (Throwable t) {
            LOG.warn("SicboBetService: recorder.record failed for nickname={} refId={}",
                     request.nickname, round.getReferenceId(), t);
        }

        return SicboBetAcceptResult.success(currentMoney, perBetTxId, transactionCode, betSideId);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Convert numeric moneyType to the string key used by WalletPort.
     * 1 = "vin", anything else = "xu".
     */
    private static String moneyTypeStr(short moneyType) {
        return moneyType == 1 ? "vin" : "xu";
    }

    /**
     * PRESERVED-DEAD-CODE helper for AMBIGUOUS #3 cross-side guard.
     * Returns 0L always because betSideId 0 (legacy potXiu side) never
     * appears in the SicboBetType enum (XIU = id 49). Kept to make the
     * dead-code guard above compile without a real pot-partition.
     */
    private static long getPotXiuTotalByUsername(SicboPotState pot, String nickname) {
        // Dead-code path: PotSicbo ID 0 never matches any SicboBetType
        return 0L;
    }

    /**
     * PRESERVED-DEAD-CODE helper for AMBIGUOUS #3 cross-side guard.
     * Returns 0L always because betSideId 1 (legacy potTai side) never
     * appears in the SicboBetType enum (TAI = id 48). Kept to make the
     * dead-code guard above compile without a real pot-partition.
     */
    private static long getPotTaiTotalByUsername(SicboPotState pot, String nickname) {
        // Dead-code path: PotSicbo ID 1 never matches any SicboBetType
        return 0L;
    }
}
