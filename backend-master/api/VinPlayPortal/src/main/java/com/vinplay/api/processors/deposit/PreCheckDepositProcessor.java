package com.vinplay.api.processors.deposit;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.dao.DepositTransactionDao;
import com.vinplay.dal.dao.UserBankDao;
import com.vinplay.dal.dao.impl.DepositTransactionDaoImpl;
import com.vinplay.dal.dao.impl.UserBankDaoImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

public class PreCheckDepositProcessor implements BaseProcessor<HttpServletRequest, String> {
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

            // Check user has bank info
            UserBankDao userBankDao = new UserBankDaoImpl();
            boolean hasBank = userBankDao.userHasBank(userId);

            // Check pending deposit count
            DepositTransactionDao depositDao = new DepositTransactionDaoImpl();
            int pendingCount = depositDao.countUserPendingTransactions(userId);

            // Determine can_deposit and reason
            boolean canDeposit = true;
            String reason = null;

            if (!hasBank) {
                canDeposit = false;
                reason = "NO_BANK";
            } else if (pendingCount >= 2) {
                canDeposit = false;
                reason = "MAX_PENDING";
            }

            // Return deposit config with detailed flags
            JSONObject data = new JSONObject();
            data.put("can_deposit", canDeposit);
            data.put("has_bank", hasBank);
            data.put("pending_count", pendingCount);
            data.put("min_deposit", 10000);
            data.put("max_deposit", 10000000);
            if (reason != null) {
                data.put("reason", reason);
            }

            response.put("success", true);
            response.put("data", data);
        } catch (Exception e) {
            logger.error("PreCheckDepositProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

