package com.sunwinkr.lottery.api.controller;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.sunwinkr.lottery.api.dto.BetRequestDto;
import com.sunwinkr.lottery.api.dto.BetResponseDto;
import com.sunwinkr.lottery.api.dto.HistoryDto;
import com.sunwinkr.lottery.api.dto.ResultDto;
import com.sunwinkr.lottery.api.dto.StateDto;
import com.sunwinkr.lottery.api.security.RoleResolver;
import com.sunwinkr.lottery.engine.bet.BetAcceptResult;
import com.sunwinkr.lottery.engine.bet.BetAcceptor;
import com.sunwinkr.lottery.engine.bet.BetRequest;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.sunwinkr.lottery.engine.clock.LotteryClock;
import com.sunwinkr.lottery.engine.clock.LotteryPhase;
import com.sunwinkr.lottery.engine.model.LotteryResult;
import com.sunwinkr.lottery.engine.model.LotteryTicket;
import com.sunwinkr.lottery.engine.port.BetStore;
import com.sunwinkr.lottery.engine.port.ResultStore;
import com.sunwinkr.lottery.engine.port.SettledFlagStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Player-facing XSMB lottery endpoints. Plan §5.1.
 *
 * <ul>
 *   <li>{@code POST /api/v2/lottery/xsmb/bet}            — place a bet via {@link BetAcceptor}</li>
 *   <li>{@code GET  /api/v2/lottery/xsmb/state}          — phase + bettingOpen + lock/scrape clock</li>
 *   <li>{@code GET  /api/v2/lottery/xsmb/result/{date}}  — 404 if settled_at IS NULL else ResultDto</li>
 *   <li>{@code GET  /api/v2/lottery/xsmb/results?from=&to=} — settled-only list</li>
 *   <li>{@code GET  /api/v2/lottery/xsmb/history?userId=}   — caller==userId OR admin</li>
 *   <li>{@code GET  /api/v2/lottery/products}            — public catalog stub</li>
 * </ul>
 *
 * <p>Authentication comes from {@link com.sunwinkr.lottery.api.security
 * .AccessTokenFilter} which binds the nickname into the Spring Security
 * context. All endpoints except {@code /products} require an
 * authenticated principal.
 */
@RestController
@RequestMapping("/api/v2/lottery")
public class XsmbController {

    private static final Logger LOG = LoggerFactory.getLogger(XsmbController.class);

    private final BetAcceptor betAcceptor;
    private final BetStore bets;
    private final ResultStore results;
    private final SettledFlagStore settledFlag;
    private final Clock clock;
    private final HazelcastInstance hazelcast;

    public XsmbController(BetAcceptor betAcceptor,
                          BetStore bets,
                          ResultStore results,
                          SettledFlagStore settledFlag,
                          Clock clock,
                          HazelcastInstance hazelcast) {
        this.betAcceptor = betAcceptor;
        this.bets = bets;
        this.results = results;
        this.settledFlag = settledFlag;
        this.clock = clock;
        this.hazelcast = hazelcast;
    }

    /**
     * Resolve {@code vinplay.users.id} for a nickname via the Hazelcast
     * {@code users} cache map. The {@code lode.user_id} FK requires a real
     * users row; passing {@code 0L} causes the INSERT to fail with
     * {@code SQLIntegrityConstraintViolationException} after the wallet has
     * already been debited.
     */
    private long resolveUserId(String nickname) {
        try {
            IMap<String, UserCacheModel> users = hazelcast.getMap("users");
            UserCacheModel u = users.get(nickname);
            return (u != null) ? u.getId() : 0L;
        } catch (Throwable t) {
            LOG.warn("resolveUserId failed nickname={}", nickname, t);
            return 0L;
        }
    }

