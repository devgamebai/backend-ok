package com.sunwinkr.lottery.api.wire;

import com.sunwinkr.lottery.engine.bet.BetAcceptResult;
import com.sunwinkr.lottery.engine.bet.BetAcceptor;
import com.sunwinkr.lottery.engine.bet.BetRequest;
import com.sunwinkr.lottery.engine.clock.LotteryClock;
import com.sunwinkr.lottery.engine.port.SettledFlagStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * BitZero ↔ engine adapter for XSMB lottery. Plan §6.
 *
 * <h3>Wire protocol unchanged</h3>
 * The legacy {@code LotteryModule.handleClientRequest} accepts BitZero
 * cmds {@code 30000} (snapshot/state) and {@code 30001} (place ticket).
 * When the bridge is enabled, these cmds dispatch here. This class
 * deliberately avoids any {@code bitzero.*} imports — the legacy module
 * accesses the bridge through reflective bean lookup
 * ({@link com.sunwinkr.lottery.api.LotteryApiApplication#contextHolder()})
 * and passes opaque field arguments. The contract between bridge and
 * legacy module is documented at each method.
 *
 * <h3>Feature flag</h3>
 * {@code LOTTERY_ENGINE_ENABLED=1} flips the legacy {@code
 * handleClientRequest} to delegate to this bridge. Default OFF; legacy
 * path remains authoritative until the cutover gate clears (14 days of
 * clean ghost-mode diff).
 */
@Component
public class LotteryModuleBridge {

    private static final Logger LOG = LoggerFactory.getLogger(LotteryModuleBridge.class);

    /** Env flag — set to "1" to flip the bridge ON. Default OFF. */
    public static final String FLAG_ENV = "LOTTERY_ENGINE_ENABLED";

    private final BetAcceptor acceptor;
    private final SettledFlagStore settledFlag;
    private final Clock clock;

    public LotteryModuleBridge(BetAcceptor acceptor,
                                SettledFlagStore settledFlag,
                                Clock clock) {
        this.acceptor = acceptor;
        this.settledFlag = settledFlag;
        this.clock = clock;
    }

    /** True iff the legacy handler should delegate to this bridge. */
    public static boolean isEnabled() {
        String v = System.getenv(FLAG_ENV);
        return v != null && (v.equals("1") || v.equalsIgnoreCase("true"));
    }

    /**
     * BitZero cmd 30000 — snapshot/state. Returns the engine's clock view
     * so the legacy module can map fields back to its wire response.
     *
     * @param nickname authenticated user nickname (opaque; may be null)
     */
    public BridgeStateView snapshot(String nickname) {
        LocalDate vnToday = LocalDate.now(clock.withZone(LotteryClock.VN));
        boolean todaySettled = settledFlag.isSettled(vnToday);
        boolean open = LotteryClock.isBettingOpen(clock, todaySettled);
        return new BridgeStateView(vnToday.toString(), open, todaySettled,
            LotteryClock.LOCK_TIME.toString(), LotteryClock.SCRAPE_TIME.toString());
    }

    /**
     * BitZero cmd 30001 — buy ticket. Translates legacy LotteryCmd fields
     * to engine {@link BetRequest} and returns the result.
     *
     * @param nickname  authenticated user
     * @param userId    BitZero user id (long)
     * @param modeId    legacy {@code lotteryCmd.mode}
     * @param ticket    legacy {@code lotteryCmd.num} (CSV for xien modes)
     * @param betValue  legacy {@code lotteryCmd.betValue}
     */
    public BridgeBetResponse bet(String nickname,
                                  long userId,
                                  int modeId,
                                  String ticket,
                                  long betValue) {
        if (nickname == null) {
            return new BridgeBetResponse("0001", -1L, null, "no nickname");
        }
        BetRequest req = new BetRequest(nickname, userId, modeId, ticket, betValue, null);
        BetAcceptResult r;
        try {
            r = acceptor.accept(req);
        } catch (Throwable t) {
            LOG.warn("LotteryModuleBridge.bet failed for user={}", nickname, t);
            return new BridgeBetResponse("0001", -1L, null, t.getMessage());
        }
        return new BridgeBetResponse(r.getErrorCode(), r.getCurrentMoney(), r.getTicketId(),
            r.isSuccess() ? "OK" : "rejected");
    }

    /** Read-only view returned to the legacy module — no result fields. */
    public static final class BridgeStateView {
        public final String vnDate;
        public final boolean bettingOpen;
        public final boolean todaySettled;
        public final String lockTime;
        public final String scrapeTime;

        BridgeStateView(String vnDate, boolean bettingOpen, boolean todaySettled,
                        String lockTime, String scrapeTime) {
            this.vnDate = vnDate;
            this.bettingOpen = bettingOpen;
            this.todaySettled = todaySettled;
            this.lockTime = lockTime;
            this.scrapeTime = scrapeTime;
        }
    }

    /** Bet response — mirrors {@link BetAcceptResult} core fields. */
    public static final class BridgeBetResponse {
        public final String errorCode;
        public final long currentMoney;
        public final Long ticketId;
        public final String message;

        BridgeBetResponse(String errorCode, long currentMoney, Long ticketId, String message) {
            this.errorCode = errorCode;
            this.currentMoney = currentMoney;
            this.ticketId = ticketId;
            this.message = message;
        }
    }
}
