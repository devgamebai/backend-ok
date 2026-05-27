package com.sunwinkr.lottery.engine.settle;

import com.sunwinkr.lottery.engine.clock.LotteryClock;
import com.sunwinkr.lottery.engine.model.LotteryResult;
import com.sunwinkr.lottery.engine.model.LotteryTicket;
import com.sunwinkr.lottery.engine.model.SettleStatus;
import com.sunwinkr.lottery.engine.port.BetStore;
import com.sunwinkr.lottery.engine.port.MoneyResult;
import com.sunwinkr.lottery.engine.port.TransKind;
import com.sunwinkr.lottery.engine.port.WalletPort;
import com.sunwinkr.lottery.engine.prize.PrizeBreakdown;
import com.sunwinkr.lottery.engine.prize.PrizeCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

/**
 * Settle loop — credits winning tickets, marks all pending settled.
 *
 * <p>Ports {@code LotteryModule.getResultLottery} settle body (JLM:121-132)
 * to the engine boundary. Key differences from legacy:
 * <ul>
 *   <li>Per-row try/catch. One ticket failing (wallet down, etc.) does
 *       NOT halt the loop. Emits {@link SettleFailureEvent} for ops.</li>
 *   <li>Closed-form prize call via
 *       {@link PrizeCalculator#calculate(LotteryResult, LotteryTicket)}
 *       — reads SUN-1295 snapshot from the ticket row, never the live
 *       enum.</li>
 *   <li>{@code lode.settled_at} stamped via
 *       {@link BetStore#markSettled} — closes audit L-1
 *       (pre-settle visibility) at the per-row level.</li>
 * </ul>
 *
 * <p>The day-level {@code result_lottery.settled_at} flip is performed
 * by {@link com.sunwinkr.lottery.engine.ingest.DrawIngest} AFTER this
 * service returns — ordering is load-bearing for the bet-gate.
 */
public final class LotterySettleService {

    private static final Logger log = LoggerFactory.getLogger(LotterySettleService.class);

    private final BetStore betStore;
    private final WalletPort walletPort;
    private final Consumer<SettleFailureEvent> failureSink;
    private final String moneyType;

    /**
     * @param failureSink callback for {@link SettleFailureEvent} —
     *                    {@code null} means swallow (tests use this)
     */
    public LotterySettleService(BetStore betStore,
                                WalletPort walletPort,
                                Consumer<SettleFailureEvent> failureSink) {
        this(betStore, walletPort, failureSink, "vin");
    }

    public LotterySettleService(BetStore betStore,
                                WalletPort walletPort,
                                Consumer<SettleFailureEvent> failureSink,
                                String moneyType) {
        this.betStore = betStore;
        this.walletPort = walletPort;
        this.failureSink = failureSink;
        this.moneyType = moneyType;
    }

    /**
     * Settle every pending ticket for the given Vietnam-wall draw date
     * against the given {@code draw} payload. One failure does NOT halt
     * the loop.
     */
    public SettleSummary settleAll(LocalDate vnDate, LotteryResult draw) {
        List<LotteryTicket> pending = betStore.findPendingForDate(vnDate, LotteryClock.LOCK_TIME);
        int settled = 0;
        int failed = 0;
        long totalCredited = 0L;
        for (LotteryTicket t : pending) {
            try {
                // SUN-1339 B1: idempotency guard — skip tickets already SETTLED
                // so re-running settle (e.g. after partial failure) does not
                // re-publish a wallet credit / settle-confirmation row.
                // The (round_id, id) unique key is a DB-level guard; this check
                // avoids the double wallet-credit before we even reach the UPDATE.
                if (SettleStatus.SETTLED.equals(t.getSettleStatus())) {
                    log.info("Settle skip (already SETTLED): ticketId={} user={}", t.getTicketId(), t.getNickname());
                    settled++;
                    continue;
                }
                PrizeBreakdown pb = PrizeCalculator.calculate(draw, t);
                long prize = pb.getPrize();
                // SUN-1306 Phase D: enrich description with mode + ticket so
                // the resulting log_money_user_vin row carries enough detail
                // for the player + agency history endpoints to render.
                com.sunwinkr.lottery.engine.model.LotteryMode mode =
                        com.sunwinkr.lottery.engine.model.LotteryMode.byId(t.getModeId())
                                .orElse(null);
                String modeName = mode != null ? mode.getName() : ("Mode " + t.getModeId());
                String detail = modeName + " — số " + t.getTicket();
                long txId = System.currentTimeMillis();

                if (prize > 0L) {
                    MoneyResult credit = walletPort.credit(
                            t.getNickname(),
                            prize,
                            moneyType,
                            "LoDe",
                            "Lô Đề",
                            "Thắng cược Lô Đề (" + detail + ")",
                            0L,
                            txId,
                            TransKind.START);
                    if (!credit.isSuccess()) {
                        failed++;
                        emit(new SettleFailureEvent(
                                t.getTicketId() == null ? -1 : t.getTicketId(),
                                t.getNickname(),
                                "wallet credit failed: " + credit.getErrorCode()));
                        continue;
                    }
                } else {
                    // SUN-1306 Phase D: ALWAYS write a settle-confirmation row
                    // (money_exchange = 0) so the ledger is symmetric — both
                    // winners and losers have 2 records per ticket.
                    walletPort.writeSettleConfirmationRow(
                            t.getNickname(),
                            moneyType,
                            "LoDe",
                            "Lô Đề",
                            "Thua cược Lô Đề (" + detail + ")",
                            "Kết quả Lô Đề",
                            txId);
                }
                betStore.markSettled(
                        t.getTicketId() == null ? -1L : t.getTicketId(),
                        prize);
                settled++;
                totalCredited += prize;
            } catch (RuntimeException e) {
                failed++;
                log.warn("Settle failed for ticket {}", t.getTicketId(), e);
                emit(new SettleFailureEvent(
                        t.getTicketId() == null ? -1 : t.getTicketId(),
                        t.getNickname(),
                        e.getClass().getSimpleName() + ": " + e.getMessage()));
            }
        }
        return new SettleSummary(settled, failed, totalCredited);
    }

    private void emit(SettleFailureEvent ev) {
        if (failureSink != null) {
            try {
                failureSink.accept(ev);
            } catch (RuntimeException ignore) {
                // Sink failure must not fail the loop.
            }
        }
    }
}
