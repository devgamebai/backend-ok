package com.vinplay.api.processors.deposit;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.dao.DepositTransactionDao;
import com.vinplay.dal.dao.impl.DepositTransactionDaoImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public class DepositHistoryProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String accessToken = request.getParameter("at");

            if (accessToken == null || accessToken.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            // Resolve nickname from token
            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = instance.getMap("cacheToken");
            if (!tokenMap.containsKey(accessToken)) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }
            String nickname = (String) tokenMap.get(accessToken);
            IMap<String, UserCacheModel> userMap = instance.getMap("users");
            UserCacheModel userCache = userMap.get(nickname);
            long userId = -1;
            if (userCache != null) {
                userId = userCache.getId();
            } else {
                try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname");
                     java.sql.PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE nick_name = ?")) {
                    ps.setString(1, nickname);
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) userId = rs.getLong("id");
                    }
                }
            }
            if (userId <= 0) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            // Parse pagination params
            int page = 1;
            int limit = 20;
            try {
                String pageStr = request.getParameter("page");
                if (pageStr != null && !pageStr.isEmpty()) {
                    page = Integer.parseInt(pageStr);
                    if (page < 1) page = 1;
                }
            } catch (NumberFormatException e) {
                page = 1;
            }
            try {
                String limitStr = request.getParameter("limit");
                if (limitStr != null && !limitStr.isEmpty()) {
                    limit = Integer.parseInt(limitStr);
                    if (limit < 1) limit = 1;
                    if (limit > 100) limit = 100;
                }
            } catch (NumberFormatException e) {
                limit = 20;
            }

            // Get user transactions
            DepositTransactionDao depositDao = new DepositTransactionDaoImpl();
            List<Map<String, Object>> transactions = depositDao.getUserTransactions(userId, page, limit);

            JSONArray dataArray = new JSONArray();
            for (Map<String, Object> tx : transactions) {
                JSONObject txObj = new JSONObject();
                txObj.put("tx_code", tx.get("tx_code") != null ? tx.get("tx_code") : JSONObject.NULL);
                txObj.put("amount", tx.get("amount"));
                txObj.put("deposit_type", tx.get("deposit_type") != null ? tx.get("deposit_type") : "");
                txObj.put("status", tx.get("status") != null ? tx.get("status") : JSONObject.NULL);
                txObj.put("created_at", tx.get("created_at") != null ? tx.get("created_at").toString() : JSONObject.NULL);
                txObj.put("processed_at", tx.get("processed_at") != null ? tx.get("processed_at").toString() : JSONObject.NULL);
                // SUN-601: include company bank info (system bank shown to user when deposit was created)
                txObj.put("user_bank_name", tx.get("user_bank_name") != null ? tx.get("user_bank_name") : JSONObject.NULL);
                txObj.put("user_bank_number", tx.get("user_bank_number") != null ? tx.get("user_bank_number") : JSONObject.NULL);
                txObj.put("user_bank_code", tx.get("user_bank_code") != null ? tx.get("user_bank_code") : JSONObject.NULL);
                // Resolve company bank from company_bank_id
                if (tx.get("company_bank_id") != null) {
                    int cbId = ((Number) tx.get("company_bank_id")).intValue();
                    if (cbId > 0) {
                        txObj.put("company_bank_id", cbId);
                        txObj.put("company_bank_name", tx.get("cb_bank_name") != null ? tx.get("cb_bank_name") : JSONObject.NULL);
                        txObj.put("company_bank_number", tx.get("cb_bank_number") != null ? tx.get("cb_bank_number") : JSONObject.NULL);
                        txObj.put("company_account_holder", tx.get("cb_account_holder") != null ? tx.get("cb_account_holder") : JSONObject.NULL);
                    }
                }
                dataArray.put(txObj);
            }

            response.put("success", true);
            response.put("data", dataArray);
        } catch (Exception e) {
            logger.error("DepositHistoryProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}
