package com.vinplay.api.processors.cashback;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Player API (c=3082): Get pending cashback summary for the logged-in user.
 *
 * Returns:
 *  - claimable: total pending amount the player can claim right now
 *  - count: number of pending entries
 *  - oldest_at: timestamp of the oldest pending entry (used by FE to compute
 *    the rolling 7-day expiry countdown)
 *  - expires_at: oldest_at + 7 days (when the rolling window wipes the pile)
 *  - claimed: lifetime total already claimed (sum of PAID SELF rebates)
 *  - items: latest N entries for the history table (SUN-751)
 *
 * Params: at (access token), limit (default 50, max 200)
 *
 * SUN-764 / SUN-750 / SUN-751 — player cashback claim flow.
 */
public class GetPendingCashbackProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;
    private static final long EXPIRY_WINDOW_DAYS = 7;

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String accessToken = request.getParameter("at");
            if (accessToken == null || accessToken.isEmpty()) {
                return err(response, "1001", "access token required");
            }

            // Resolve nickname from token (same pattern as other portal processors)
            HazelcastInstance hz = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = hz.getMap("cacheToken");
            if (!tokenMap.containsKey(accessToken)) {
                return err(response, "1001", "invalid session");
            }
            String nickname = tokenMap.get(accessToken);

            // Rolling 7-day expiry: wipe this user's pending pile if the oldest
            // entry has aged out of the window. Lazy, per-user — no cron needed.
            CashbackExpiryHelper.wipeIfExpired(nickname);

            int limit = DEFAULT_LIMIT;
            int page = 1;
            try {
                String l = request.getParameter("limit");
                if (l != null && !l.isEmpty()) limit = Integer.parseInt(l);
            } catch (NumberFormatException ignored) {}
            try {
                String p = request.getParameter("page");
                if (p != null && !p.isEmpty()) page = Integer.parseInt(p);
            } catch (NumberFormatException ignored) {}
            if (limit < 1) limit = 1;
            if (limit > MAX_LIMIT) limit = MAX_LIMIT;
            if (page < 1) page = 1;

            long claimable = 0;
            long count = 0;
            String oldestAt = null;
            long claimed = 0;
            long totalItems = 0;
            JSONArray items = new JSONArray();

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // 1. Summary: pending total + count + oldest timestamp.
                // SUN-1180: skip 0-amount rows — they're rebate-tracking
                // placeholders for games at 0% commission, not claimable.
                try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(rebate_amount),0) AS total, COUNT(*) AS cnt, " +
                    "MIN(created_at) AS oldest " +
                    "FROM rebate_logs WHERE agent_nickname = ? " +
                    "AND rebate_type = 'SELF' AND status = 'PENDING' AND rebate_amount > 0")) {
                    ps.setString(1, nickname);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            claimable = rs.getLong("total");
                            count = rs.getLong("cnt");
                            java.sql.Timestamp ts = rs.getTimestamp("oldest");
                            if (ts != null) oldestAt = ts.toString();
                        }
                    }
                }

                // 2. Lifetime claimed total
                try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(rebate_amount),0) AS total FROM rebate_logs " +
                    "WHERE agent_nickname = ? AND rebate_type = 'SELF' AND status = 'PAID'")) {
                    ps.setString(1, nickname);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) claimed = rs.getLong("total");
                    }
                }

                // 3. Total items count (for pagination). SUN-1180: same
                // 0-amount filter as items query so totalPages matches.
                try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) AS total FROM rebate_logs " +
                    "WHERE agent_nickname = ? AND rebate_type = 'SELF' AND rebate_amount > 0")) {
                    ps.setString(1, nickname);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) totalItems = rs.getLong("total");
                    }
                }

                // 4. Paginated history items (for SUN-751 display table).
                // SUN-1180: hide 0-amount rows from the player view. Those are
                // tracking-only entries for games at 0% commission rate; they
                // exist in rebate_logs for agent rolling history (LS Rolling)
                // but the player has nothing to claim for them.
                int offset = (page - 1) * limit;
                try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, total_f1_volume AS bet_amount, rebate_percentage, " +
                    "rebate_amount, status, note, created_at " +
                    "FROM rebate_logs WHERE agent_nickname = ? AND rebate_type = 'SELF' " +
                    "AND rebate_amount > 0 " +
                    "ORDER BY created_at DESC LIMIT ? OFFSET ?")) {
                    ps.setString(1, nickname);
                    ps.setInt(2, limit);
                    ps.setInt(3, offset);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject row = new JSONObject();
                            row.put("id", rs.getLong("id"));
                            row.put("bet_amount", rs.getLong("bet_amount"));
                            // SUN-1098: 2-decimal string preserves trailing zeros for cashback display.
                            row.put("rate", com.vinplay.dal.utils.PctFormatter.formatRs(rs, "rebate_percentage"));
                            // SUN-1150: rebate_amount is DECIMAL(20,2); preserve fractional KRW.
                            java.math.BigDecimal amt = rs.getBigDecimal("rebate_amount");
                            row.put("amount",
                                    com.vinplay.dal.utils.PctFormatter.format(
                                            amt != null ? amt : java.math.BigDecimal.ZERO));
                            row.put("status", rs.getString("status"));
                            // Replace the raw AUTO_COMMISSION note (long internal
                            // dedup string with source|action|service|money|time)
                            // with a short human-readable label for the FE table.
                            // QC 2026-04-27: "make it short and human readable".
                            row.put("note", formatDisplayNote(
                                    rs.getString("note"),
                                    rs.getLong("bet_amount")));
                            java.sql.Timestamp ts = rs.getTimestamp("created_at");
                            row.put("created_at", ts != null ? ts.toString() : null);
                            items.put(row);
                        }
                    }
                }
            }

            // Compute rolling 7-day expiry from oldest pending timestamp
            String expiresAt = null;
            if (oldestAt != null) {
                try {
                    java.sql.Timestamp t = java.sql.Timestamp.valueOf(oldestAt);
                    long expiry = t.getTime() + EXPIRY_WINDOW_DAYS * 24L * 3600L * 1000L;
                    expiresAt = new java.sql.Timestamp(expiry).toString();
                } catch (Exception ignored) {}
            }

            long totalPages = Math.max(1L, (totalItems + limit - 1) / limit);

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("claimable", claimable);
            response.put("count", count);
            response.put("claimed", claimed);
            if (oldestAt != null) response.put("oldest_at", oldestAt);
            if (expiresAt != null) response.put("expires_at", expiresAt);
            response.put("expiry_days", EXPIRY_WINDOW_DAYS);
            response.put("items", items);
            response.put("page", page);
            response.put("limit", limit);
            response.put("total", totalItems);
            response.put("totalPages", totalPages);
        } catch (Exception e) {
            logger.error("GetPendingCashbackProcessor error", e);
            return err(response, "9999", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false);
        r.put("errorCode", code);
        r.put("message", msg);
        return r.toString();
    }

    /**
     * Render a short human-readable label for the FE history table.
     *
     * Raw note stored on the row looks like:
     *   AUTO_COMMISSION source=&lt;sourceKey&gt; type=SELF
     *   user=&lt;nick&gt; action=gsc_1002_LotusSicBo000001 service=1002
     *
     * The FE only needs game + bet — type is always SELF on this endpoint and
     * user is always the logged-in player. Renders e.g.:
     *   "Hoàn cược · Lotus Sic Bo · cược 1,000"
     *
     * Falls back to a generic "Hoàn cược · cược N" if the note is empty or
     * missing the action= field (older rows / non-AUTO_COMMISSION sources).
     */
    static String formatDisplayNote(String rawNote, long betAmount) {
        String label = friendlyGameLabel(extractField(rawNote, "action="));
        StringBuilder sb = new StringBuilder("Hoàn cược");
        if (label != null && !label.isEmpty()) {
            sb.append(" · ").append(label);
        }
        sb.append(" · cược ").append(formatKrw(betAmount));
        return sb.toString();
    }

    /** Pulls a "key=value" token out of a space-separated note. */
    static String extractField(String note, String key) {
        if (note == null || key == null) return null;
        int idx = note.indexOf(key);
        if (idx < 0) return null;
        int start = idx + key.length();
        int end = start;
        while (end < note.length()) {
            char c = note.charAt(end);
            if (c == ' ' || c == '|') break;
            end++;
        }
        return end > start ? note.substring(start, end).trim() : null;
    }

    /**
     * Reduce a raw action name to a player-friendly game label.
     * "gsc_1002_LotusSicBo000001" → "Lotus Sic Bo"
     * "taixiu"                    → "Tài Xỉu"
     * Anything unrecognized falls back to the raw value (or null).
     */
    static String friendlyGameLabel(String action) {
        if (action == null || action.isEmpty()) return null;
        String stripped = action;
        // strip "gsc_NNNN_" prefix
        if (stripped.startsWith("gsc_")) {
            int second = stripped.indexOf('_', 4);
            if (second > 0 && second < stripped.length() - 1) {
                stripped = stripped.substring(second + 1);
            }
        }
        // strip trailing digits (gsc games suffix the round id e.g. ...000001)
        int end = stripped.length();
        while (end > 0 && Character.isDigit(stripped.charAt(end - 1))) end--;
        if (end > 0) stripped = stripped.substring(0, end);

        // Known short labels (extend as new games come online)
        String key = stripped.toLowerCase();
        if (key.equals("taixiu"))    return "Tài Xỉu";
        if (key.equals("xocdia"))    return "Xóc Đĩa";
        if (key.equals("baccarat"))  return "Baccarat";
        if (key.equals("dragontiger")) return "Dragon Tiger";
        if (key.equals("roulette"))  return "Roulette";
        if (key.equals("sicbo"))     return "Sic Bo";
        if (key.equals("lotussicbo")) return "Lotus Sic Bo";
        if (key.equals("slot"))      return "Slot";
        if (key.equals("caothap"))   return "Cao Thấp";
        if (key.equals("poker"))     return "Poker";
        if (key.equals("tlmn"))      return "Tiến Lên Miền Nam";
        if (key.equals("lieng"))     return "Liêng";
        if (key.equals("binh"))      return "Mậu Bình";
        if (key.equals("baicao"))    return "Bài Cào";
        if (key.equals("bacay"))     return "Ba Cây";
        if (key.equals("sam"))       return "Sâm Lốc";
        if (key.equals("xizach"))    return "Xì Zảch";

        // Insert spaces before capitals: "LotusSicBo" → "Lotus Sic Bo"
        StringBuilder pretty = new StringBuilder();
        for (int i = 0; i < stripped.length(); i++) {
            char c = stripped.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && !Character.isUpperCase(stripped.charAt(i - 1))) {
                pretty.append(' ');
            }
            pretty.append(c);
        }
        return pretty.toString();
    }

    private static String formatKrw(long n) {
        return String.format("%,d", n);
    }
}
