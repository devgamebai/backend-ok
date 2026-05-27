package com.vinplay.api.backend.processors.user;

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
 * c=9516 — List/Add/Remove live users (users displayed on live stream).
 * Stored in game_config WHERE name='live_users' as JSON array of nicknames.
 *
 * Params:
 *   (none) = list live users
 *   action=add&nn={nickname} = add user to live list
 *   action=remove&nn={nickname} = remove user from live list
 *
 * Response: {"success":true,"listUser":["nick1","nick2",...]}
 */
public class ListLiveUserProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");
    private static final String CONFIG_NAME = "live_users";

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String action = request.getParameter("action");
            String nickname = request.getParameter("nn");

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {

                // Get current live users list
                JSONArray listUser = getLiveUsers(conn);

                if ("add".equals(action) && nickname != null && !nickname.isEmpty()) {
                    // Verify user exists
                    if (!userExists(conn, nickname)) {
                        response.put("success", false);
                        response.put("errorCode", "1002");
                        response.put("message", "User not found");
                        return response.toString();
                    }
                    // Add if not already in list
                    boolean found = false;
                    for (int i = 0; i < listUser.length(); i++) {
                        if (nickname.equals(listUser.getString(i))) { found = true; break; }
                    }
                    if (!found) listUser.put(nickname);
                    saveLiveUsers(conn, listUser);

                } else if ("remove".equals(action) && nickname != null && !nickname.isEmpty()) {
                    JSONArray newList = new JSONArray();
                    for (int i = 0; i < listUser.length(); i++) {
                        if (!nickname.equals(listUser.getString(i))) newList.put(listUser.getString(i));
                    }
                    listUser = newList;
                    saveLiveUsers(conn, listUser);
                }

                response.put("success", true);
                response.put("listUser", listUser);
            }

        } catch (Exception e) {
            logger.error("ListLiveUserProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
            response.put("message", "Internal error");
        }
        return response.toString();
    }

    private JSONArray getLiveUsers(Connection conn) throws Exception {
        String sql = "SELECT value FROM game_config WHERE name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, CONFIG_NAME);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String val = rs.getString("value");
                    if (val != null && !val.isEmpty()) {
                        return new JSONArray(val);
                    }
                }
            }
        }
        return new JSONArray();
    }

    private void saveLiveUsers(Connection conn, JSONArray list) throws Exception {
        // Try update first, then insert if no rows affected
        String updateSql = "UPDATE game_config SET value = ? WHERE name = ?";
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, list.toString());
            ps.setString(2, CONFIG_NAME);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                String insertSql = "INSERT INTO game_config (name, value, platform) VALUES (?, ?, 'all')";
                try (PreparedStatement ips = conn.prepareStatement(insertSql)) {
                    ips.setString(1, CONFIG_NAME);
                    ips.setString(2, list.toString());
                    ips.executeUpdate();
                }
            }
        }
    }

    private boolean userExists(Connection conn, String nickname) throws Exception {
        String sql = "SELECT COUNT(*) FROM users WHERE nick_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nickname);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }
}
