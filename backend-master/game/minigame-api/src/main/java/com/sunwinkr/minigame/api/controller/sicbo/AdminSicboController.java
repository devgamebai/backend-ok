package com.sunwinkr.minigame.api.controller.sicbo;

import com.sunwinkr.minigame.api.adapter.sicbo.JdbcSicboBetStore;
import com.sunwinkr.minigame.api.adapter.sicbo.JdbcSicboBetStore.SicboBetRow;
import com.sunwinkr.minigame.api.dto.sicbo.SicboForceResultRequest;
import com.sunwinkr.minigame.api.dto.sicbo.SicboKillSwitchRequest;
import com.sunwinkr.minigame.api.dto.sicbo.SicboUnsettleRequest;
import com.sunwinkr.minigame.engine.port.ForceResultStore;
import com.sunwinkr.minigame.engine.port.MoneyResult;
import com.sunwinkr.minigame.engine.port.TransKind;
import com.sunwinkr.minigame.engine.port.WalletPort;
import com.sunwinkr.minigame.engine.sicbo.core.SicboRound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Admin Sicbo endpoints — role-gated by
 * {@link com.sunwinkr.minigame.api.security.RoleResolver#ROLE_MINIGAME_ADMIN}
 * via the {@link com.sunwinkr.minigame.api.config.SecurityConfig} chain.
 *
 * <h3>MED-1 fix (Sicbo coverage)</h3>
 * Legacy {@code SicboCheatHandler} guarded force-result by
 * {@code user.getName().contains("superadmin")} (same pattern as TaiXiu
 * TXM:259). The Spring chain now enforces explicit role authority — the
 * substring path is gone. {@code superadmin_sicbo} usernames without
 * {@code ROLE_MINIGAME_ADMIN} are rejected with 403.
 *
 * <ul>
 *   <li>{@code POST /api/v2/admin/sicbo/force-result}  — write
 *       {@code ketquataixiusicbo} HZ map; engine consumes atomically
 *       next round. Body: {@code {"dice":[n,n,n]}}, each in {@code [1..6]}.</li>
 *   <li>{@code POST /api/v2/admin/sicbo/kill-switch}   — pause/resume
 *       the engine</li>
 *   <li>{@code GET  /api/v2/admin/sicbo/round-state}   — diagnostics</li>
 *   <li>{@code POST /api/v2/admin/sicbo/unsettle}      — void a SETTLED
 *       bet, reverse the wallet, audit via log_money_user_vin (SUN-1339 §B2)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v2/admin/sicbo")
public class AdminSicboController {

    private static final Logger LOG = LoggerFactory.getLogger(AdminSicboController.class);

    /** Vietnamese game name used in wallet audit entries. */
    private static final String GAME_NAME = "Xí Ngầu";

    /** Wallet source tag for Sicbo refund entries. */
    private static final String SOURCE_REFUND = "Sicbo Refund";

    private final ForceResultStore forceStore;
    private final SicboRound round;
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final JdbcSicboBetStore betStore;
    private final WalletPort wallet;

    @Autowired
    public AdminSicboController(@Qualifier("sicboForceResultStore") ForceResultStore forceStore,
                                 SicboRound round,
                                 JdbcSicboBetStore betStore,
                                 @Qualifier("sicboWalletPort") WalletPort wallet) {
        this.forceStore = forceStore;
        this.round      = round;
        this.betStore   = betStore;
        this.wallet     = wallet;
    }

    @PostMapping("/force-result")
    public ResponseEntity<Map<String, Object>> forceResult(@Valid @RequestBody SicboForceResultRequest req) {
        Map<String, Object> resp = new HashMap<>();
        if (req == null || req.dice == null || req.dice.length != 3) {
            resp.put("success", false);
            resp.put("message", "dice must be a length-3 array");
            return ResponseEntity.badRequest().body(resp);
        }
        for (short d : req.dice) {
            if (d < 1 || d > 6) {
                resp.put("success", false);
                resp.put("message", "each die must be in [1..6]");
                return ResponseEntity.badRequest().body(resp);
            }
        }
        short[] dice = new short[] { req.dice[0], req.dice[1], req.dice[2] };
        forceStore.set(dice);
        String role = currentRole();
        LOG.info("Admin sicbo force-result dice=[{},{},{}] role={}",
            dice[0], dice[1], dice[2], role);
        resp.put("success", true);
        resp.put("dice", new short[] { dice[0], dice[1], dice[2] });
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/kill-switch")
    public ResponseEntity<Map<String, Object>> killSwitch(@Valid @RequestBody SicboKillSwitchRequest req) {
        Map<String, Object> resp = new HashMap<>();
        if (req == null || req.paused == null) {
            resp.put("success", false);
            return ResponseEntity.badRequest().body(resp);
        }
        boolean was = paused.getAndSet(req.paused);
        LOG.info("Admin sicbo kill-switch paused: {} -> {} by={}", was, req.paused, currentRole());
        resp.put("success", true);
        resp.put("paused", req.paused);
        resp.put("previous", was);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/round-state")
    public ResponseEntity<Map<String, Object>> roundState() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("referenceId", round.getReferenceId());
        resp.put("phase", round.getPhase().name());
        resp.put("paused", paused.get());
        return ResponseEntity.ok(resp);
    }

    /**
     * Void a SETTLED Sicbo bet and reverse the wallet.
     *
     * <h3>Logic (SUN-1339 §B2)</h3>
     * <ol>
     *   <li>Lookup the {@code sicbo_bet} row — 404 if not found.</li>
     *   <li>Reject 400 {@code NOT_SETTLED} if {@code settle_status != 'SETTLED'}.</li>
     *   <li>Determine refund direction:
     *     <ul>
     *       <li>Winner ({@code prize > 0}) → debit the prize back.</li>
     *       <li>Loser  ({@code prize == 0}) → credit the original bet amount.</li>
     *     </ul>
     *   </li>
     *   <li>Flip {@code settle_status = 'VOIDED'} in MySQL.</li>
     *   <li>Wallet operation is logged to {@code log_money_user_vin} via the
     *       existing {@link WalletPort} → {@code UserServiceImpl.updateMoney}
     *       path (source={@value #SOURCE_REFUND}, gameId={@value #GAME_NAME}).</li>
     * </ol>
     *
     * <p>The endpoint is idempotent at the DB layer: the
     * {@code markVoided} UPDATE uses {@code WHERE settle_status='SETTLED'} so
     * a double-call returns 400 on the second request (row is already VOIDED).
     */
    @PostMapping("/unsettle")
    public ResponseEntity<Map<String, Object>> unsettleBet(
            @Valid @RequestBody SicboUnsettleRequest req) {
        Map<String, Object> resp = new HashMap<>();

        if (req == null || req.ticketId == null) {
            resp.put("success", false);
            resp.put("errorCode", "4001");
            resp.put("message", "ticketId is required");
            return ResponseEntity.badRequest().body(resp);
        }

        // Step 1 — lookup
        SicboBetRow row = betStore.findById(req.ticketId);
        if (row == null) {
            resp.put("success", false);
            resp.put("errorCode", "1002");
            resp.put("message", "Ticket not found: " + req.ticketId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
        }

        // Step 2 — must be SETTLED
        if (!row.isSettled()) {
            resp.put("success", false);
            resp.put("errorCode", "NOT_SETTLED");
            resp.put("message", "Ticket " + req.ticketId + " is " + row.settleStatus + ", not SETTLED");
            return ResponseEntity.badRequest().body(resp);
        }

        String actor       = currentRole();
        String moneyType   = row.moneyTypeStr();
        String description = GAME_NAME + " — void by admin. Reason: " + req.reason
                             + " | ticketId=" + req.ticketId
                             + " | roundId=" + row.roundId;

        // Step 3 — reverse wallet
        // Winner: debit the prize back. Loser: credit the original bet.
        MoneyResult walletResult;
        if (row.prize > 0L) {
            // Debit the prize that was paid out
            walletResult = wallet.debit(
                row.nickname,
                row.prize,
                moneyType,
                SOURCE_REFUND,
                0L,
                description,
                0L,
                req.ticketId,
                TransKind.END);
        } else {
            // Credit the original bet (loser refund)
            walletResult = wallet.credit(
                row.nickname,
                row.betValue,
                moneyType,
                SOURCE_REFUND,
                0L,
                description,
                0L,
                req.ticketId,
                TransKind.END);
        }

        if (!walletResult.isSuccess()) {
            LOG.error("AdminSicboController.unsettleBet: wallet op failed ticketId={} nickname={} errorCode={} actor={}",
                      req.ticketId, row.nickname, walletResult.getErrorCode(), actor);
            resp.put("success", false);
            resp.put("errorCode", "9999");
            resp.put("message", "Wallet reversal failed: " + walletResult.getErrorCode());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
        }

        // Step 4 — flip status to VOIDED
        boolean voided = betStore.markVoided(req.ticketId);
        if (!voided) {
            // Wallet was already reversed but DB update failed — log at ERROR, return 409
            LOG.error("AdminSicboController.unsettleBet: markVoided no-op after wallet reversal ticketId={} actor={}",
                      req.ticketId, actor);
            resp.put("success", false);
            resp.put("errorCode", "4009");
            resp.put("message", "Status update failed — ticket may have been voided concurrently");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(resp);
        }

        LOG.info("AdminSicboController.unsettleBet: ticketId={} nickname={} prize={} betValue={} moneyType={} reason={} actor={}",
                 req.ticketId, row.nickname, row.prize, row.betValue, moneyType, req.reason, actor);

        resp.put("success", true);
        resp.put("ticketId", req.ticketId);
        resp.put("nickname", row.nickname);
        resp.put("settleStatus", "VOIDED");
        resp.put("walletBalance", walletResult.getCurrentMoney());
        return ResponseEntity.ok(resp);
    }

    /** True iff the current SecurityContext principal is an admin authority. */
    static String currentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return "anonymous";
        }
        return auth.getAuthorities() == null
            ? "unknown"
            : auth.getAuthorities().toString();
    }
}
