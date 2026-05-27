package com.sunwinkr.lottery.engine.bet;

import com.sunwinkr.lottery.engine.clock.LotteryClock;
import com.sunwinkr.lottery.engine.model.LotteryMode;
import com.sunwinkr.lottery.engine.model.LotteryTicket;
import com.sunwinkr.lottery.engine.port.BetStore;
import com.sunwinkr.lottery.engine.port.MoneyResult;
import com.sunwinkr.lottery.engine.port.SettledFlagStore;
import com.sunwinkr.lottery.engine.port.TransKind;
import com.sunwinkr.lottery.engine.port.WalletPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Bet-path orchestrator — replaces {@code LotteryModule.buyTicket}
 * (JLM:206-236).
 *
 * <p>Order (every step is a precondition for the next):
 * <ol>
 *   <li>{@link BetValidator#validate} — mode known, ticket shape OK,
 *       bet ≥ {@link BetValidator#MIN_BET} (else
 *       {@code 0004/0005})</li>
 *   <li>{@link LotteryClock#isBettingOpen} — bet window open (else
 *       {@code 0002 locked})</li>
 *   <li>{@link BetSnapshot#of} — SUN-1295 snapshot of rate +
 *       prizeMultiplier</li>
 *   <li>{@link WalletPort#debit} — pay the wager (else
 *       {@code 0001/0003})</li>
 *   <li>{@link BetStore#insert} — persist the row with snapshot</li>
 * </ol>
 *
 * <p>If step (5) fails after step (4) succeeded, the ticket is lost
 * but the player was charged. Adapter (PR-3) is expected to retry the
 * insert against a unique-by-(user, clientNonce) constraint — this
 * engine class returns {@code 0001} on insert failure so the caller
 * can take corrective action.
 */
public final class BetAcceptor {

    private static final Logger log = LoggerFactory.getLogger(BetAcceptor.class);

    private final WalletPort walletPort;
    private final BetStore betStore;
    private final SettledFlagStore settledFlagStore;
    private final Clock clock;
    private final String moneyType;

    public BetAcceptor(WalletPort walletPort,
                       BetStore betStore,
                       SettledFlagStore settledFlagStore,
                       Clock clock) {
        this(walletPort, betStore, settledFlagStore, clock, "vin");
    }

    public BetAcceptor(WalletPort walletPort,
                       BetStore betStore,
                       SettledFlagStore settledFlagStore,
                       Clock clock,
                       String moneyType) {
        this.walletPort = walletPort;
        this.betStore = betStore;
        this.settledFlagStore = settledFlagStore;
        this.clock = clock;
        this.moneyType = moneyType;
    }

    /**
     * Accept (or reject) a bet. Pure orchestration — every behaviour
     * branch is encoded in the returned {@link BetAcceptResult} code.
     * No exceptions thrown to callers.
     */
    public BetAcceptResult accept(BetRequest req) {
        // (1) shape + min-bet + known-mode
        try {
            BetValidator.validate(req);
        } catch (BetValidator.InvalidBetException e) {
            log.debug("Bet rejected for {}: {}", req.getNickname(), e.getMessage());
            if ("0004".equals(e.getErrorCode())) return BetAcceptResult.unknownMode();
            // SUN-1366 — preserve 0006 (per-number cap exceeded) instead of
            // collapsing into the generic 0005 shape-error code.
            if ("0006".equals(e.getErrorCode())) return BetAcceptResult.betExceedsCap();
            return BetAcceptResult.invalidNumber();
        }

        // (2) bet-window gate — combined Hanoi clock + day-settle flag
        LocalDate vnToday = LocalDate.now(clock.withZone(LotteryClock.VN));
        boolean settled = settledFlagStore.isSettled(vnToday);
        if (!LotteryClock.isBettingOpen(clock, settled)) {
            return BetAcceptResult.locked();
        }

        // (3) SUN-1295 snapshot
        LotteryMode mode = LotteryMode.byId(req.getModeId()).orElse(null);
        if (mode == null) {
            // Defence-in-depth — validator already filtered, but guard the cast.
            return BetAcceptResult.unknownMode();
        }
        BetSnapshot snap = BetSnapshot.of(mode);
        long userBet = req.getBetValue();
        long finalBetValue = userBet * (long) snap.getRateAtPurchase();

        // (4) wallet debit
        MoneyResult debit = walletPort.debit(
                req.getNickname(),
                finalBetValue,
                moneyType,
                "LoDe",
                "Lô Đề",
                "Cược " + req.getTicket() + "\n " + mode.getName(),
                0L,
                System.currentTimeMillis(),
                TransKind.START);
        if (!debit.isSuccess()) {
            if ("0003".equals(debit.getErrorCode())) {
                return BetAcceptResult.insufficientFunds(debit.getCurrentMoney());
            }
            return BetAcceptResult.walletRejected();
        }

        // (5) persist the ticket with SUN-1295 snapshot stamped on the row
        LotteryTicket pending = new LotteryTicket(
                null,
                req.getUserId(),
                req.getNickname(),
                finalBetValue,
                req.getModeId(),
                req.getTicket(),
                null,
                LocalDateTime.now(clock.withZone(LotteryClock.VN)),
                null,
                userBet,
                snap.getRateAtPurchase(),
                snap.getPrizeMultiplierAtPurchase());
        try {
            LotteryTicket stored = betStore.insert(pending);
            return BetAcceptResult.ok(
                    debit.getCurrentMoney(),
                    stored.getTicketId() == null ? -1L : stored.getTicketId());
        } catch (RuntimeException e) {
            log.warn("Bet insert failed after wallet debit for {}", req.getNickname(), e);
            return BetAcceptResult.walletRejected();
        }
    }
}
