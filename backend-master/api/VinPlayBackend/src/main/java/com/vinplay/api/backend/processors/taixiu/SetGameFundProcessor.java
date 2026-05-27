package com.vinplay.api.backend.processors.taixiu;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Set game fund/quỹ value (c=8801).
 * Params: fund_name (e.g. TaiXiu_Fund_vin, SICBO_FUND_VIN), value (long), aat (admin token)
 *
 * Updates vinplay_minigame.minigame_funds table.
 * Note: This updates DB only. The game server loads fund from DB at startup and
 * persists back on each round. Changes take effect on next round finish when
 * the game server reloads, or on restart.
 */
public class SetGameFundProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            // Validate admin token
            String aat = request.getParameter("aat");
            if (aat == null || aat.isEmpty()) aat = request.getParameter("at");
            if (aat == null || aat.isEmpty()) {
                return err(response, "1001", "Missing admin token");
            }
            HazelcastInstance hz = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = hz.getMap("cacheToken");
            String adminNick = tokenMap.get(aat);
            if (adminNick == null) {
                return err(response, "1001", "Invalid admin token");
            }

            // Parse params - support query params and JSON body
            String fundName = request.getParameter("fund_name");
            String valueStr = request.getParameter("value");

            if (fundName == null || valueStr == null) {
                try {
                    StringBuilder sb = new StringBuilder();
                    BufferedReader reader = request.getReader();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    String body = sb.toString().trim();
                    if (!body.isEmpty()) {
                        JSONObject bodyJson = new JSONObject(body);
                        if (fundName == null && bodyJson.has("fund_name")) fundName = bodyJson.getString("fund_name");
                        if (valueStr == null && bodyJson.has("value")) valueStr = String.valueOf(bodyJson.getLong("value"));
                    }
                } catch (Exception e) {
                    logger.warn("SetGameFundProcessor: failed to parse JSON body", e);
                }
            }

            if (fundName == null || fundName.isEmpty()) {
                return err(response, "4001", "Missing fund_name (e.g. TaiXiu_Fund_vin, SICBO_FUND_VIN)");
            }
            if (valueStr == null || valueStr.isEmpty()) {
                return err(response, "4001", "Missing value");
            }

            long value;
            try {
                value = Long.parseLong(valueStr);
            } catch (NumberFormatException e) {
                return err(response, "4002", "value must be a number");
            }

            // Verify fund_name exists
            long oldValue;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame")) {
                PreparedStatement check = conn.prepareStatement(
                        "SELECT value FROM minigame_funds WHERE fund_name = ?");
                check.setString(1, fundName);
                ResultSet rs = check.executeQuery();
                if (!rs.next()) {
                    rs.close();
                    check.close();
                    return err(response, "1002", "Fund not found: " + fundName);
                }
                oldValue = rs.getLong("value");
                rs.close();
                check.close();

                // Update
                PreparedStatement update = conn.prepareStatement(
                        "UPDATE minigame_funds SET value = ? WHERE fund_name = ?");
                update.setLong(1, value);
                update.setString(2, fundName);
                update.executeUpdate();
                update.close();
            }

            logger.info("SetGameFund: admin=" + adminNick + " fund=" + fundName
                    + " old=" + oldValue + " new=" + value);

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("fund_name", fundName);
            response.put("old_value", oldValue);
            response.put("new_value", value);

        } catch (Exception e) {
            logger.error("SetGameFundProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    private String err(JSONObject r, String code, String msg) {
        r.put("success", false);
        r.put("errorCode", code);
        r.put("message", msg);
        return r.toString();
    }
}