    @PostMapping("/xsmb/bet")
    public ResponseEntity<BetResponseDto> bet(@Valid @RequestBody BetRequestDto req) {
        // FE contract: always HTTP 200, errorCode in body — matches the rest of
        // the player APIs ({success:false, errorCode, ...}). Non-200 made the
        // FE think the call had failed at the transport layer.
        String nickname = currentNickname();
        if (nickname == null) {
            return ResponseEntity.ok(BetResponseDto.error("0001", 0L, describeError("0001")));
        }
        if (req == null || req.modeId == null || req.ticket == null) {
            return ResponseEntity.ok(BetResponseDto.error("0005", 0L, describeError("0005")));
        }
        // SUN-1339 A4: timestamp-based bet-window guard. Computes today's lock
        // instant (18:10 Hanoi) in epoch-ms and rejects if server now >= that
        // instant. This tightens the existing LotteryClock.isBettingOpen boolean
        // check so a 1-second clock skew cannot slip a bet through.
        //
        // Test-only kill switch: env var XSMB_DISABLE_BET_WINDOW=1 skips the
        // gate so QA / FE can probe the success (0000) and the cap (0006)
        // paths outside the natural 00:00–18:10 Hanoi window. Unset by
        // default — production behaviour stays unchanged.
        if (!"1".equals(System.getenv("XSMB_DISABLE_BET_WINDOW"))) {
            java.time.ZonedDateTime nowVN = java.time.ZonedDateTime.now(clock.withZone(LotteryClock.VN));
            long todayLockMs = java.time.ZonedDateTime.of(
                    nowVN.toLocalDate(), LotteryClock.LOCK_TIME, LotteryClock.VN)
                .toInstant().toEpochMilli();
            if (clock.millis() >= todayLockMs) {
                return ResponseEntity.ok(BetResponseDto.error("0002", 0L, describeError("0002")));
            }
        }
        // SUN-1338: resolve real users.id BEFORE accepting the bet — otherwise
        // JdbcBetStore.insert hits SQLIntegrityConstraintViolationException on
        // fk_lode_user AFTER the wallet has been debited.
        long userId = resolveUserId(nickname);
        if (userId == 0L) {
            return ResponseEntity.ok(BetResponseDto.error("0001", 0L, describeError("0001")));
        }
        BetRequest engineReq = new BetRequest(
            nickname,
            userId,
            req.modeId,
            req.ticket,
            req.betValue,
            req.clientNonce);
        BetAcceptResult result = betAcceptor.accept(engineReq);
        if (result.isSuccess()) {
            return ResponseEntity.ok(BetResponseDto.ok(result.getCurrentMoney(), result.getTicketId()));
        }
        return ResponseEntity.ok(
            BetResponseDto.error(result.getErrorCode(), result.getCurrentMoney(),
                describeError(result.getErrorCode())));
    }

