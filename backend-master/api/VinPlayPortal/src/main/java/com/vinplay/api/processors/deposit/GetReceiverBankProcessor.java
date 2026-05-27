package com.vinplay.api.processors.deposit;

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
 * c=3014: Returns active admin bank accounts (platform receiving accounts).
 * Players see this info to know where to transfer money.
 * Only returns accounts with status=1 (active).
 */
public class GetReceiverBankProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String accessToken = request.getParameter("at");

            // Validate token
            if (accessToken == null || accessToken.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = instance.getMap("cacheToken");
            if (!tokenMap.containsKey(accessToken)) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            // Query active admin bank accounts. Resolve the canonical Korean
            // bank_name via the bank_id FK (2026-05-12 admin_banks migration)
            // so the player sees "씨티은행" instead of the stored short code
            // "CITI".
            // 2026-05-13: bank_name combined as "Korean (CODE)" for the
            // player's receiver-bank picker.
            String sql = "SELECT ab.id, ab.customer_name, " +
                    "CASE WHEN b.bank_name IS NOT NULL AND b.code IS NOT NULL AND b.code <> '' " +
                    "     THEN CONCAT(b.bank_name, ' (', b.code, ')') " +
                    "     WHEN b.bank_name IS NOT NULL THEN b.bank_name " +
                    "     ELSE ab.bank_name END AS bank_name, " +
                    "b.code, ab.bank_number, ab.branch " +
                    "FROM admin_banks ab " +
                    "LEFT JOIN banks b ON b.id = ab.bank_id " +
                    "WHERE ab.status = 1 ORDER BY ab.id ASC";

            JSONArray banks = new JSONArray();
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement stm = conn.prepareStatement(sql);
                 ResultSet rs = stm.executeQuery()) {

                while (rs.next()) {
                    JSONObject bank = new JSONObject();
                    bank.put("id", rs.getInt("id"));
                    bank.put("customer_name", rs.getString("customer_name"));
                    bank.put("bank_name", rs.getString("bank_name"));
                    bank.put("bank_code", rs.getString("code"));
                    bank.put("bank_number", rs.getString("bank_number"));
                    String branch = rs.getString("branch");
                    bank.put("branch", branch != null ? branch : JSONObject.NULL);
                    banks.put(bank);
                }
            }

            response.put("success", true);
            response.put("data", banks);
        } catch (Exception e) {
            logger.error("GetReceiverBankProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}
