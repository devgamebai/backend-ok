package com.sunwinkr.lottery.api.controller;

import com.sunwinkr.lottery.api.dto.AdminSettleRequestDto;
import com.sunwinkr.lottery.api.dto.HistoryDto;
import com.sunwinkr.lottery.api.dto.SearchResultDto;
import com.sunwinkr.lottery.api.dto.UnsettleRequestDto;
import com.sunwinkr.lottery.api.push.SettleAnnouncePublisher;
import com.sunwinkr.lottery.engine.ingest.DrawIngest;
// SettleAnnouncePublisher injected; fires announce after markSettled.
import com.sunwinkr.lottery.engine.model.LotteryResult;
import com.sunwinkr.lottery.engine.model.LotteryTicket;
import com.sunwinkr.lottery.engine.model.LotteryMode;
import com.sunwinkr.lottery.engine.model.SettleStatus;
import com.sunwinkr.lottery.engine.port.BetStore;
import com.sunwinkr.lottery.engine.port.MoneyResult;
import com.sunwinkr.lottery.engine.port.ResultStore;
import com.sunwinkr.lottery.engine.port.SettledFlagStore;
import com.sunwinkr.lottery.engine.port.TransKind;
import com.sunwinkr.lottery.engine.port.WalletPort;
import com.sunwinkr.lottery.engine.settle.LotterySettleService;
import com.sunwinkr.lottery.engine.settle.SettleSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Admin lottery endpoints — role-gated by
 * {@link com.sunwinkr.lottery.api.security.RoleResolver#ROLE_LOTTERY_ADMIN}
 * via the {@link com.sunwinkr.lottery.api.config.SecurityConfig} filter
 * chain.
 *
 * <h3>MED-1 fix</h3>
 * Legacy admin checks substring-matched {@code user.getName().contains("superadmin")}
 * which let any nickname containing the substring pass. The Spring chain
 * now enforces explicit role authority — the substring path is gone.
 *
 * <ul>
 *   <li>{@code POST /api/v2/lottery/admin/xsmb/settle}      — idempotent settle for a date</li>
 *   <li>{@code POST /api/v2/lottery/admin/xsmb/rescrape}    — dual-control wrapper (audit §5.8)</li>
 *   <li>{@code GET  /api/v2/lottery/admin/transactions}     — PreparedStatement-backed search</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v2/lottery/admin")
public class AdminLotteryController {

    private static final Logger LOG = LoggerFactory.getLogger(AdminLotteryController.class);

    private final LotterySettleService settleService;
    private final DrawIngest drawIngest;
    private final ResultStore results;
    private final SettledFlagStore settledFlag;
    private final BetStore bets;
    private final SettleAnnouncePublisher announcer;
    private final WalletPort walletPort;

    public AdminLotteryController(LotterySettleService settleService,
                                   DrawIngest drawIngest,
                                   ResultStore results,
                                   SettledFlagStore settledFlag,
                                   BetStore bets,
                                   SettleAnnouncePublisher announcer,
                                   WalletPort walletPort) {
        this.settleService = settleService;
        this.drawIngest = drawIngest;
        this.results = results;
        this.settledFlag = settledFlag;
        this.bets = bets;
        this.announcer = announcer;
        this.walletPort = walletPort;
    }

    /**
     * Idempotent settle. If the day is already settled, returns the count
     * of tickets already settled for the date as {@code settledCount}.
     * Otherwise runs the settle loop and returns the fresh count.
     */
    @PostMapping("/xsmb/settle")
    public ResponseEntity<Map<String, Object>> settle(@Valid @RequestBody AdminSettleRequestDto req) {
        Map<String, Object> resp = new HashMap<>();
        if (req == null || req.date == null) {
            resp.put("success", false);
            resp.put("message", "date required");
            return ResponseEntity.badRequest().body(resp);
        }
        LocalDate vnDate;
        try {
            vnDate = LocalDate.parse(req.date);
        } catch (DateTimeParseException e) {
            resp.put("success", false);
            resp.put("message", "invalid date format (expected yyyy-MM-dd)");
            return ResponseEntity.badRequest().body(resp);
        }

        String actor = currentNickname();
        LOG.info("Admin settle for date={} by actor={}", vnDate, actor);

        // Idempotent — second call on a settled day returns the existing count.
        if (settledFlag.isSettled(vnDate)) {
            long existing = bets.count(new BetStore.SearchFilter(
                Optional.<String>empty(),
                Optional.<String>empty(),
                Optional.<Integer>empty(),
                Optional.of(vnDate),
                Optional.of(vnDate),
                new BetStore.Paging(0, 1)));
            resp.put("success", true);
            resp.put("alreadySettled", true);
            resp.put("settledCount", existing);
            return ResponseEntity.ok(resp);
        }

        Optional<LotteryResult> draw = results.findByDate(vnDate);
        if (!draw.isPresent()) {
            resp.put("success", false);
            resp.put("message", "no draw payload persisted for " + vnDate);
            return ResponseEntity.badRequest().body(resp);
        }
        SettleSummary summary = settleService.settleAll(vnDate, draw.get());
        settledFlag.markSettled(vnDate);
        // Plan §5.4 — one-shot announce. NO result fields in payload.
        announcer.announceSettled(vnDate);
        resp.put("success", true);
        resp.put("alreadySettled", false);
        resp.put("settledCount", summary.getSettledCount());
        resp.put("failureCount", summary.getFailureCount());
        resp.put("totalPrizeCredited", summary.getTotalPrizeCredited());
        return ResponseEntity.ok(resp);
    }

    /**
     * Rescrape today's draw. Dual-control: requires {@code secondApproverNickname}
     * to be present AND different from the calling admin. Audit §5.8.
     */
    @PostMapping("/xsmb/rescrape")
    public ResponseEntity<Map<String, Object>> rescrape(@Valid @RequestBody AdminSettleRequestDto req) {
        Map<String, Object> resp = new HashMap<>();
        String actor = currentNickname();
        if (req == null || req.date == null) {
            resp.put("success", false);
            resp.put("message", "date required");
            return ResponseEntity.badRequest().body(resp);
        }
        // Dual-control gate.
        if (req.secondApproverNickname == null || req.secondApproverNickname.isEmpty()) {
            resp.put("success", false);
            resp.put("message", "dual-control: secondApproverNickname required");
            return ResponseEntity.badRequest().body(resp);
        }
        if (actor != null && actor.equals(req.secondApproverNickname)) {
            resp.put("success", false);
            resp.put("message", "dual-control: second approver must be a different admin");
            return ResponseEntity.badRequest().body(resp);
        }
        LocalDate vnDate;
        try {
            vnDate = LocalDate.parse(req.date);
        } catch (DateTimeParseException e) {
            resp.put("success", false);
            resp.put("message", "invalid date format (expected yyyy-MM-dd)");
            return ResponseEntity.badRequest().body(resp);
        }
        LOG.info("Admin rescrape for date={} by actor={} secondApprover={}",
            vnDate, actor, req.secondApproverNickname);
        DrawIngest.IngestSummary summary = drawIngest.runOnce(vnDate);
        resp.put("success", !summary.scrapeFailed);
        resp.put("newScrapePersisted", summary.newScrapePersisted);
        resp.put("settledCount", summary.settledCount);
        resp.put("failureCount", summary.failureCount);
        resp.put("scrapeFailed", summary.scrapeFailed);
        return ResponseEntity.ok(resp);
    }

    /**
     * PreparedStatement-backed search — H3 fix. Filter values flow through
     * the strongly typed {@link BetStore.SearchFilter} → JDBC bindings
     * only. {@code ' OR 1=1 --}-style payloads land in a parameterized
     * {@code nick_name = ?} clause that returns zero rows.
     */
    @GetMapping("/transactions")
    public ResponseEntity<SearchResultDto> transactions(
            @RequestParam(value = "nickname", required = false) String nickname,
            @RequestParam(value = "ticket", required = false) String ticket,
            @RequestParam(value = "modeId", required = false) Integer modeId,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        Optional<LocalDate> fromDate = Optional.empty();
        Optional<LocalDate> toDate = Optional.empty();
        try {
            if (from != null && !from.isEmpty()) fromDate = Optional.of(LocalDate.parse(from));
            if (to != null && !to.isEmpty()) toDate = Optional.of(LocalDate.parse(to));
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().build();
        }
        int cappedLimit = Math.max(1, Math.min(limit, 500));
        int cappedOffset = Math.max(0, offset);
        BetStore.SearchFilter filter = new BetStore.SearchFilter(
            optStr(nickname),
            optStr(ticket),
            Optional.ofNullable(modeId),
            fromDate,
            toDate,
            new BetStore.Paging(cappedOffset, cappedLimit));
        List<LotteryTicket> ticks = bets.search(filter);
        long total = bets.count(filter);
        List<HistoryDto.Entry> entries = new ArrayList<>(ticks.size());
        for (LotteryTicket t : ticks) {
            entries.add(HistoryDto.Entry.fromTicket(t));
        }
        return ResponseEntity.ok(new SearchResultDto(entries, total, cappedOffset, cappedLimit));
    }

    /**
     * Admin un-settle a previously settled Lô Đề ticket.
     *
     * <p>POST {@code /api/v2/lottery/xsmb/admin/unsettle}
     * body: {@code { "ticketId": 123, "reason": "..." }}
     *
     * <ul>
     *   <li>404 if ticket not found.</li>
     *   <li>400 {@code NOT_SETTLED} if {@code settle_status != 'SETTLED'}.</li>
     *   <li>Reverses wallet: winners debited (prize taken back); losers credited
     *       (original bet refunded).</li>
     *   <li>Flips {@code settle_status} to {@code 'VOIDED'} atomically (conditional
     *       UPDATE WHERE settle_status = 'SETTLED').</li>
     * </ul>
     */
    @PostMapping("/xsmb/unsettle")
    public ResponseEntity<Map<String, Object>> unsettle(@Valid @RequestBody UnsettleRequestDto req) {
        Map<String, Object> resp = new HashMap<>();
        if (req == null || req.ticketId == null) {
            resp.put("success", false);
            resp.put("message", "ticketId required");
            return ResponseEntity.badRequest().body(resp);
        }
        String reason = (req.reason != null && !req.reason.trim().isEmpty())
                ? req.reason.trim() : "(no reason)";
        String actor = currentNickname();
        LOG.info("Admin unsettle ticketId={} by actor={} reason={}", req.ticketId, actor, reason);

        Optional<LotteryTicket> found = bets.findById(req.ticketId);
        if (!found.isPresent()) {
            resp.put("success", false);
            resp.put("errorCode", "NOT_FOUND");
            resp.put("message", "ticket " + req.ticketId + " not found");
            return ResponseEntity.status(404).body(resp);
        }
        LotteryTicket t = found.get();
        if (!SettleStatus.SETTLED.equals(t.getSettleStatus())) {
            resp.put("success", false);
            resp.put("errorCode", "NOT_SETTLED");
            resp.put("message", "ticket " + req.ticketId + " is not in SETTLED state (current: " + t.getSettleStatus() + ")");
            return ResponseEntity.badRequest().body(resp);
        }

        // Build a description that flows into the log_money_user_vin audit row.
        LotteryMode mode = LotteryMode.byId(t.getModeId()).orElse(null);
        String modeName = mode != null ? mode.getName() : ("Mode " + t.getModeId());
        String detail = modeName + " — số " + t.getTicket();
        String description = "Hoàn cược Lô Đề (" + detail + ") — Refund từ admin: " + reason;
        long txId = System.currentTimeMillis();

        Long prize = t.getPrize(); // null = was never settled properly (guard below)
        long prizeVal = prize != null ? prize : 0L;

        MoneyResult walletResult;
        if (prizeVal > 0L) {
            // Winner reversal: debit the credited prize back from the player's wallet.
            walletResult = walletPort.debit(
                    t.getNickname(),
                    prizeVal,
                    "vin",
                    "LoDe Refund",
                    "Lô Đề",
                    description,
                    0L,
                    txId,
                    TransKind.START);
        } else {
            // Loser reversal: credit the original bet amount back to the player's wallet.
            walletResult = walletPort.credit(
                    t.getNickname(),
                    t.getBetValue(),
                    "vin",
                    "LoDe Refund",
                    "Lô Đề",
                    description,
                    0L,
                    txId,
                    TransKind.START);
        }

        if (!walletResult.isSuccess()) {
            LOG.error("Admin unsettle wallet reversal failed ticketId={} errorCode={}",
                    req.ticketId, walletResult.getErrorCode());
            resp.put("success", false);
            resp.put("errorCode", "WALLET_ERROR");
            resp.put("message", "wallet reversal failed: " + walletResult.getErrorCode());
            return ResponseEntity.status(500).body(resp);
        }

        // Flip settle_status to VOIDED. Conditional UPDATE — 0 rows means race or already voided.
        int rows = bets.voidTicket(req.ticketId);
        if (rows == 0) {
            LOG.warn("Admin unsettle voidTicket returned 0 rows — possible race ticketId={}", req.ticketId);
            resp.put("success", false);
            resp.put("errorCode", "VOID_RACE");
            resp.put("message", "ticket was not in SETTLED state at time of void (concurrent modification?)");
            return ResponseEntity.status(409).body(resp);
        }

        resp.put("success", true);
        resp.put("ticketId", req.ticketId);
        resp.put("voidedPrize", prizeVal);
        resp.put("refundedBet", prizeVal > 0L ? 0L : t.getBetValue());
        resp.put("walletBalance", walletResult.getCurrentMoney());
        return ResponseEntity.ok(resp);
    }

    private static Optional<String> optStr(String s) {
        if (s == null) return Optional.empty();
        String t = s.trim();
        return t.isEmpty() ? Optional.<String>empty() : Optional.of(t);
    }

    static String currentNickname() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        // In production AccessTokenFilter binds a String principal (nickname).
        // In @WithMockUser tests the principal is a UserDetails — handle both.
        Object principal = auth.getPrincipal();
        if (principal == null) return null;
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            return ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
        }
        String s = principal.toString();
        if (s.isEmpty() || "anonymousUser".equals(s)) return null;
        return s;
    }
}
