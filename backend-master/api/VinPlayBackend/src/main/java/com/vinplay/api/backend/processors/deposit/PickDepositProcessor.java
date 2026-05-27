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
import java.util.Map;

public class PickDepositProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        long txId = -1;
        String adminNickname = null;
        DepositLockService lockService = new DepositLockServiceImpl();
        boolean hazelcastLockAcquired = false;

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

            // Try Hazelcast lock first (fast rejection)
            hazelcastLockAcquired = lockService.tryLock(txId, adminNickname, "CMS");
            if (!hazelcastLockAcquired) {
                // Already locked by someone
                Map<String, Object> lockInfo = lockService.getLockInfo(txId);
                response.put("success", false);
                response.put("errorCode", "5002");
                if (lockInfo != null) {
                    response.put("locked_by", lockInfo.get("operator"));
                    response.put("lock_platform", lockInfo.get("platform"));
                }
                return response.toString();
            }

            // Atomic DB pick
            DepositTransactionDao txDao = new DepositTransactionDaoImpl();
            boolean dbSuccess = txDao.pickTransaction(txId, adminNickname, "CMS");

            if (!dbSuccess) {
                // DB pick failed - release Hazelcast lock
                lockService.release(txId, adminNickname);
                hazelcastLockAcquired = false;
                response.put("success", false);
                response.put("errorCode", "5001");
                response.put("message", "ALREADY_PICKED");
                return response.toString();
            }

            // Audit log
            try {
                Map<String, Object> tx = txDao.getTransaction(txId);
                String txCode = tx != null ? (String) tx.get("tx_code") : "";
                txDao.insertAuditLog(txId, txCode, "PICKED", adminNickname, adminNickname, "CMS",
                        "Transaction picked by " + adminNickname);
            } catch (Exception auditErr) {
                logger.error("PickDepositProcessor audit log error txId=" + txId, auditErr);
            }

            // Return success with transaction details
            Map<String, Object> tx = txDao.getTransaction(txId);
            JSONObject data = new JSONObject();
            if (tx != null) {
                data.put("id", tx.get("id"));
                data.put("tx_code", tx.get("tx_code") != null ? tx.get("tx_code") : JSONObject.NULL);
                data.put("nick_name", tx.get("nick_name") != null ? tx.get("nick_name") : JSONObject.NULL);
                data.put("amount", tx.get("amount") != null ? tx.get("amount") : 0);
                data.put("status", tx.get("status") != null ? tx.get("status") : JSONObject.NULL);
                data.put("locked_by", adminNickname);
                data.put("lock_platform", "CMS");
            }

            response.put("success", true);
            response.put("data", data);

            // Sync to Telegram — update message to show "being processed"
            try {
                if (tx != null) {
                    Number msgIdNum = (Number) tx.get("telegram_msg_id");
                    long msgId = msgIdNum != null ? msgIdNum.longValue() : 0;
                    String txCode = (String) tx.get("tx_code");
                    String nickName = (String) tx.get("nick_name");
                    long amount = ((Number) tx.get("amount")).longValue();
                    com.vinplay.dal.deposit.TelegramDepositNotifier.getInstance()
                            .editMessagePicked(msgId, txId, txCode, nickName, amount, adminNickname);
                }
            } catch (Exception tgErr) {
                logger.warn("PickDepositProcessor Telegram sync failed txId=" + txId, tgErr);
            }
        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("errorCode", "4001");
            response.put("message", "Invalid tx_id");
        } catch (Exception e) {
            logger.error("PickDepositProcessor error txId=" + txId, e);
            // Release Hazelcast lock on error
            if (hazelcastLockAcquired && txId > 0 && adminNickname != null) {
                try {
                    lockService.release(txId, adminNickname);
                } catch (Exception releaseErr) {
                    logger.error("PickDepositProcessor failed to release lock txId=" + txId, releaseErr);
                }
            }
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}
