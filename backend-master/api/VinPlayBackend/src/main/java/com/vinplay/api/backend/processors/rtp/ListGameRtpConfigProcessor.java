package com.vinplay.api.backend.processors.rtp;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * c=9770 — List all game RTP config rows.
 * Params: aat (admin token, validated at servlet level).
 * Returns: {success, data: [{game_code, win_rate_pct, status, updated_by, updated_at, note}, ...]}
 */
public class ListGameRtpConfigProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            JSONArray arr = new JSONArray();
            String sql = "SELECT game_code, win_rate_pct, status, updated_by, " +
                    "DATE_FORMAT(updated_at,'%Y-%m-%d %H:%i:%s') AS updated_at, note " +
                    "FROM game_rtp_config ORDER BY game_code";
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject row = new JSONObject();
                    row.put("game_code", rs.getString("game_code"));
                    // SUN-1098: 2-decimal string for RTP display.
                    row.put("win_rate_pct", com.vinplay.dal.utils.PctFormatter.formatRs(rs, "win_rate_pct"));
                    row.put("status", rs.getInt("status"));
                    row.put("updated_by", rs.getString("updated_by"));
                    row.put("updated_at", rs.getString("updated_at"));
                    row.put("note", rs.getString("note") != null ? rs.getString("note") : "");
                    arr.put(row);
                }
            }
            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", arr);
            response.put("total", arr.length());
        } catch (Exception e) {
            logger.error("ListGameRtpConfigProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }
}
