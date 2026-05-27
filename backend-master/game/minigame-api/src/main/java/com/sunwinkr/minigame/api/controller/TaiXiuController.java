package com.sunwinkr.minigame.api.controller;

import com.sunwinkr.minigame.api.adapter.LegacyTaixiuHistoryPort;
import com.sunwinkr.minigame.api.dto.BetRequestDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sunwinkr.minigame.api.dto.BetResponseDto;
import com.sunwinkr.minigame.api.dto.HistoryDto;
import com.sunwinkr.minigame.api.dto.StateDto;
import com.sunwinkr.minigame.api.scheduler.TaiXiuRoundScheduler;
import com.sunwinkr.minigame.api.scheduler.TaiXiuRoundState;
import com.sunwinkr.minigame.engine.bet.BetAcceptResult;
import com.sunwinkr.minigame.engine.bet.BetAcceptor;
import com.sunwinkr.minigame.engine.bet.BetLedger;
import com.sunwinkr.minigame.engine.bet.BetRequest;
import com.sunwinkr.minigame.engine.bet.TxIdGenerator;
import com.sunwinkr.minigame.engine.core.TaiXiuRound;
import com.sunwinkr.minigame.engine.port.BetRecorder;
import com.sunwinkr.minigame.engine.port.MoneyResult;
import com.sunwinkr.minigame.engine.port.TransKind;
import com.sunwinkr.minigame.engine.port.WalletPort;
import com.sunwinkr.minigame.engine.snapshot.TaiXiuSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Collections;

/**
 * Player-facing TaiXiu endpoints. Plan §5.1.
 *
 * <ul>
 *   <li>{@code POST /api/v2/taixiu/join}     — claim a seat in a moneyType room (no-op for now; snapshot returned)</li>
 *   <li>{@code POST /api/v2/taixiu/leave}    — leave the room</li>
 *   <li>{@code POST /api/v2/taixiu/bet}      — place a bet via {@link BetAcceptor}</li>
 *   <li>{@code GET  /api/v2/taixiu/state}    — current snapshot (censored pre-reveal)</li>
 *   <li>{@code GET  /api/v2/taixiu/history}  — last N round results (cap 120)</li>
 * </ul>
 *
 * <p>Authentication comes from {@link com.sunwinkr.minigame.api.security
 * .AccessTokenFilter} which binds the nickname into the Spring Security
 * context. All endpoints require an authenticated principal except for
 * the {@code /products} group (defined elsewhere).
 */
@RestController
@RequestMapping("/api/v2/taixiu")
public class TaiXiuController {

    private static final Logger LOG = LoggerFactory.getLogger(TaiXiuController.class);

    private final TaiXiuRound round;
    private final BetAcceptor betAcceptor;
    private final BetLedger ledger;
    private final WalletPort wallet;
    private final BetRecorder recorder;
    /** Standalone scheduler state — non-null when TAIXIU_SCHEDULER_ENABLED=1. */
    private final TaiXiuRoundState schedulerState;
    /** Legacy history adapter — writes log_taixiu + transaction_tai_xiu_sicbo on bet. */
    private final LegacyTaixiuHistoryPort legacyHistory;

    @Autowired
    public TaiXiuController(TaiXiuRound round,
                             BetAcceptor betAcceptor,
                             BetLedger ledger,
                             @org.springframework.beans.factory.annotation.Qualifier("jdbcWalletPort") WalletPort wallet,
                             @org.springframework.beans.factory.annotation.Qualifier("mongoBetRecorder") BetRecorder recorder,
                             TaiXiuRoundState schedulerState,
                             LegacyTaixiuHistoryPort legacyHistory) {
        this.round = round;
        this.betAcceptor = betAcceptor;
        this.ledger = ledger;
        this.wallet = wallet;
        this.recorder = recorder;
        this.schedulerState = schedulerState;
        this.legacyHistory = legacyHistory;
    }

