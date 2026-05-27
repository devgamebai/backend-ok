package com.sunwinkr.minigame.api.wire;

import com.sunwinkr.minigame.api.adapter.JdbcTaixiuBetSettlePort;
import com.sunwinkr.minigame.api.adapter.JdbcTaixiuBetSettlePort.SettleOutcome;
import com.sunwinkr.minigame.api.push.TickPublisher;
import com.sunwinkr.minigame.engine.bet.BetAcceptResult;
import com.sunwinkr.minigame.engine.bet.BetAcceptor;
import com.sunwinkr.minigame.engine.bet.BetLedger;
import com.sunwinkr.minigame.engine.bet.BetRequest;
import com.sunwinkr.minigame.engine.core.TaiXiuRound;
import com.sunwinkr.minigame.engine.port.BetRecorder;
import com.sunwinkr.minigame.engine.port.WalletPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * BitZero ↔ engine adapter. Plan §6.
 *
 * <h3>Wire protocol unchanged</h3>
 * All {@code BaseMsg} subclasses
 * ({@code UpdateResultDicesMsg}, {@code UpdateTaiXiuPerSecondMsg},
 *  {@code BetTaiXiuMsg}, {@code TaiXiuInfoMsg}, {@code TaiXiuRefundMsg},
 *  {@code UpdatePrizeTaiXiuMsg}, {@code BroadcastTXTimeMsg},
 *  {@code LichSuPhienMsg}, {@code StartNewGameTaiXiuMsg},
 *  {@code TaiXiuJackpotMsg}) preserve their wire shape — the bridge
 * builds them from engine snapshots and hands them back to BitZero's
 * {@code sendMessageToUser} / {@code sendMessageToRoom}.
 *
 * <p>This class deliberately avoids any {@code bitzero.*} imports so the
 * engine + API jars stay BitZero-free. The legacy {@code TaiXiuModule}
 * accesses the bridge through reflective bean lookup
 * ({@link com.sunwinkr.minigame.api.MinigameApiApplication#contextHolder()})
 * and passes opaque {@code Object} payloads + raw fields. The contract
 * between bridge and legacy is documented at each method.
 *
 * <h3>Feature flag</h3>
 * {@code MINIGAME_ENGINE_ENABLED=1} flips the legacy
 * {@code handleClientRequest} to delegate to this bridge. Default OFF;
 * BitZero path remains authoritative until the cutover gate clears.
 */
@Component
public class TaiXiuModuleBridge {

    private static final Logger LOG = LoggerFactory.getLogger(TaiXiuModuleBridge.class);

    /** Env flag — set to "1" to flip the bridge ON. Default OFF. */
    public static final String FLAG_ENV = "MINIGAME_ENGINE_ENABLED";

    private final TaiXiuRound round;
    private final BetAcceptor acceptor;
    private final BetLedger ledger;
    private final WalletPort wallet;
    private final BetRecorder recorder;
    private final TickPublisher push;
    private final JdbcTaixiuBetSettlePort settlePort;

    public TaiXiuModuleBridge(TaiXiuRound round,
                                BetAcceptor acceptor,
                                BetLedger ledger,
                                @org.springframework.beans.factory.annotation.Qualifier("jdbcWalletPort") WalletPort wallet,
                                @org.springframework.beans.factory.annotation.Qualifier("mongoBetRecorder") BetRecorder recorder,
                                TickPublisher push,
                                JdbcTaixiuBetSettlePort settlePort) {
        this.round = round;
        this.acceptor = acceptor;
        this.ledger = ledger;
        this.wallet = wallet;
        this.recorder = recorder;
        this.push = push;
        this.settlePort = settlePort;
    }

    /** True iff the legacy handler should delegate to this bridge. */
    public static boolean isEnabled() {
        String v = System.getenv(FLAG_ENV);
        return v != null && (v.equals("1") || v.equalsIgnoreCase("true"));
    }

    /**
     * Subscribe handler. The legacy
     * {@code TaiXiuModule.subcribeMiniGame} is a join+ack with history
     * payload. We mirror the join by stamping the snapshot for the user
     * and emitting a roundStart push for the new subscriber.
     *
     * @param nickname    authenticated user nickname
     * @param gameId      target game id (legacy: 2 for TaiXiu)
     * @param roomId      legacy room id (0 = XU, 1 = VIN)
     * @return state snapshot (caller serializes to wire)
     */
    public BridgeStateView subscribe(String nickname, short gameId, short roomId) {
        return new BridgeStateView(round.snapshotForClient(nickname == null ? "" : nickname));
    }

    /** Unsubscribe — no-op in PR-4 (engine has no per-user session state). */
    public void unsubscribe(String nickname, short gameId, short roomId) {
        // intentionally empty
    }

    /** Change room — bridge translates a leave+join pair. */
    public BridgeStateView changeRoom(String nickname, short gameId, short lastRoom, short newRoom) {
        unsubscribe(nickname, gameId, lastRoom);
        return subscribe(nickname, gameId, newRoom);
    }

    /**
     * Bet entry point — wraps the legacy {@code BetTaiXiuCmd} fields. The
     * caller is responsible for resolving {@code isLivestream} from
     * Hazelcast {@code usersSetWin} before delegating.
     */
    public BridgeBetResponse bet(String nickname,
                                  int userId,
                                  long betValue,
                                  short inputTime,
                                  short moneyType,
                                  short betSide,
                                  boolean isBot,
                                  boolean isLivestream) {
        BetRequest req = new BetRequest(nickname, userId, betValue, inputTime,
            moneyType, betSide, isBot, isLivestream);
        BetAcceptResult r = acceptor.accept(req, round, ledger, wallet, recorder);
        try {
            if (r.isOk() && push != null) {
                push.publishPotDelta(moneyType, ledger.potTai().totalValue(), ledger.potXiu().totalValue());
            }
        } catch (Throwable t) {
            LOG.debug("publishPotDelta failed", t);
        }
        return new BridgeBetResponse((byte) r.errorCode, r.currentMoney, r.perBetTxId);
    }

    /**
     * B1 — Settle idempotency gate.
     *
     * <p>Called by the legacy settle path (MGRoomTaiXiu / SETTLE event) for each
     * bet row after the wallet credit is issued. Flips {@code taixiu_bet.settle_status}
     * from {@code PENDING} to {@code SETTLED}. If the row is already {@code SETTLED}
     * the call is a no-op (logs INFO and returns). Any DB failure is caught and
     * logged as WARN — the caller is not interrupted (fire-and-forget semantics
     * match the rest of the bridge settle path).
     *
     * @param betId   taixiu_bet primary key
     * @param roundId round identifier (for audit correlation)
     */
    public void settleBet(long betId, long roundId) {
        try {
            SettleOutcome outcome = settlePort.markSettled(betId, roundId);
            switch (outcome) {
                case SETTLED:
                    LOG.info("settleBet: SETTLED betId={} roundId={}", betId, roundId);
                    break;
                case ALREADY_SETTLED:
                    // Idempotency hit — do not re-publish wallet credit / RMQ row
                    LOG.info("settleBet: already SETTLED betId={} roundId={} — skip", betId, roundId);
                    break;
                case ALREADY_VOIDED:
                    LOG.warn("settleBet: betId={} roundId={} is VOIDED — settle rejected", betId, roundId);
                    break;
                case NOT_FOUND:
                    LOG.warn("settleBet: betId={} roundId={} not found in taixiu_bet", betId, roundId);
                    break;
                default:
                    LOG.warn("settleBet: unexpected outcome={} betId={}", outcome, betId);
                    break;
            }
        } catch (JdbcTaixiuBetSettlePort.SettlePortException e) {
            LOG.warn("settleBet: DB error betId={} roundId={}", betId, roundId, e);
        } catch (Throwable t) {
            LOG.warn("settleBet: unexpected error betId={} roundId={}", betId, roundId, t);
        }
    }

    /** History fetch — returns the history list size; payload built by caller. */
    public int historySize() {
        // PR-4: engine history list not yet owned by engine; bridge no-ops.
        return 0;
    }

    /** Read-only view returned to the legacy module. */
    public static final class BridgeStateView {
        public final long referenceId;
        public final short remainTime;
        public final boolean bettingState;

        BridgeStateView(com.sunwinkr.minigame.engine.snapshot.TaiXiuSnapshot s) {
            this.referenceId = s == null ? 0L : s.referenceId;
            this.remainTime = s == null ? 0 : s.remainTime;
            this.bettingState = s != null && s.bettingState;
        }
    }

    /** Bet response (mirrors {@code BetTaiXiuMsg} core fields). */
    public static final class BridgeBetResponse {
        public final byte error;
        public final long currentMoney;
        public final long perBetTxId;

        BridgeBetResponse(byte error, long currentMoney, long perBetTxId) {
            this.error = error;
            this.currentMoney = currentMoney;
            this.perBetTxId = perBetTxId;
        }
    }
}
