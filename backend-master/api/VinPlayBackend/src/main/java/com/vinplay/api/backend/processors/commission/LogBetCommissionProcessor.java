package com.vinplay.api.backend.processors.commission;

import com.vinplay.dal.rebate.RealTimeCommission;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * SUN-1054 — HTTP bridge for cross-stack commission.
 *
 * <p>BanCa (C# .NET) and any other non-Java game server with its own
 * currency posts one event per session end to this command. The processor
 * hands off to {@link RealTimeCommission#calculate(String, long, String)}
 * — the same entry point every Java game uses — so commission cascades,
 * agent hierarchy walks, and rebate_logs writes all happen through a
 * single implementation.
 *
 * <p><b>Command ID:</b> 9854. Lives on {@code backend-api} (internal
 * admin/agent surface; not reachable from the public internet).
 *
 * <p><b>Auth:</b> shared secret header {@code X-Service-Token} must match
 * env {@code BANCA_SERVICE_TOKEN}. This is service-to-service — no player
 * aat / admin aat involved. If the env var is unset we reject every call
 * (fail closed).
 *
 * <p><b>Kill switch:</b> env {@code BANCA_COMMISSION_ENABLED}. Default
 * OFF (returns success=true, errorCode=0, reason="disabled") so the
 * processor can be merged and the endpoint exercised for wiring tests
 * without actually crediting agents. Flip to 1 when ready to go live.
 *
 * <p><b>Idempotency:</b> {@code session_id} (PK on
 * {@code banca_bet_commission_log}). A duplicate POST returns
 * {@code success=true, errorCode=0, reason="already_processed"} without
 * re-crediting. Safe under BanCa-side retry.
 *
 * <p><b>Input (JSON body):</b>
 * <pre>
 * {
 *   "nickname":         "laviai",
 *   "game_key":         "fish",
 *   "bet_amount":       870000,
 *   "session_id":       "bc-sess-42987",
 *   "deposit_in_vin":   100000,
 *   "withdraw_out_vin": 20000,
 *   "net_pl_vin":       -80000,
 *   "started_at":       1713740000,
 *   "ended_at":         1713750000
 * }
 * </pre>
 *
 * <p>Errors:
 * <table border="1">
 *   <tr><td>4001</td><td>missing required field</td></tr>
 *   <tr><td>4002</td><td>invalid bet_amount (must be &gt;= 0)</td></tr>
 *   <tr><td>1001</td><td>auth — X-Service-Token missing / wrong</td></tr>
 *   <tr><td>9999</td><td>internal — see logs</td></tr>
 * </table>
 */
public class LogBetCommissionProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        response.put("success", false);

        try {
            HttpServletRequest request = param.get();

            // --- Auth ---
            String expectedToken = System.getenv("BANCA_SERVICE_TOKEN");
            String providedToken = request.getHeader("X-Service-Token");
            if (expectedToken == null || expectedToken.isEmpty()) {
                return err(response, "1001", "service token not configured on server");
            }
            if (providedToken == null || !expectedToken.equals(providedToken)) {
                return err(response, "1001", "invalid service token");
            }

            // --- Parse JSON body ---
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = request.getReader()) {
                if (br != null) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }
            }
            String body = sb.toString().trim();
            if (body.isEmpty() || !body.startsWith("{")) {
                return err(response, "4001", "JSON body required");
            }

            JSONObject in;
            try {
                in = new JSONObject(body);
            } catch (Exception e) {
                return err(response, "4001", "invalid JSON: " + e.getMessage());
            }

            String nickname = in.optString("nickname", "").trim();
            String gameKey  = in.optString("game_key", "fish").trim();
            long betAmount  = in.optLong("bet_amount", -1L);
            String sessionId = in.optString("session_id", "").trim();

            if (nickname.isEmpty())   return err(response, "4001", "nickname required");
            if (sessionId.isEmpty())  return err(response, "4001", "session_id required");
            if (gameKey.isEmpty())    return err(response, "4001", "game_key required");
            if (betAmount < 0)        return err(response, "4002", "bet_amount must be >= 0");

            // Audit fields — never drive behaviour, only stored.
            long depositIn  = in.optLong("deposit_in_vin", 0L);
            long withdrawOut = in.optLong("withdraw_out_vin", 0L);
            long netPl      = in.optLong("net_pl_vin", 0L);
            long startedAt  = in.optLong("started_at", 0L);
            long endedAt    = in.optLong("ended_at", 0L);

            // --- Kill switch ---
            String enabled = System.getenv("BANCA_COMMISSION_ENABLED");
            boolean liveMode = enabled != null && (enabled.equals("1") || enabled.equalsIgnoreCase("true"));

            // --- Idempotency insert ---
            // INSERT IGNORE returns affectedRows=0 if session_id already exists.
            // We always record the audit row (even with kill switch off) so ops
            // can see what BanCa is sending before flipping the switch on.
            int affected;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT IGNORE INTO banca_bet_commission_log " +
                         "(session_id, nickname, game_key, bet_amount, " +
                         " deposit_in_vin, withdraw_out_vin, net_pl_vin, " +
                         " session_started_at, session_ended_at, client_ip) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, sessionId);
                ps.setString(2, nickname);
                ps.setString(3, gameKey);
                ps.setLong(4, betAmount);
                ps.setLong(5, depositIn);
                ps.setLong(6, withdrawOut);
                ps.setLong(7, netPl);
                ps.setTimestamp(8, startedAt > 0 ? new Timestamp(startedAt * 1000L) : null);
                ps.setTimestamp(9, endedAt > 0 ? new Timestamp(endedAt * 1000L) : null);
                ps.setString(10, PortalIp.clientIp(request));
                affected = ps.executeUpdate();
            }

            if (affected == 0) {
                // Duplicate — already processed, safe no-op.
                response.put("success", true);
                response.put("errorCode", "0");
                response.put("reason", "already_processed");
                return response.toString();
            }

            // --- Fire the commission cascade (or skip under kill switch) ---
            if (!liveMode) {
                response.put("success", true);
                response.put("errorCode", "0");
                response.put("reason", "disabled");
                response.put("note", "BANCA_COMMISSION_ENABLED!=1 — audit row inserted, commission not credited");
                return response.toString();
            }

            if (betAmount == 0) {
                // Zero-turnover session — audit it but skip the commission call.
                // RealTimeCommission itself would no-op here, saving ourselves the
                // hierarchy walk.
                response.put("success", true);
                response.put("errorCode", "0");
                response.put("reason", "zero_bet");
                return response.toString();
            }

            // Same call LogMoneyUserExtraProcessor makes for Java-game bets.
            RealTimeCommission.calculate(nickname, betAmount, gameKey);

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("reason", "processed");
            response.put("session_id", sessionId);
            response.put("bet_amount", betAmount);
        } catch (SQLException se) {
            logger.error("LogBetCommissionProcessor DB error", se);
            return err(response, "9999", "database: " + se.getMessage());
        } catch (Exception e) {
            logger.error("LogBetCommissionProcessor error", e);
            return err(response, "9999", "internal: " + e.getMessage());
        }
        return response.toString();
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false);
        r.put("errorCode", code);
        r.put("message", msg);
        return r.toString();
    }

    /** Minimal helper — extract the real client IP when front-ends set X-Forwarded-For. */
    private static final class PortalIp {
        static String clientIp(HttpServletRequest req) {
            String fwd = req.getHeader("X-Forwarded-For");
            if (fwd != null && !fwd.isEmpty()) {
                int comma = fwd.indexOf(',');
                return (comma > 0 ? fwd.substring(0, comma) : fwd).trim();
            }
            String real = req.getHeader("X-Real-IP");
            if (real != null && !real.isEmpty()) return real.trim();
            return req.getRemoteAddr();
        }
    }
}
