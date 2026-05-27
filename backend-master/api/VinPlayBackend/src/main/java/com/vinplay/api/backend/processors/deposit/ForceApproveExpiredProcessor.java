package com.vinplay.api.backend.processors.deposit;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.dao.DepositTransactionDao;
import com.vinplay.dal.dao.impl.DepositTransactionDaoImpl;
import com.vinplay.dal.service.DepositLockService;
import com.vinplay.dal.service.impl.DepositLockServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.response.BaseResponseModel;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class ForceApproveExpiredProcessor implements BaseProcessor<HttpServletRequest, String> {

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

            // Get transaction details
            DepositTransactionDao txDao = new DepositTransactionDaoImpl();
            Map<String, Object> tx = txDao.getTransaction(txId);
            if (tx == null) {
                response.put("success", false);
                response.put("errorCode", "5010");
                response.put("message", "Transaction not found");
                return response.toString();
            }

            long amount = ((Number) tx.get("amount")).longValue();
            long userId = ((Number) tx.get("user_id")).longValue();
            String nickName = (String) tx.get("nick_name");
            String txCode = (String) tx.get("tx_code");
            String currentStatus = (String) tx.get("status");

            // --- Validation: detailed error messages per spec ---

            // Idempotency check: already force approved?
            Number forceApprovedFlag = (Number) tx.get("force_approved");
            if (forceApprovedFlag != null && forceApprovedFlag.intValue() == 1) {
                response.put("success", false);
                response.put("errorCode", "5011");
                response.put("message", "Transaction already processed");
                return response.toString();
            }

            // Already approved via normal flow? (duplicate confirmation)
            if ("APPROVED".equals(currentStatus)) {
                response.put("success", false);
                response.put("errorCode", "5012");
                response.put("message", "Duplicate confirmation");
                return response.toString();
            }

            // Only EXPIRED or REJECTED can be force approved
            if (!"EXPIRED".equals(currentStatus) && !"REJECTED".equals(currentStatus)) {
                response.put("success", false);
                response.put("errorCode", "5013");
                response.put("message", "Only expired transactions can be force approved");
                return response.toString();
            }

            // --- Atomic DB: force approve expired/rejected transaction ---
            boolean dbSuccess = txDao.forceApproveTransaction(txId, adminNickname, amount);
            if (!dbSuccess) {
                response.put("success", false);
                response.put("errorCode", "5010");
                response.put("message", "CANNOT_APPROVE - concurrent modification detected");
                return response.toString();
            }

            // Credit balance via MoneyGateway (handles promo + push + audit)
            try {
                long gwUserId = 0;
                try (java.sql.Connection uc = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname");
                     java.sql.PreparedStatement ups = uc.prepareStatement("SELECT id FROM users WHERE nick_name = ?")) {
                    ups.setString(1, nickName);
                    try (java.sql.ResultSet urs = ups.executeQuery()) { if (urs.next()) gwUserId = urs.getLong("id"); }
                }
                com.vinplay.dal.service.MoneyGateway.CreditResult cr =
                    com.vinplay.dal.service.MoneyGateway.creditUser(
                        gwUserId, nickName, amount, "DEPOSIT_BANK",
                        String.valueOf(txId), "Force-approved expired deposit");

                if (cr.success) {
                    txDao.updateCreditStatus(txId, "CREDITED");
                } else {
                    logger.error("ForceApproveExpiredProcessor credit failed txId=" + txId +
                            " nickName=" + nickName + " amount=" + amount +
                            " error=" + cr.error);
                    txDao.updateCreditStatus(txId, "CREDIT_FAILED");
                }
            } catch (Exception creditErr) {
                logger.error("ForceApproveExpiredProcessor credit exception txId=" + txId, creditErr);
                try {
                    txDao.updateCreditStatus(txId, "CREDIT_FAILED");
                } catch (Exception updateErr) {
                    logger.error("ForceApproveExpiredProcessor updateCreditStatus failed txId=" + txId, updateErr);
                }
            }

            // Release any Hazelcast lock that might exist
            try {
                DepositLockService lockService = new DepositLockServiceImpl();
                lockService.forceRelease(txId);
            } catch (Exception releaseErr) {
                logger.error("ForceApproveExpiredProcessor failed to release lock txId=" + txId, releaseErr);
            }

            // Audit log
            try {
                txDao.insertAuditLog(txId, txCode != null ? txCode : "", "FORCE_APPROVED",
                        adminNickname, adminNickname, "CMS",
                        "Force approved by " + adminNickname + ", amount=" + amount
                                + ", previous_status=" + currentStatus);
            } catch (Exception auditErr) {
                logger.error("ForceApproveExpiredProcessor audit log error txId=" + txId, auditErr);
            }

            response.put("success", true);
            response.put("force_approved", true);

            // Sync to Telegram — update message to show force approved status
            // with bank info preserved (User request 2026-04-22).
            try {
                Number msgIdNum = (Number) tx.get("telegram_msg_id");
                long msgId = msgIdNum != null ? msgIdNum.longValue() : 0;
                if (msgId > 0) {
                    String bankName = (String) tx.get("user_bank_name");
                    String bankNumber = (String) tx.get("user_bank_number");
                    // Holder lives on vinplay.users_bank (see ApproveDepositProcessor).
                    String holderName = null;
                    try (java.sql.Connection bConn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname");
                         java.sql.PreparedStatement bPs = bConn.prepareStatement(
                                 "SELECT customer_name FROM vinplay.users_bank "
                                 + "WHERE user_id = ? AND bank_number = ? LIMIT 1")) {
                        bPs.setLong(1, userId);
                        bPs.setString(2, bankNumber != null ? bankNumber : "");
                        try (java.sql.ResultSet bRs = bPs.executeQuery()) {
                            if (bRs.next()) holderName = bRs.getString(1);
                        }
                    } catch (Exception holderErr) {
                        logger.warn("ForceApproveExpiredProcessor holder lookup failed txId=" + txId, holderErr);
                    }
                    com.vinplay.dal.deposit.TelegramDepositNotifier.getInstance()
                            .editMessageForceApproved(msgId, txCode, amount, nickName, adminNickname,
                                    bankName, bankNumber, holderName);
                }
            } catch (Exception tgErr) {
                logger.warn("ForceApproveExpiredProcessor Telegram sync failed txId=" + txId, tgErr);
            }

        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("errorCode", "4001");
            response.put("message", "Invalid tx_id");
        } catch (Exception e) {
            logger.error("ForceApproveExpiredProcessor error txId=" + txId, e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}
