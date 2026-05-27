package com.vinplay.api.backend.processors.deposit;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.dao.DepositTransactionDao;
import com.vinplay.dal.dao.impl.DepositTransactionDaoImpl;
import com.vinplay.dal.service.DepositLockService;
import com.vinplay.dal.service.impl.DepositLockServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

public class ReleaseDepositLockProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        long txId = -1;
        String adminNickname = null;

        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String accessToken = request.getParameter("at");
            if (accessToken == null || accessToken.isEmpty()) accessToken = request.getParameter("aat");

            // Validate admin token
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
            adminNickname = tokenMap.get(accessToken);

            // Parse tx_id
            String txIdStr = request.getParameter("tx_id");
            if (txIdStr == null || txIdStr.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "tx_id is required");
                return response.toString();
            }
            txId = Long.parseLong(txIdStr);

            DepositLockService lockService = new DepositLockServiceImpl();
            DepositTransactionDao txDao = new DepositTransactionDaoImpl();

            // Get tx_code for audit log
            String txCode = "";
            try {
                java.util.Map<String, Object> tx = txDao.getTransaction(txId);
                if (tx != null) {
                    txCode = (String) tx.get("tx_code");
                }
            } catch (Exception e) {
                logger.error("ReleaseDepositLockProcessor get tx error txId=" + txId, e);
            }

            // Release Hazelcast lock (force release since admin is explicitly releasing)
            lockService.forceRelease(txId);

            // Unlock in DB: set status back to PENDING, clear lock fields
            txDao.unlockTransaction(txId);

            // Audit log
            try {
                txDao.insertAuditLog(txId, txCode != null ? txCode : "", "RELEASED",
                        adminNickname, adminNickname, "CMS",
                        "Lock released by " + adminNickname);
            } catch (Exception auditErr) {
                logger.error("ReleaseDepositLockProcessor audit log error txId=" + txId, auditErr);
            }

            response.put("success", true);
        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("errorCode", "4001");
            response.put("message", "Invalid tx_id");
        } catch (Exception e) {
            logger.error("ReleaseDepositLockProcessor error txId=" + txId, e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}