    @GetMapping("/xsmb/state")
    public ResponseEntity<StateDto> state() {
        String nickname = currentNickname();
        if (nickname == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        LocalDate vnToday = LocalDate.now(clock.withZone(LotteryClock.VN));
        boolean settled = settledFlag.isSettled(vnToday);
        boolean open = LotteryClock.isBettingOpen(clock, settled);
        LotteryPhase phase = derivePhase(open, settled);
        long lockEpochMs = nextEpochMs(LotteryClock.LOCK_TIME);
        long scrapeEpochMs = nextEpochMs(LotteryClock.SCRAPE_TIME);
        StateDto d = new StateDto();
        d.phase = phase.name();
        d.bettingOpen = open;
        d.lockTime = lockEpochMs;
        d.safeBetExpiresAt = lockEpochMs;
        d.scrapeTime = scrapeEpochMs;
        d.settleAt = scrapeEpochMs;
        d.vnDate = vnToday.toString();
        d.roundId = roundIdForDate(vnToday);
        return ResponseEntity.ok(d);
    }

    /**
     * L-1 gate: result hidden until {@code settled_at IS NOT NULL}.
     *
     * <p>Response semantics:
     * <ul>
     *   <li>Past date + row settled → 200 OK with full result payload</li>
     *   <li>Past date + no settled row → 404 (truly missing draw)</li>
     *   <li>Date == today + not yet drawn → 200 OK with
     *       {@code {success:false, errorCode:"DRAW_PENDING", ...}}
     *       so the FE can render "kết quả chưa có" instead of treating
     *       a plain 404 as a generic error.</li>
     *   <li>Future date → 400 invalid_date</li>
     * </ul>
     */
    @GetMapping("/xsmb/result/{date}")
    public ResponseEntity<Object> result(@PathVariable("date") String date) {
        String nickname = currentNickname();
        if (nickname == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        LocalDate vnDate;
        try {
            vnDate = LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            Map<String, Object> body = new HashMap<>();
            body.put("success", false);
            body.put("errorCode", "INVALID_DATE");
            body.put("message", "Định dạng ngày không hợp lệ (yyyy-MM-dd)");
            return ResponseEntity.badRequest().body(body);
        }
        LocalDate vnToday = LocalDate.now(clock.withZone(LotteryClock.VN));
        if (!settledFlag.isSettled(vnDate)) {
            // Distinguish "draw not yet" from "really missing".
            Map<String, Object> body = new HashMap<>();
            body.put("success", false);
            body.put("date", vnDate.toString());
            if (vnDate.isAfter(vnToday)) {
                body.put("errorCode", "DRAW_FUTURE");
                body.put("message", "Ngày " + vnDate + " ở tương lai — chưa có phiên xổ số");
                return ResponseEntity.badRequest().body(body);
            }
            if (vnDate.equals(vnToday)) {
                body.put("errorCode", "DRAW_PENDING");
                body.put("message", "Kết quả XSMB ngày " + vnDate
                        + " chưa có. Phiên dự kiến chốt lúc 18:36 (giờ Hà Nội).");
                body.put("scheduledScrapeTime", nextEpochMs(LotteryClock.SCRAPE_TIME));
                return ResponseEntity.ok(body);
            }
            body.put("errorCode", "DRAW_MISSING");
            body.put("message", "Không có kết quả XSMB cho ngày " + vnDate + " (phiên bị bỏ lỡ?)");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
        return results.findByDate(vnDate)
            .<ResponseEntity<Object>>map(r -> ResponseEntity.ok((Object) ResultDto.fromResult(r)))
            .orElseGet(() -> {
                Map<String, Object> body = new HashMap<>();
                body.put("success", false);
                body.put("errorCode", "DRAW_MISSING");
                body.put("date", vnDate.toString());
                body.put("message", "settled_at flag set but result row missing — ops anomaly");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
            });
    }

    @GetMapping("/xsmb/results")
    public ResponseEntity<List<ResultDto>> results(@RequestParam(value = "from", required = false) String from,
                                                    @RequestParam(value = "to", required = false) String to) {
        String nickname = currentNickname();
        if (nickname == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        LocalDate vnToday = LocalDate.now(clock.withZone(LotteryClock.VN));
        LocalDate fromDate;
        LocalDate toDate;
        try {
            fromDate = (from == null || from.isEmpty()) ? vnToday.minusDays(30) : LocalDate.parse(from);
            toDate = (to == null || to.isEmpty()) ? vnToday : LocalDate.parse(to);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().build();
        }
        // ResultStore.listSettled filters settled_at IS NOT NULL (audit L-1).
        List<LotteryResult> raws = results.listSettled(fromDate, toDate);
        List<ResultDto> wire = new ArrayList<>(raws.size());
        for (LotteryResult r : raws) {
            wire.add(ResultDto.fromResult(r));
        }
        return ResponseEntity.ok(wire);
    }

    @GetMapping("/xsmb/history")
    public ResponseEntity<HistoryDto> history(@RequestParam(value = "userId", required = false) String userId,
                                                @RequestParam(value = "limit", defaultValue = "50") int limit,
                                                @RequestParam(value = "offset", defaultValue = "0") int offset) {
        String nickname = currentNickname();
        if (nickname == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // Access control: caller must be the requested user OR an admin.
        String target = (userId == null || userId.isEmpty()) ? nickname : userId;
        if (!target.equals(nickname) && !currentIsAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        int cappedLimit = Math.max(1, Math.min(limit, 200));
        int cappedOffset = Math.max(0, offset);
        long total = bets.countByUser(target);
        List<LotteryTicket> ticks = bets.findByUser(target, new BetStore.Paging(cappedOffset, cappedLimit));
        List<HistoryDto.Entry> entries = new ArrayList<>(ticks.size());
        for (LotteryTicket t : ticks) {
            entries.add(HistoryDto.Entry.fromTicket(t));
        }
        return ResponseEntity.ok(new HistoryDto(entries, total, cappedOffset, cappedLimit));
    }

    /** Public — catalog stub (plan §5.1). */
    @GetMapping("/products")
    public ResponseEntity<List<Map<String, Object>>> products() {
        Map<String, Object> p = new HashMap<>();
        p.put("code", "xsmb");
        p.put("name", "Xổ Số Miền Bắc");
        p.put("lockTime", nextEpochMs(LotteryClock.LOCK_TIME));
        p.put("scrapeTime", nextEpochMs(LotteryClock.SCRAPE_TIME));
        p.put("timezone", LotteryClock.VN.getId());
        return ResponseEntity.ok(Collections.singletonList(p));
    }

    // ---------- helpers ----------

    /**
     * Round identifier for a given Vietnamese-wall date as a {@code yyyymmdd}
     * numeric (e.g. {@code 20260515}). Uses {@link LotteryClock#VN} — never
     * the JVM default — consistent with the TZ policy in {@link LotteryClock}.
     */
    private static long roundIdForDate(LocalDate vnDate) {
        return Long.parseLong(vnDate.format(DateTimeFormatter.BASIC_ISO_DATE));
    }

    /**
     * Returns the next occurrence of {@code wallTime} (Hanoi) as Unix epoch ms.
     * If {@code now} (in VN) is before today's {@code wallTime}, returns today;
     * else returns tomorrow. Used for FE-side countdown rendering.
     */
    private long nextEpochMs(java.time.LocalTime wallTime) {
        java.time.ZonedDateTime nowVN = java.time.ZonedDateTime.now(clock.withZone(LotteryClock.VN));
        java.time.ZonedDateTime targetToday = java.time.ZonedDateTime.of(
                nowVN.toLocalDate(), wallTime, LotteryClock.VN);
        java.time.ZonedDateTime target = nowVN.isBefore(targetToday)
                ? targetToday
                : targetToday.plusDays(1);
        return target.toInstant().toEpochMilli();
    }

    private static LotteryPhase derivePhase(boolean bettingOpen, boolean todaySettled) {
        if (bettingOpen) {
            return todaySettled ? LotteryPhase.SETTLED : LotteryPhase.DRAW_PENDING;
        }
        // Bets closed. SETTLED only if today's row is flagged; otherwise we're in
        // the [18:10..settle complete] window — locked but yet to scrape.
        return todaySettled ? LotteryPhase.SETTLED : LotteryPhase.DRAW_LOCKED;
    }

    private static String currentNickname() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal == null) return null;
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            return ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
        }
        String s = principal.toString();
        if (s.isEmpty() || "anonymousUser".equals(s)) return null;
        return s;
    }

    private static boolean currentIsAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) return false;
        for (GrantedAuthority a : auth.getAuthorities()) {
            if (RoleResolver.ROLE_LOTTERY_ADMIN.equals(a.getAuthority())) return true;
        }
        return false;
    }

    private static String describeError(String code) {
        // FE alignment (2026-05-18): return Vietnamese raw text so the
        // client can render `message` directly with no code-to-string map.
        // Shared registry in VbeeCommon — see GameBetErrors javadoc.
        return com.vinplay.vbee.common.error.GameBetErrors.message(
                com.vinplay.vbee.common.error.GameBetErrors.Game.XSMB, code);
    }
}
