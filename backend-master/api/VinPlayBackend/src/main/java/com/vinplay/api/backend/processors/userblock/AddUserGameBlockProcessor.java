package com.vinplay.api.backend.processors.userblock;

import com.hazelcast.core.IMap;
import com.vinplay.dal.service.UserGameBlock;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

/**
 * c=9985 — Add a per-user block. Examples:
 *
 *   block all GSC for user 42:
 *     ?user_id=42&provider=GSC
 *   block user "abc" on Evo (GSC product 1002):
 *     ?nick_name=abc&provider=GSC&vendor_platform=1002
 *   block user 42 on every baccarat (across providers):
 *     ?user_id=42&category_id=1
 *   block user 42 on Sexy Baccarat M01 only:
 *     ?user_id=42&provider=AWC&vendor_platform=SEXYBCRT&game_code=MX-LIVE-001&table_tag=M01
 *
 * <p>Params:
 * <ul>
 *   <li>{@code aat} — admin token</li>
 *   <li>{@code user_id} OR {@code nick_name} — at least one</li>
 *   <li>{@code provider}, {@code vendor_platform}, {@code game_code},
 *       {@code table_tag}, {@code category_id} — any subset; missing
 *       means "any". Empty string also means "any".</li>
 *   <li>{@code reason} — optional free-text audit note</li>
 *   <li>{@code expires_at} — ISO-8601 datetime; omit for permanent</li>
 * </ul>
 */
public class AddUserGameBlockProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();

            String aat = request.getParameter("aat");
            if (aat == null || aat.isEmpty()) return err(response, "1001", "aat required");
            IMap<String, String> tokenMap = HazelcastClientFactory.getInstance().getMap("cacheToken");
            String adminNick = tokenMap.get(aat);
            if (adminNick == null || adminNick.isEmpty()) return err(response, "1001", "Unauthorized");

            String userIdStr = request.getParameter("user_id");
            String nickName = blankToNull(request.getParameter("nick_name"));
            Long userId = null;
            if (userIdStr != null && !userIdStr.isEmpty()) {
                try { userId = Long.parseLong(userIdStr); }
                catch (NumberFormatException e) { return err(response, "4002", "user_id must be numeric"); }
            }
            if ((userId == null || userId <= 0) && nickName == null) {
                return err(response, "4001", "user_id or nick_name required");
            }

            String provider       = blankToNull(request.getParameter("provider"));
            if (provider != null) provider = provider.toUpperCase();
            String vendorPlatform = blankToNull(request.getParameter("vendor_platform"));
            String gameCode       = blankToNull(request.getParameter("game_code"));
            String tableTag       = blankToNull(request.getParameter("table_tag"));
            Integer categoryId    = parseIntOrNull(request.getParameter("category_id"));
            String reason         = blankToNull(request.getParameter("reason"));
            String expiresAtStr   = blankToNull(request.getParameter("expires_at"));

            Timestamp expiresAt = null;
            if (expiresAtStr != null) {
                try {
                    String s = expiresAtStr.replace('T', ' ');
                    if (s.length() == 10) s = s + " 00:00:00";
                    expiresAt = Timestamp.valueOf(s.substring(0, Math.min(s.length(), 19)));
                } catch (Exception e) {
                    return err(response, "4002", "expires_at must be 'YYYY-MM-DD HH:MM:SS' or ISO-8601");
                }
            }

            // Resolve missing user_id ↔ nick_name when only one provided —
            // makes downstream lookups O(1) by user_id and the audit row
            // self-describing.
            if (userId == null && nickName != null) {
                Long resolved = lookupUserId(nickName);
                if (resolved != null) userId = resolved;
            }
            if (nickName == null && userId != null) {
                String resolved = lookupNickname(userId);
                if (resolved != null) nickName = resolved;
            }

            long id = UserGameBlock.add(userId, nickName, provider, vendorPlatform,
                    gameCode, tableTag, categoryId, reason, adminNick, expiresAt);
            if (id < 0) return err(response, "9999", "Insert failed");

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO log_admin (action, username, reason, status, account_name) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, "usergameblock.add");
                ps.setString(2, adminNick);
                ps.setString(3, "user_id=" + userId + " nick=" + nickName + " provider=" + provider
                        + " vp=" + vendorPlatform + " game=" + gameCode + " tag=" + tableTag
                        + " cat=" + categoryId + " reason=" + reason);
                ps.setString(4, "blocked_id=" + id);
                ps.setString(5, nickName != null ? nickName : String.valueOf(userId));
                ps.executeUpdate();
            } catch (Exception auditErr) {
                logger.warn("AddUserGameBlock audit insert failed: " + auditErr.getMessage());
            }

            JSONObject data = new JSONObject();
            data.put("id", id);
            data.put("user_id", userId);
            data.put("nick_name", nickName);
            data.put("provider", provider);
            data.put("vendor_platform", vendorPlatform);
            data.put("game_code", gameCode);
            data.put("table_tag", tableTag);
            data.put("category_id", categoryId);
            data.put("expires_at", expiresAtStr);
            data.put("note", "Block takes effect within 60s (UserGameBlock cache TTL).");
            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", data);
        } catch (Exception e) {
            logger.error("AddUserGameBlockProcessor error", e);
            return err(response, "9999", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    private static Long lookupUserId(String nickName) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE nick_name = ? LIMIT 1")) {
            ps.setString(1, nickName);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (Exception ignore) {}
        return null;
    }

    private static String lookupNickname(long userId) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement("SELECT nick_name FROM users WHERE id = ? LIMIT 1")) {
            ps.setLong(1, userId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (Exception ignore) {}
        return null;
    }

    private static String blankToNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }

    private static Integer parseIntOrNull(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false);
        r.put("errorCode", code);
        r.put("message", msg);
        return r.toString();
    }
}
