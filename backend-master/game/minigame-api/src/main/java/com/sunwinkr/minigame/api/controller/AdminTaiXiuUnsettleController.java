package com.sunwinkr.minigame.api.controller;

import com.sunwinkr.minigame.api.adapter.JdbcTaixiuBetSettlePort;
import com.sunwinkr.minigame.api.adapter.JdbcTaixiuBetSettlePort.BetRow;
import com.sunwinkr.minigame.api.adapter.JdbcTaixiuUnsettleAuditPort;
import com.sunwinkr.minigame.api.dto.UnsettleBetRequest;
import com.sunwinkr.minigame.engine.port.MoneyResult;
import com.sunwinkr.minigame.engine.port.TransKind;
import com.sunwinkr.minigame.engine.port.WalletPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * Admin TaiXiu unsettle endpoint — role-gated by
 * {@link com.sunwinkr.minigame.api.security.RoleResolver#ROLE_MINIGAME_ADMIN}
 * via the {@link com.sunwinkr.minigame.api.config.SecurityConfig} chain
 * (path pattern {@code /api/v2/admin/taixiu/**}).
 *
 * <h3>SUN-1339 Phase B2 — unsettleBet</h3>
 *
 * <pre>POST /api/v2/admin/taixiu/unsettle
 * Body: { "ticketId": 123, "reason": "player dispute" }</pre>
 *
 * <h3>Flow</h3>
 * <ol>
 *   <li>Look up {@code taixiu_bet} row by {@code ticketId}. 404 if absent.</li>
 *   <li>Reject 400 {@code NOT_SETTLED} if {@code settle_status != 'SETTLED'}.</li>
 *   <li>Reverse wallet:
 *     <ul>
 *       <li>Winner had prize credited → debit prize back (house reclaims).</li>
 *       <li>Loser had bet debited (no prize) → credit bet back (refund).</li>
 *     </ul>
 *     Uses {@link WalletPort} with {@code source="Tài Xỉu Refund"},
 *     {@code gameId=2} (legacy TaiXiu game id), and an admin-supplied reason
 *     in the description field. Wallet failure logs WARN but does NOT abort
 *     the status flip — the admin can retry the wallet manually if needed;
 *     the status flip is authoritative.</li>
 *   <li>Flip {@code settle_status = 'VOIDED'} via {@link JdbcTaixiuBetSettlePort}.</li>
 *   <li>Write audit row via {@link JdbcTaixiuUnsettleAuditPort}.</li>
 * </ol>
 *
 * <p>Plan SUN-1339 §B2.
 */
@RestController
@RequestMapping("/api/v2/admin/taixiu")
public class AdminTaiXiuUnsettleController {

    private static final Logger LOG = LoggerFactory.getLogger(AdminTaiXiuUnsettleController.class);

    /** Legacy TaiXiu game id used in wallet transaction source. */
    private static final long GAME_ID_TAIXIU = 2L;

    /** Source tag for wallet transactions — Vietnamese per A2 convention. */
    private static final String SOURCE_TAIXIU_REFUND = "Tài Xỉu Refund";

    private final JdbcTaixiuBetSettlePort settlePort;
    private final JdbcTaixiuUnsettleAuditPort auditPort;
    private final WalletPort wallet;

    @Autowired
    public AdminTaiXiuUnsettleController(
            JdbcTaixiuBetSettlePort settlePort,
            JdbcTaixiuUnsettleAuditPort auditPort,
            @Qualifier("jdbcWalletPort") WalletPort wallet) {
        this.settlePort = settlePort;
        this.auditPort = auditPort;
        this.wallet = wallet;
    }

    /**
     * Unsettle a previously SETTLED TaiXiu bet.
     *
     * <p>Role guard: {@code ROLE_MINIGAME_ADMIN} (enforced by SecurityConfig).
     */
    @PostMapping("/unsettle")
    public ResponseEntity<Map<String, Object>> unsettleBet(
            @Valid @RequestBody UnsettleBetRequest req) {

        Map<String, Object> resp = new HashMap<>();

        if (req == null || req.ticketId == null) {
            resp.put("success", false);
            resp.put("errorCode", "4001");
            resp.put("message", "ticketId required");
            return ResponseEntity.badRequest().body(resp);
        }
        if (req.reason == null || req.reason.trim().isEmpty()) {
            resp.put("success", false);
            resp.put("errorCode", "4002");
            resp.put("message", "reason required");
            return ResponseEntity.badRequest().body(resp);
        }

        long betId = req.ticketId;
        String actor = currentNickname();

        // 1. Fetch bet row
        BetRow row;
        try {
            row = settlePort.findById(betId);
        } catch (JdbcTaixiuBetSettlePort.SettlePortException e) {
            LOG.error("unsettleBet: DB error fetching betId={} actor={}", betId, actor, e);
            resp.put("success", false);
            resp.put("errorCode", "9999");
            resp.put("message", "Internal error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
        }

        // 2. 404 if not found
        if (row == null) {
            resp.put("success", false);
            resp.put("errorCode", "1002");
            resp.put("message", "Ticket not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
        }

        // 3. 400 if not SETTLED
        if (!"SETTLED".equals(row.settleStatus)) {
            resp.put("success", false);
            resp.put("errorCode", "NOT_SETTLED");
            resp.put("message", "Bet is not in SETTLED state: " + row.settleStatus);
            return ResponseEntity.badRequest().body(resp);
        }

        String moneyType = row.moneyType == 1 ? "vin" : "xu";
        String desc = "Admin unsettle betId=" + betId + " reason=" + req.reason.trim();

        // 4. Reverse wallet
        //    Winner (prize > 0): debit the prize that was credited to player
        //    Loser  (prize == 0): credit the bet back (refund)
        long walletReversalAmount = row.prize > 0 ? row.prize : row.betValue;
        boolean isWinner = row.prize > 0;
        long txId = betId; // reuse betId as txId for idempotency tracking

        try {
            MoneyResult walletResult;
            if (isWinner) {
                // Winner had prize credited → debit it back
                walletResult = wallet.debit(
                    row.nickname,
                    walletReversalAmount,
                    moneyType,
                    SOURCE_TAIXIU_REFUND,
                    GAME_ID_TAIXIU,
                    desc,
                    0L,
                    txId,
                    TransKind.END);
            } else {
                // Loser had bet debited, no prize → credit bet back
                walletResult = wallet.credit(
                    row.nickname,
                    walletReversalAmount,
                    moneyType,
                    SOURCE_TAIXIU_REFUND,
                    GAME_ID_TAIXIU,
                    desc,
                    0L,
                    txId,
                    TransKind.END);
            }
            if (!walletResult.isSuccess()) {
                LOG.warn("unsettleBet: wallet reversal failed betId={} nick={} amount={} errorCode={}",
                    betId, row.nickname, walletReversalAmount, walletResult.getErrorCode());
                // Proceed with status flip — admin can reconcile wallet manually
            }
        } catch (Throwable t) {
            LOG.warn("unsettleBet: wallet reversal threw betId={} nick={}", betId, row.nickname, t);
            // Proceed — don't abort the status flip on wallet error
        }

        // 5. Flip settle_status → VOIDED
        boolean flipped;
        try {
            flipped = settlePort.markVoided(betId);
        } catch (JdbcTaixiuBetSettlePort.SettlePortException e) {
            LOG.error("unsettleBet: markVoided DB error betId={} actor={}", betId, actor, e);
            resp.put("success", false);
            resp.put("errorCode", "9999");
            resp.put("message", "Internal error during status flip");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
        }

        if (!flipped) {
            // Concurrent unsettle raced ahead
            resp.put("success", false);
            resp.put("errorCode", "NOT_SETTLED");
            resp.put("message", "Bet status changed concurrently — no longer SETTLED");
            return ResponseEntity.badRequest().body(resp);
        }

        // 6. Audit log (fire-and-forget; failure logged by auditPort)
        auditPort.writeAudit(betId, actor, req.reason.trim(), walletReversalAmount);

        LOG.info("unsettleBet: VOIDED betId={} nick={} amount={} actor={} reason={}",
            betId, row.nickname, walletReversalAmount, actor, req.reason.trim());

        resp.put("success", true);
        resp.put("ticketId", betId);
        resp.put("nickname", row.nickname);
        resp.put("newStatus", "VOIDED");
        resp.put("reversalAmount", walletReversalAmount);
        resp.put("moneyType", moneyType);
        resp.put("actor", actor);
        return ResponseEntity.ok(resp);
    }

    /** Read admin nickname from Spring Security context. */
    private static String currentNickname() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return "unknown";
        }
        String p = auth.getPrincipal().toString();
        return (p.isEmpty() || "anonymousUser".equals(p)) ? "unknown" : p;
    }
}
