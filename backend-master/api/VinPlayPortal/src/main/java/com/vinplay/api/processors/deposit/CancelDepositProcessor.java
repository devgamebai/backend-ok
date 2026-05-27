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
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class CancelDepositProcessor implements BaseProcessor<HttpServletRequest, String> {
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

            // Validate tx_id parameter (spec: c=3012&tx_id={id})
            // Also accept tx_code for backward compatibility
            DepositTransactionDao depositDao = new DepositTransactionDaoImpl();
            Map<String, Object> tx = null;
            String txCode = null;

            String txIdStr = request.getParameter("tx_id");
            String txCodeParam = request.getParameter("tx_code");

            if (txIdStr != null && !txIdStr.isEmpty()) {
                // Primary: lookup by tx_id (per spec)
                try {
                    long txIdParam = Long.parseLong(txIdStr);
                    tx = depositDao.getTransaction(txIdParam);
                } catch (NumberFormatException e) {
                    // invalid tx_id
                }
            } else if (txCodeParam != null && !txCodeParam.isEmpty()) {
                // Fallback: lookup by tx_code (backward compatible)
                tx = depositDao.getTransactionByCode(txCodeParam);
            }

            if (tx == null) {
                response.put("success", false);
                response.put("errorCode", "4020");
                return response.toString();
            }

            // Verify transaction belongs to this user
            long txUserId = ((Number) tx.get("user_id")).longValue();
            if (txUserId != userId) {
                response.put("success", false);
                response.put("errorCode", "4020");
                return response.toString();
            }

            long txId = ((Number) tx.get("id")).longValue();
            txCode = (String) tx.get("tx_code");


            // Atomic cancel: UPDATE WHERE status='PENDING' AND user_id=?
            boolean cancelled = depositDao.cancelTransaction(txId, userId);
            if (!cancelled) {
                response.put("success", false);
                response.put("errorCode", "4020");
                return response.toString();
            }

            // Insert audit log
            try {
                depositDao.insertAuditLog(txId, txCode, "CANCELLED", null, nickname, "USER", null);
            } catch (Exception e) {
                logger.warn("CancelDepositProcessor failed to insert audit log txId=" + txId, e);
            }

            response.put("success", true);
        } catch (Exception e) {
            logger.error("CancelDepositProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}