    @PostMapping("/join")
    public ResponseEntity<StateDto> join(@RequestBody(required = false) java.util.Map<String, Object> body) {
        String nickname = currentNickname();
        if (nickname == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        TaiXiuSnapshot snap = TaiXiuRoundScheduler.isEnabled()
            ? schedulerState.toSnapshot(nickname)
            : round.snapshotForClient(nickname);
        return ResponseEntity.ok(StateDto.fromSnapshot(snap));
    }

    @PostMapping("/leave")
    public ResponseEntity<java.util.Map<String, Object>> leave(@RequestBody(required = false) java.util.Map<String, Object> body) {
        String nickname = currentNickname();
        if (nickname == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("success", true);
        return ResponseEntity.ok(m);
    }

    @PostMapping("/bet")
    public ResponseEntity<BetResponseDto> bet(@Valid @RequestBody BetRequestDto req) {
        String nickname = currentNickname();
        if (nickname == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(BetResponseDto.error(401, 0L, "Unauthorized"));
        }
        if (req == null || req.moneyType == null || req.betSide == null) {
            return ResponseEntity.badRequest()
                .body(BetResponseDto.error(4, 0L, "Missing required fields"));
        }

        // When the standalone scheduler is running, validate bet window and
        // debit wallet directly; the BetLedger / TaiXiuRound path is bypassed.
        if (TaiXiuRoundScheduler.isEnabled()) {
            return betViaScheduler(nickname, req);
        }

        short inputTime = req.inputTime == null ? (short) 0 : req.inputTime;
        BetRequest engineReq = new BetRequest(
            nickname,
            /*userId*/ 0,
            req.betValue,
            inputTime,
            req.moneyType,
            req.betSide,
            /*isBot*/ false,
            /*isLivestream*/ false);
        BetAcceptResult result = betAcceptor.accept(engineReq, round, ledger, wallet, recorder);
        if (result.isOk()) {
            return ResponseEntity.ok(BetResponseDto.ok(result.currentMoney, result.perBetTxId));
        }
        HttpStatus status = result.errorCode == 1 || result.errorCode == 3
            ? HttpStatus.OK   // wallet/balance errors are operational, not 4xx
            : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
            .body(BetResponseDto.error(result.errorCode, result.currentMoney,
                describeError(result.errorCode)));
    }

    /**
     * Bet path when the standalone scheduler is active (SUN-1341 E1).
     * Validates wall-clock window, debits wallet, registers bet in
     * {@link TaiXiuRoundState} for the upcoming settle pass.
     */
    private ResponseEntity<BetResponseDto> betViaScheduler(String nickname, BetRequestDto req) {
        // Check betting window.
        if (!schedulerState.isBettingOpen()) {
            return ResponseEntity.ok(BetResponseDto.error(7, 0L, "BET_WINDOW_CLOSED"));
        }
        // Validate minimum bet.
        if (req.betValue < com.sunwinkr.minigame.engine.bet.BetAcceptor.MIN_BET) {
            long balance = wallet.getBalance(nickname, moneyTypeStr(req.moneyType));
            return ResponseEntity.badRequest()
                .body(BetResponseDto.error(4, balance, describeError(4)));
        }
        String moneyTypeStr = moneyTypeStr(req.moneyType);
        long balance = wallet.getBalance(nickname, moneyTypeStr);
        if (req.betValue > balance) {
            return ResponseEntity.ok(BetResponseDto.error(3, balance, describeError(3)));
        }
        long roundId = schedulerState.getRoundId();
        long perBetTxId = TxIdGenerator.nextBetTxId(roundId);
        // Debit wallet.
        MoneyResult debit = wallet.debit(
            nickname,
            req.betValue,
            moneyTypeStr,
            "TaiXiu",
            2L,
            "TaiXiu bet roundId=" + roundId + " side=" + req.betSide,
            0L,
            perBetTxId,
            TransKind.START);
        if (!debit.isSuccess()) {
            return ResponseEntity.ok(
                BetResponseDto.error(1, debit.getCurrentMoney(), describeError(1)));
        }
        // Re-check window after debit (race guard).
        if (!schedulerState.isBettingOpen()) {
            // Rollback.
            try {
                wallet.credit(nickname, req.betValue, moneyTypeStr,
                    "TaiXiuHoanTien", 2L,
                    "TaiXiu auto-refund roundId=" + roundId,
                    0L, perBetTxId, TransKind.END);
            } catch (Throwable t) {
                // Best-effort refund.
            }
            return ResponseEntity.ok(BetResponseDto.error(7, debit.getCurrentMoney(), "BET_WINDOW_CLOSED"));
        }
        // Register in round state.
        schedulerState.registerBet(new TaiXiuRoundState.PendingBet(
            nickname, roundId, req.betValue,
            req.betSide, req.moneyType, perBetTxId,
            System.currentTimeMillis()));

        // Write legacy history rows (fire-and-forget) so c=303 can surface this bet.
        try {
            legacyHistory.recordBet(
                    roundId,
                    nickname,
                    req.betValue,
                    (int) req.betSide,
                    (int) req.moneyType,
                    debit.getCurrentMoney());
        } catch (Throwable t) {
            LOG.warn("TaiXiuController.betViaScheduler: legacyHistory.recordBet failed " +
                     "roundId={} nickname={}", roundId, nickname, t);
        }

        return ResponseEntity.ok(BetResponseDto.ok(debit.getCurrentMoney(), perBetTxId));
    }

    private static String moneyTypeStr(short moneyType) {
        return moneyType == 1 ? "vin" : "xu";
    }

    @GetMapping("/state")
    public ResponseEntity<StateDto> state(@RequestParam(value = "moneyType", defaultValue = "1") short moneyType) {
        String nickname = currentNickname();
        if (nickname == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        TaiXiuSnapshot snap = TaiXiuRoundScheduler.isEnabled()
            ? schedulerState.toSnapshot(nickname)
            : round.snapshotForClient(nickname);
        return ResponseEntity.ok(StateDto.fromSnapshot(snap));
    }

    @GetMapping("/history")
    public ResponseEntity<HistoryDto> history(@RequestParam(value = "moneyType", defaultValue = "1") short moneyType,
                                                @RequestParam(value = "n", defaultValue = "100") int n) {
        String nickname = currentNickname();
        if (nickname == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // PR-4 baseline: history list lives in the BitZero module's
        // lichSuPhienTX field; the engine doesn't yet own it. Return
        // an empty list — bridge will mirror history into engine in a
        // follow-up. Plan §2.8 H1/H2.
        int cap = Math.max(0, Math.min(n, 120));
        return ResponseEntity.ok(new HistoryDto(Collections.emptyList()));
    }

    /** Resolved by {@link com.sunwinkr.minigame.api.security.AccessTokenFilter}. */
    private static String currentNickname() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return null;
        }
        String principal = auth.getPrincipal().toString();
        if (principal.isEmpty() || "anonymousUser".equals(principal)) {
            return null;
        }
        return principal;
    }

    private static String describeError(int code) {
        switch (code) {
            case 1: return "Wallet failure or betting disabled mid-call";
            case 2: return "Betting closed";
            case 3: return "Insufficient balance";
            case 4: return "Bet below minimum (100)";
            case 5: return "Cross-side bet not allowed";
            case 7: return "BET_WINDOW_CLOSED";
            default: return "Bet rejected";
        }
    }
}
