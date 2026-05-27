package com.sunwinkr.minigame.api.controller.sicbo;

import com.sunwinkr.minigame.api.adapter.sicbo.LegacySicboHistoryPort;
import com.sunwinkr.minigame.api.dto.sicbo.SicboBetRequestDto;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboPotEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sunwinkr.minigame.api.dto.sicbo.SicboBetResponseDto;
import com.sunwinkr.minigame.api.dto.sicbo.SicboHistoryDto;
import com.sunwinkr.minigame.api.dto.sicbo.SicboStateDto;
import com.sunwinkr.minigame.engine.port.BetRecorder;
import com.sunwinkr.minigame.engine.port.WalletPort;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboBetAcceptResult;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboBetRequest;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboBetService;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboPotState;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboTxIdGenerator;
import com.sunwinkr.minigame.engine.sicbo.core.SicboRound;
import com.sunwinkr.minigame.engine.sicbo.snapshot.SicboSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
 * Player-facing Sicbo endpoints. Plan §6 (analog of TaiXiu §5.1).
 *
 * <ul>
 *   <li>{@code POST /api/v2/sicbo/join}     — claim a seat (snapshot returned)</li>
 *   <li>{@code POST /api/v2/sicbo/leave}    — leave the room</li>
 *   <li>{@code POST /api/v2/sicbo/bet}      — place a bet via {@link SicboBetService};
 *       {@code betSide} is a STRING name (e.g. "TAI", "ONE_DICE_3")</li>
 *   <li>{@code GET  /api/v2/sicbo/state}    — current snapshot (censored pre-reveal)</li>
 *   <li>{@code GET  /api/v2/sicbo/history}  — last N round results (cap 120)</li>
 * </ul>
 *
 * <p>Authentication comes from {@link com.sunwinkr.minigame.api.security
 * .AccessTokenFilter}.
 */
@RestController
@RequestMapping("/api/v2/sicbo")
public class SicboController {

    private static final Logger LOG = LoggerFactory.getLogger(SicboController.class);

    private final SicboRound round;
    private final SicboBetService betService;
    private final SicboPotState pot;
    private final SicboTxIdGenerator txGen;
    private final WalletPort wallet;
    private final BetRecorder recorder;
    private final LegacySicboHistoryPort legacyHistory;

    @Autowired
    public SicboController(SicboRound round,
                            SicboBetService betService,
                            SicboPotState pot,
                            SicboTxIdGenerator txGen,
                            @Qualifier("sicboWalletPort") WalletPort wallet,
                            @Qualifier("sicboBetRecorder") BetRecorder recorder,
                            LegacySicboHistoryPort legacyHistory) {
        this.round = round;
        this.betService = betService;
        this.pot = pot;
        this.txGen = txGen;
        this.wallet = wallet;
        this.recorder = recorder;
        this.legacyHistory = legacyHistory;
    }

    @PostMapping("/join")
    public ResponseEntity<SicboStateDto> join(@RequestBody(required = false) java.util.Map<String, Object> body) {
        String nickname = currentNickname();
        if (nickname == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(SicboStateDto.fromSnapshot(buildSnapshot(nickname)));
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
    public ResponseEntity<SicboBetResponseDto> bet(@Valid @RequestBody SicboBetRequestDto req) {
        String nickname = currentNickname();
        if (nickname == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(SicboBetResponseDto.error(401, 0L, "Unauthorized"));
        }
        if (req == null || req.moneyType == null || req.betSide == null) {
            return ResponseEntity.badRequest()
                .body(SicboBetResponseDto.error(4, 0L, "Missing required fields"));
        }
        short inputTime = req.inputTime == null ? (short) 0 : req.inputTime;
        SicboBetRequest engineReq = new SicboBetRequest(
            nickname,
            /*userId*/ 0,
            req.betValue,
            inputTime,
            req.moneyType,
            req.betSide,
            /*isBot*/ false);
        SicboBetAcceptResult result = betService.accept(engineReq, round, txGen, pot, wallet, recorder);
        if (result.isSuccess()) {
            // SUN-1341 F2: mirror bet into log_sicbo + transaction_tai_xiu_sicbo
            // so c=303 history surfaces bets placed via the REST path.
            // Failures are warn-logged inside recordBet — never block the response.
            try {
                SicboPotEntry entry = pot.findByTxId(result.perBetTxId);
                if (entry != null) {
                    legacyHistory.recordBet(round.getReferenceId(), entry);
                }
            } catch (Throwable t) {
                LOG.warn("SicboController.bet: legacyHistory.recordBet failed nickname={} refId={}",
                         nickname, round.getReferenceId(), t);
            }
            return ResponseEntity.ok(SicboBetResponseDto.ok(
                result.currentMoney,
                result.perBetTxId,
                result.transactionCode,
                result.betSideId));
        }
        HttpStatus status = result.errorCode == 1 || result.errorCode == 3
            ? HttpStatus.OK   // wallet/balance errors are operational, not 4xx
            : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
            .body(SicboBetResponseDto.error(result.errorCode, result.currentMoney,
                describeError(result.errorCode)));
    }

    @GetMapping("/state")
    public ResponseEntity<SicboStateDto> state(@RequestParam(value = "moneyType", defaultValue = "1") short moneyType) {
        String nickname = currentNickname();
        if (nickname == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(SicboStateDto.fromSnapshot(buildSnapshot(nickname)));
    }

    @GetMapping("/history")
    public ResponseEntity<SicboHistoryDto> history(@RequestParam(value = "moneyType", defaultValue = "1") short moneyType,
                                                    @RequestParam(value = "n", defaultValue = "100") int n) {
        String nickname = currentNickname();
        if (nickname == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // PR-4 baseline: history list lives in the BitZero module's
        // lichSuPhienSicbo field; the engine doesn't yet own it. Return
        // an empty list — bridge will mirror history into engine in a
        // follow-up. Plan §2.8 H1/H2.
        int cap = Math.max(0, Math.min(n, 120));
        return ResponseEntity.ok(new SicboHistoryDto(Collections.emptyList()));
    }

    /**
     * Build a censored client snapshot from the current Sicbo round +
     * pot state. The {@link SicboSnapshot} ctor enforces dice censoring
     * (pre-REVEALED → null).
     */
    SicboSnapshot buildSnapshot(String nickname) {
        short[] dice = round.getPendingDice();
        Short d1 = dice != null && dice.length >= 3 ? dice[0] : null;
        Short d2 = dice != null && dice.length >= 3 ? dice[1] : null;
        Short d3 = dice != null && dice.length >= 3 ? dice[2] : null;
        return SicboSnapshot.of(
            /*gameId*/          (short) 5,
            /*moneyType*/       (short) 1,
            round.getReferenceId(),
            /*bettingClosesAt*/ round.bettingClosesAt(),
            /*roundId*/         round.getReferenceId(),
            /*remainTime*/      0,
            round.isBetting(),
            /*potTai*/          0L,
            /*potXiu*/          0L,
            /*myBetTai*/        0L,
            /*myBetXiu*/        0L,
            /*jpTai*/           0L,
            /*jpXiu*/           0L,
            d1, d2, d3,
            round.getPhase());
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
            case 6: return "Invalid bet type name";
            case 7: return "BET_WINDOW_CLOSED";
            default: return "Bet rejected";
        }
    }
}
