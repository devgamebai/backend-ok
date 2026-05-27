package com.sunwinkr.minigame.api.scheduler;

import com.sunwinkr.minigame.api.adapter.JdbcTaixiuSchedulerSettlePort;
import com.sunwinkr.minigame.api.adapter.LegacyTaixiuHistoryPort;
import com.sunwinkr.minigame.api.push.TickPublisher;
import com.sunwinkr.minigame.api.scheduler.TaiXiuRoundState.PendingBet;
import com.sunwinkr.minigame.engine.dice.ResultPipeline;
import com.sunwinkr.minigame.engine.dice.RoundContext;
import com.sunwinkr.minigame.engine.port.WalletPort;
import com.sunwinkr.minigame.engine.settle.TaiXiuSettleService;
import com.sunwinkr.minigame.engine.settle.TaiXiuSettleService.BetEntry;
import com.sunwinkr.minigame.engine.settle.TaiXiuSettleService.SettleSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Self-contained TaiXiu round scheduler (SUN-1341 E1).
 *
 * <p>Runs independently of the BitZero {@code game-minigame} container.
 * Only active when {@link #SCHEDULER_ENABLED_ENV} is set to {@code "1"} or
 * {@code "true"}. When the flag is off all three scheduled methods are
 * no-ops so the legacy BitZero path remains authoritative.
 *
 * <h3>Round timing (60-second cadence)</h3>
 * <pre>
 *  0 s  {@link #startNewRound()}     — open betting, increment roundId
 * 30 s  {@link #lockBetting()}       — close bet window
 * 40 s  {@link #revealAndSettle()}   — roll dice, settle, announce
 * 60 s  (next startNewRound fires)
 * </pre>
 *
 * <h3>Feature flag</h3>
 * {@code TAIXIU_SCHEDULER_ENABLED=1} activates this scheduler. When OFF the
 * existing bridge ({@code TaiXiuModuleBridge} + BitZero) remains authoritative.
 *
 * <h3>Relation to MINIGAME_API_BRIDGE_ENABLED</h3>
 * The two flags are independent. The bridge flag controls whether the BitZero
 * wire-protocol path delegates to the Spring engine. This scheduler flag
 * controls whether the Spring layer owns the round clock. They should not
 * both be ON at the same time on the same JVM.
 *
 * <p>Plan SUN-1341 §E1.
 */
@Component
public class TaiXiuRoundScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(TaiXiuRoundScheduler.class);

    /** Env flag — set {@code "1"} to activate standalone scheduler. Default OFF. */
    public static final String SCHEDULER_ENABLED_ENV = "TAIXIU_SCHEDULER_ENABLED";

    /** Betting window: 30 seconds. */
    static final long BETTING_WINDOW_MS = 30_000L;

    private final TaiXiuRoundState state;
    private final ResultPipeline resultPipeline;
    private final WalletPort walletPort;
    private final JdbcTaixiuSchedulerSettlePort settlePort;
    private final SimpMessagingTemplate broker;
    /** Legacy history adapter — writes log_taixiu + transaction_tai_xiu_sicbo. */
    private final LegacyTaixiuHistoryPort legacyHistory;

    /** Settle service — built lazily per round so no stale state leaks. */
    private final TaiXiuSettleService settleService;

    public TaiXiuRoundScheduler(
            TaiXiuRoundState state,
            ResultPipeline resultPipeline,
            @Qualifier("jdbcWalletPort") WalletPort walletPort,
            JdbcTaixiuSchedulerSettlePort settlePort,
            SimpMessagingTemplate broker,
            LegacyTaixiuHistoryPort legacyHistory) {
        this.state          = state;
        this.resultPipeline = resultPipeline;
        this.walletPort     = walletPort;
        this.settlePort     = settlePort;
        this.broker         = broker;
        this.legacyHistory  = legacyHistory;
        this.settleService  = new TaiXiuSettleService(walletPort, settlePort,
                ev -> LOG.error("TaiXiu settle failure roundId={} nickname={} reason={}",
                                ev.roundId, ev.nickname, ev.reason));
    }

    // -----------------------------------------------------------------------
    // Scheduled methods
    // -----------------------------------------------------------------------

    /**
     * Phase 1 — Open a new round. Fires every 60 s starting at JVM boot
     * (initialDelay=0 means the first round opens immediately).
     */
    @Scheduled(fixedRate = 60_000L, initialDelay = 0L)
    public void startNewRound() {
        if (!isEnabled()) {
            return;
        }
        try {
            long roundId = state.openNewRound(BETTING_WINDOW_MS);
            LOG.info("TaiXiuRoundScheduler.startNewRound: roundId={}", roundId);
            publishRoundStart(roundId);
        } catch (Throwable t) {
            LOG.error("TaiXiuRoundScheduler.startNewRound failed", t);
        }
    }

    /**
     * Phase 2 — Lock the bet window. Fires 30 s after startNewRound.
     */
    @Scheduled(fixedRate = 60_000L, initialDelay = 30_000L)
    public void lockBetting() {
        if (!isEnabled()) {
            return;
        }
        try {
            state.lockBetting();
            LOG.info("TaiXiuRoundScheduler.lockBetting: roundId={}", state.getRoundId());
            publishLocked(state.getRoundId());
        } catch (Throwable t) {
            LOG.error("TaiXiuRoundScheduler.lockBetting failed", t);
        }
    }

    /**
     * Phase 3 — Roll dice, settle bets, announce result via STOMP.
     * Fires 40 s after startNewRound.
     */
    @Scheduled(fixedRate = 60_000L, initialDelay = 40_000L)
    public void revealAndSettle() {
        if (!isEnabled()) {
            return;
        }
        try {
            long roundId = state.getRoundId();
            if (roundId == 0L) {
                LOG.warn("TaiXiuRoundScheduler.revealAndSettle: roundId=0 — skipping (scheduler not started yet)");
                return;
            }

            // Roll dice via ResultPipeline (honours force-result + RTP house edge).
            // Use RoundContext.of convenience ctor — pot totals not tracked by
            // the standalone scheduler in E1 (no BetLedger), so pass 0/0.
            RoundContext ctx = RoundContext.of(0L, 0L);
            short[] dice = resultPipeline.generate(ctx, 0L, 0L);

            // Publish dice to state so /state endpoint reflects revealed values.
            state.revealDice(dice);

            // Drain bets collected during the open window.
            List<PendingBet> pendingBets = state.drainBets();
            List<BetEntry> entries = toEntries(pendingBets);

            // Settle.
            SettleSummary summary = settleService.settle(roundId, dice, entries);
            LOG.info("TaiXiuRoundScheduler.revealAndSettle: roundId={} dice=[{},{},{}] " +
                     "settled={} failed={} totalCredited={}",
                     roundId, dice[0], dice[1], dice[2],
                     summary.settled, summary.failed, summary.totalCredited);

            // Write legacy history for c=303 visibility (fire-and-forget).
            int diceSum = dice[0] + dice[1] + dice[2];
            short winSide = (diceSum >= 11) ? (short) 1 : (short) 0;
            for (PendingBet pb : pendingBets) {
                try {
                    boolean isWin = (pb.betSide == winSide);
                    long prize = isWin
                            ? com.sunwinkr.minigame.engine.settle.TaiXiuSettleService.computePrize(pb.betValue)
                            : 0L;
                    legacyHistory.recordSettle(
                            roundId,
                            pb.nickname,
                            (int) pb.betSide,
                            (int) pb.moneyType,
                            prize,
                            isWin);
                } catch (Throwable t) {
                    LOG.warn("TaiXiuRoundScheduler: legacyHistory.recordSettle failed " +
                             "roundId={} nickname={}", roundId, pb.nickname, t);
                }
            }

            // Announce via STOMP /topic/taixiu/announce.
            publishAnnounce(roundId, dice, summary);

        } catch (Throwable t) {
            LOG.error("TaiXiuRoundScheduler.revealAndSettle failed", t);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Convert scheduler PendingBet list to engine BetEntry list. */
    static List<BetEntry> toEntries(List<PendingBet> pending) {
        List<BetEntry> out = new ArrayList<>(pending.size());
        for (PendingBet pb : pending) {
            out.add(new BetEntry(
                pb.nickname,
                pb.roundId,
                pb.betValue,
                pb.betSide,
                pb.moneyType,
                pb.perBetTxId));
        }
        return out;
    }

    private void publishRoundStart(long roundId) {
        try {
            Map<String, Object> m = new HashMap<>();
            m.put("event", "ROUND_START");
            m.put("roundId", roundId);
            m.put("safeBetExpiresAt", state.getSafeBetExpiresAt());
            broker.convertAndSend("/topic/taixiu/announce", m);
        } catch (Throwable t) {
            LOG.warn("TaiXiuRoundScheduler.publishRoundStart failed", t);
        }
    }

    private void publishLocked(long roundId) {
        try {
            Map<String, Object> m = new HashMap<>();
            m.put("event", "BETTING_LOCKED");
            m.put("roundId", roundId);
            broker.convertAndSend("/topic/taixiu/announce", m);
        } catch (Throwable t) {
            LOG.warn("TaiXiuRoundScheduler.publishLocked failed", t);
        }
    }

    private void publishAnnounce(long roundId, short[] dice, SettleSummary summary) {
        try {
            int sum = dice[0] + dice[1] + dice[2];
            short result = (sum >= 11) ? (short) 1 : (short) 0;
            Map<String, Object> m = new HashMap<>();
            m.put("event", "ROUND_RESULT");
            m.put("roundId", roundId);
            m.put("dice1", dice[0]);
            m.put("dice2", dice[1]);
            m.put("dice3", dice[2]);
            m.put("sum", sum);
            m.put("result", result); // 1=TAI, 0=XIU
            m.put("settled", summary.settled);
            broker.convertAndSend("/topic/taixiu/announce", m);
        } catch (Throwable t) {
            LOG.warn("TaiXiuRoundScheduler.publishAnnounce failed", t);
        }
    }

    /** True iff the standalone scheduler is active. */
    public static boolean isEnabled() {
        String v = System.getenv(SCHEDULER_ENABLED_ENV);
        return v != null && (v.equals("1") || v.equalsIgnoreCase("true"));
    }
}
