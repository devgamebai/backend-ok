package com.vinplay.api.backend.processors.banca;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * SUN-1062 / Wallet Phase 5c follow-up — BanCa unified-wallet balance read bridge.
 *
 * <p>Companion to {@link BanCaSettleProcessor} (c=9998). Phase 5b/5c migrated
 * every BanCa write path to {@code MoneyGateway}, so the legacy Redis key
 * {@code User_Cash:{cgame_user_id}} is no longer refreshed on write. Anything
 * that still reads via {@code RedisManager.GetUserCash} would see a stale
 * mirror. This endpoint lets the C# side query the canonical PLAYER_VIN
 * balance directly when {@code BANCA_USE_UNIFIED_WALLET=1}.
 *
 * <p><b>Command ID:</b> 9997. Lives on {@code backend-api}, same surface as
 * c=9998. Service-token guarded; not part of the admin token path.
 *
 * <p><b>Auth:</b> {@code X-Service-Token} header must match env
 * {@code BANCA_SERVICE_TOKEN}. Fail closed when env is missing.
 *
 * <p><b>Input (GET query params):</b>
 * <ul>
 *   <li>{@code tok} — alternate token if header path is awkward (header still preferred)</li>
 *   <li>{@code user_id} — vinplay {@code users.id} (preferred)</li>
 *   <li>{@code cgame_user_id} — cgame.users.user_id; only consulted when {@code user_id} absent</li>
 * </ul>
 *
 * <p>If both are absent the call fails 4001. If {@code cgame_user_id} is
 * given we resolve the player by joining {@code cgame.users.nickname} to
 * {@code vinplay.users.nick_name} (cgame and vinplay share the cgame_user_id
 * for migrated accounts, so the direct lookup is best-effort first).
 *
 * <p><b>Output (success):</b>
 * <pre>
 * { "success": true, "vin_balance": 1500000000 }     // milli-VND for parity with settle
 * </pre>
 *
 * <p>{@code vin_balance} is in milli-VND (VND × 1000) so the C# caller can
 * substitute it directly for the legacy {@code User_Cash:{id}} value, which
 * was also stored in milli-VND.
 *
 * <p>Errors:
 * <table border="1">
 *   <tr><td>1001</td><td>service token missing / mismatch</td></tr>
 *   <tr><td>4001</td><td>neither user_id nor cgame_user_id provided</td></tr>
 *   <tr><td>1002</td><td>user not found</td></tr>
 *   <tr><td>9999</td><td>internal — see logs</td></tr>
 * </table>
 */
public class BanCaBalanceQueryProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        response.put("success", false);

        try {
            HttpServletRequest request = param.get();

            // --- Auth (X-Service-Token header, with `tok` query-param fallback) ---
            String expectedToken = System.getenv("BANCA_SERVICE_TOKEN");
            if (expectedToken == null || expectedToken.isEmpty()) {
                return err(response, "1001", "service token not configured on server");
            }
            String providedToken = request.getHeader("X-Service-Token");
            if (providedToken == null || providedToken.isEmpty()) {
                providedToken = request.getParameter("tok");
            }
            if (providedToken == null || !expectedToken.equals(providedToken)) {
                return err(response, "1001", "invalid service token");
            }

            // --- Resolve user_id ---
            long userId = parseLong(request.getParameter("user_id"));
            long cgameUserId = parseLong(request.getParameter("cgame_user_id"));

            if (userId <= 0 && cgameUserId <= 0) {
                return err(response, "4001", "user_id or cgame_user_id required");
            }

            // cgame.users.user_id is NOT 1:1 with vinplay.users.id (audit
            // proved laviai cgame=184 → wrong vinplay row). Always resolve
            // via nickname join; cgame.vinplay_user_id FK is also unreliable.
            if (userId <= 0) {
                userId = resolveByNickname(cgameUserId);
            }

            if (userId <= 0) {
                return err(response, "1002", "user not found cgame_user_id=" + cgameUserId);
            }

            long vinVnd = lookupBalanceVnd(userId);
            if (vinVnd < 0) {
                return err(response, "1002", "user not found id=" + userId);
            }

            // Milli-VND for parity with the legacy Redis User_Cash:{id} value
            // (BanCa hot path stores VND × 1000).
            long vinMilli = vinVnd * 1000L;

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("user_id", userId);
            response.put("vin_balance", vinMilli);
            response.put("vin_balance_vnd", vinVnd);
            return response.toString();

        } catch (Exception e) {
            logger.error("BanCaBalanceQueryProcessor unhandled error", e);
            return err(response, "9999", "internal: " + e.getMessage());
        }
    }

    private static long parseLong(String s) {
        if (s == null || s.isEmpty()) return 0L;
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return 0L; }
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false);
        r.put("errorCode", code);
        r.put("message", msg);
        return r.toString();
    }

    /**
     * Direct lookup: cgame.users.user_id is kept 1:1 with vinplay.users.id
     * for every migrated account, so a single point read is the happy path.
     */
    private static long resolveByCgameDirect(long cgameUserId) {
        if (cgameUserId <= 0) return -1L;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id FROM users WHERE id = ? LIMIT 1")) {
            ps.setLong(1, cgameUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (Exception e) {
            logger.warn("BanCaBalanceQueryProcessor.resolveByCgameDirect failed cgame_user_id=" + cgameUserId
                    + ": " + e.getMessage());
        }
        return -1L;
    }

    /**
     * Nickname-join fallback: cgame.users.nickname ↔ vinplay.users.nick_name.
     * Used only when the direct {@code id} lookup misses (legacy / pre-migration users).
     */
    private static long resolveByNickname(long cgameUserId) {
        if (cgameUserId <= 0) return -1L;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT v.id FROM users v " +
                     "INNER JOIN cgame.users c ON c.nickname = v.nick_name " +
                     "WHERE c.user_id = ? LIMIT 1")) {
            ps.setLong(1, cgameUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (Exception e) {
            logger.warn("BanCaBalanceQueryProcessor.resolveByNickname failed cgame_user_id=" + cgameUserId
                    + ": " + e.getMessage());
        }
        return -1L;
    }

    /**
     * Current PLAYER_VIN balance in VND. Returns -1 when the user does not
     * exist so the caller can surface 1002.
     */
    private static long lookupBalanceVnd(long userId) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT vin FROM users WHERE id = ? LIMIT 1")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (Exception e) {
            logger.warn("BanCaBalanceQueryProcessor.lookupBalanceVnd failed userId=" + userId
                    + ": " + e.getMessage());
        }
        return -1L;
    }
}
