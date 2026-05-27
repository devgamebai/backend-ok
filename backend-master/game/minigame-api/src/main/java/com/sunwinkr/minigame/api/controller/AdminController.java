package com.sunwinkr.minigame.api.controller;

import com.sunwinkr.minigame.api.dto.ForceResultRequest;
import com.sunwinkr.minigame.api.dto.KillSwitchRequest;
import com.sunwinkr.minigame.engine.core.RevealGuard;
import com.sunwinkr.minigame.engine.core.TaiXiuRound;
import com.sunwinkr.minigame.engine.port.ForceResultStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Admin endpoints — role-gated by {@link com.sunwinkr.minigame.api.security
 * .RoleResolver#ROLE_MINIGAME_ADMIN} via the
 * {@link com.sunwinkr.minigame.api.config.SecurityConfig} chain.
 *
 * <h3>MED-1 fix</h3>
 * Legacy force-result was guarded by
 * {@code user.getName().contains("superadmin")} (TXM:259) which let any
 * username containing the substring through. The Spring chain now
 * enforces explicit role authority — the substring path is gone.
 *
 * <ul>
 *   <li>{@code POST /api/v2/admin/taixiu/force-result}  — write
 *       {@code ketquataixiu} HZ map; engine consumes atomically next round</li>
 *   <li>{@code POST /api/v2/admin/taixiu/kill-switch}   — pause/resume the engine</li>
 *   <li>{@code GET  /api/v2/admin/taixiu/round-state}   — full diagnostics</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v2/admin/taixiu")
public class AdminController {

    private static final Logger LOG = LoggerFactory.getLogger(AdminController.class);

    private final ForceResultStore forceStore;
    private final TaiXiuRound round;
    private final AtomicBoolean paused = new AtomicBoolean(false);

    @Autowired
    public AdminController(@org.springframework.beans.factory.annotation.Qualifier("hazelcastForceResultStore") ForceResultStore forceStore, TaiXiuRound round) {
        this.forceStore = forceStore;
        this.round = round;
    }

    @PostMapping("/force-result")
    public ResponseEntity<Map<String, Object>> forceResult(@Valid @RequestBody ForceResultRequest req) {
        Map<String, Object> resp = new HashMap<>();
        if (req == null || req.side == null || (req.side != 0 && req.side != 1)) {
            resp.put("success", false);
            resp.put("message", "side must be 0 or 1");
            return ResponseEntity.badRequest().body(resp);
        }
        short[] dice = generateForSide(req.side);
        forceStore.set(dice);
        String role = currentRole();
        RevealGuard.adminTrace(role, dice, "ForceResult");
        LOG.info("Admin force-result side={} role={} dice=[{},{},{}]",
            req.side, role, dice[0], dice[1], dice[2]);
        resp.put("success", true);
        resp.put("dice", new short[] { dice[0], dice[1], dice[2] });
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/kill-switch")
    public ResponseEntity<Map<String, Object>> killSwitch(@Valid @RequestBody KillSwitchRequest req) {
        Map<String, Object> resp = new HashMap<>();
        if (req == null || req.paused == null) {
            resp.put("success", false);
            return ResponseEntity.badRequest().body(resp);
        }
        boolean was = paused.getAndSet(req.paused);
        LOG.info("Admin kill-switch paused: {} -> {} by={}", was, req.paused, currentRole());
        resp.put("success", true);
        resp.put("paused", req.paused);
        resp.put("previous", was);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/round-state")
    public ResponseEntity<Map<String, Object>> roundState() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("referenceId", round.referenceId());
        resp.put("phase", round.phase().name());
        resp.put("startTimeMs", round.startTimeMs());
        resp.put("lastTickMs", round.lastTickMs());
        resp.put("paused", paused.get());
        return ResponseEntity.ok(resp);
    }

    /** Generate three dice in [1..6] whose sum maps to the requested side. */
    static short[] generateForSide(int side) {
        ThreadLocalRandom rd = ThreadLocalRandom.current();
        short[] dice;
        int total;
        int safety = 0;
        do {
            dice = new short[] {
                (short) (rd.nextInt(6) + 1),
                (short) (rd.nextInt(6) + 1),
                (short) (rd.nextInt(6) + 1)
            };
            total = dice[0] + dice[1] + dice[2];
            if (++safety > 100) {
                break;
            }
        } while ((total > 10 ? 1 : 0) != side);
        return dice;
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
