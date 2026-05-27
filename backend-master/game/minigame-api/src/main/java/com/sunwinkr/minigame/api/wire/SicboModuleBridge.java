package com.sunwinkr.minigame.api.wire;

import com.sunwinkr.minigame.api.push.SicboTickPublisher;
import com.sunwinkr.minigame.engine.port.BetRecorder;
import com.sunwinkr.minigame.engine.port.WalletPort;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboBetAcceptResult;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboBetRequest;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboBetService;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboPotState;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboTxIdGenerator;
import com.sunwinkr.minigame.engine.sicbo.core.SicboRound;
import com.sunwinkr.minigame.engine.sicbo.snapshot.SicboSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * BitZero ↔ Sicbo engine adapter. Plan §6 (analog of TaiXiuModuleBridge).
 *
 * <h3>Wire protocol unchanged</h3>
 * All {@code BaseMsg} subclasses
 * ({@code UpdateResultSicboDicesMsg}, {@code UpdateSicboPerSecondMsg},
 *  {@code BetSicboMsg}, {@code SicboInfoMsg}, {@code SicboRefundMsg},
 *  {@code UpdatePrizeSicboMsg}, {@code BroadcastTXTimeMsg},
 *  {@code LichSuPhienSicboMsg}, {@code StartNewGameSicboMsg},
 *  {@code UpdateFinalSicboMsg}) preserve their wire shape — the bridge
 * builds them from engine snapshots and hands them back to BitZero's
 * {@code sendMessageToUser} / {@code sendMessageToRoom}.
 *
 * <p>This class deliberately avoids any {@code bitzero.*} imports so the
 * engine + API jars stay BitZero-free. The legacy {@code SicboModule}
 * accesses the bridge through reflective bean lookup
 * ({@link com.sunwinkr.minigame.api.MinigameApiApplication#contextHolder()})
 * and passes opaque {@code Object} payloads + raw fields.
 *
 * <h3>Command dispatch (legacy command IDs preserved)</h3>
 * <ul>
 *   <li>{@code 28000} — subscribe → {@link #subscribe}</li>
 *   <li>{@code 28001} — change room → {@link #changeRoom}</li>
 *   <li>{@code 28002} — unsubscribe → {@link #unsubscribe}</li>
 *   <li>{@code 28003} — force-result (DEPRECATED on player socket; moved
 *       to AdminSicboController role-gated path)</li>
 *   <li>{@code 28110} — bet → {@link #bet}</li>
 * </ul>
 *
 * <h3>Feature flag</h3>
 * {@code MINIGAME_ENGINE_ENABLED=1} flips the legacy
 * {@code SicboModule.handleClientRequest} to delegate to this bridge.
 * Default OFF; BitZero path remains authoritative until cutover.
 */
@Component
public class SicboModuleBridge {

    private static final Logger LOG = LoggerFactory.getLogger(SicboModuleBridge.class);

    /** Env flag — set to "1" to flip the bridge ON. Default OFF. */
    public static final String FLAG_ENV = "MINIGAME_ENGINE_ENABLED";

    private final SicboRound round;
    private final SicboBetService betService;
    private final SicboPotState pot;
    private final SicboTxIdGenerator txGen;
    private final WalletPort wallet;
    private final BetRecorder recorder;
    private final SicboTickPublisher push;

    public SicboModuleBridge(SicboRound round,
                              SicboBetService betService,
                              SicboPotState pot,
                              SicboTxIdGenerator txGen,
                              @Qualifier("sicboWalletPort") WalletPort wallet,
                              @Qualifier("sicboBetRecorder") BetRecorder recorder,
                              SicboTickPublisher push) {
        this.round = round;
        this.betService = betService;
        this.pot = pot;
        this.txGen = txGen;
        this.wallet = wallet;
        this.recorder = recorder;
        this.push = push;
    }

    /** True iff the legacy handler should delegate to this bridge. */
    public static boolean isEnabled() {
        String v = System.getenv(FLAG_ENV);
        return v != null && (v.equals("1") || v.equalsIgnoreCase("true"));
    }

    /**
     * Subscribe handler — legacy cmd 28000. Returns a state snapshot for
     * the new subscriber.
     */
    public BridgeStateView subscribe(String nickname, short gameId, short roomId) {
        return new BridgeStateView(buildSnapshot(nickname));
    }

    /** Unsubscribe — legacy cmd 28002. No-op in PR-4. */
    public void unsubscribe(String nickname, short gameId, short roomId) {
        // intentionally empty
    }

    /** Change room — legacy cmd 28001 — translates to a leave+join pair. */
    public BridgeStateView changeRoom(String nickname, short gameId, short lastRoom, short newRoom) {
        unsubscribe(nickname, gameId, lastRoom);
        return subscribe(nickname, gameId, newRoom);
    }

    /**
     * Bet entry point — legacy cmd 28110. Wraps the {@code BetSicboCmd}
     * fields. The caller is responsible for resolving {@code isBot} from
     * the BitZero user object.
     */
    public BridgeBetResponse bet(String nickname,
                                  int userId,
                                  long betValue,
                                  short inputTime,
                                  short moneyType,
                                  String betSideName,
                                  boolean isBot) {
        SicboBetRequest req = new SicboBetRequest(nickname, userId, betValue,
            inputTime, moneyType, betSideName, isBot);
        SicboBetAcceptResult r = betService.accept(req, round, txGen, pot, wallet, recorder);
        try {
            if (r.isSuccess() && push != null) {
                push.publishPotDelta(moneyType, pot.totalValueBetUser());
            }
        } catch (Throwable t) {
            LOG.debug("publishPotDelta failed", t);
        }
        return new BridgeBetResponse((byte) r.errorCode, r.currentMoney, r.perBetTxId,
            r.transactionCode, r.betSideId);
    }

    /**
     * Engine event listener — fired by SicboRound when phase advances
     * to REVEALED. Pushes a {@code reveal} STOMP message.
     */
    public void onDiceRevealed(short[] dice) {
        if (push == null) {
            return;
        }
        try {
            SicboSnapshot snap = SicboSnapshot.of(
                (short) 5, (short) 1, round.getReferenceId(),
                0, false,
                0L, 0L, 0L, 0L, 0L, 0L,
                dice != null && dice.length > 0 ? dice[0] : null,
                dice != null && dice.length > 1 ? dice[1] : null,
                dice != null && dice.length > 2 ? dice[2] : null,
                round.getPhase());
            push.publishReveal(snap);
        } catch (Throwable t) {
            LOG.debug("onDiceRevealed publish failed", t);
        }
    }

    /** Engine event listener — fired on new-round transition. */
    public void onNewRound(long referenceId) {
        if (push == null) {
            return;
        }
        try {
            push.publishRoundStart(referenceId);
        } catch (Throwable t) {
            LOG.debug("onNewRound publish failed", t);
        }
    }

    /** History fetch — returns the history list size; payload built by caller. */
    public int historySize() {
        // PR-4: engine history list not yet owned by engine; bridge no-ops.
        return 0;
    }

    private SicboSnapshot buildSnapshot(String nickname) {
        short[] dice = round.getPendingDice();
        return SicboSnapshot.of(
            (short) 5, (short) 1, round.getReferenceId(),
            0, round.isBetting(),
            0L, 0L, 0L, 0L, 0L, 0L,
            dice != null && dice.length > 0 ? dice[0] : null,
            dice != null && dice.length > 1 ? dice[1] : null,
            dice != null && dice.length > 2 ? dice[2] : null,
            round.getPhase());
    }

    /** Read-only view returned to the legacy module. */
    public static final class BridgeStateView {
        public final long referenceId;
        public final boolean bettingState;
        public final String phase;

        BridgeStateView(SicboSnapshot s) {
            this.referenceId = s == null ? 0L : s.referenceId;
            this.bettingState = s != null && s.bettingState;
            this.phase = s == null || s.phase == null ? "OPEN" : s.phase.name();
        }
    }

    /** Bet response (mirrors {@code BetSicboMsg} core fields). */
    public static final class BridgeBetResponse {
        public final byte error;
        public final long currentMoney;
        public final long perBetTxId;
        public final String transactionCode;
        public final int betSideId;

        BridgeBetResponse(byte error, long currentMoney, long perBetTxId,
                           String transactionCode, int betSideId) {
            this.error = error;
            this.currentMoney = currentMoney;
            this.perBetTxId = perBetTxId;
            this.transactionCode = transactionCode;
            this.betSideId = betSideId;
        }
    }
}
