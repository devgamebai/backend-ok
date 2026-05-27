package com.vinplay.api.processors.deposit;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.dao.DepositTransactionDao;
import com.vinplay.dal.dao.UserBankDao;
import com.vinplay.dal.dao.impl.DepositTransactionDaoImpl;
import com.vinplay.dal.dao.impl.UserBankDaoImpl;
import com.vinplay.dal.service.DepositLockService;
import com.vinplay.dal.service.DepositQueueService;
import com.vinplay.dal.service.impl.DepositLockServiceImpl;
import com.vinplay.dal.service.impl.DepositQueueServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CreateDepositProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("api");
    private static final String COMPANY_BANK_CACHE_KEY = "active_company_bank";
    private static final long COMPANY_BANK_CACHE_TTL_MIN = 5; // 5 minutes TTL

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
                // Fallback: lookup by nickname in DB
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

            // Validate amount parameter
            String amountStr = request.getParameter("amount");
            if (amountStr == null || amountStr.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4012");
                return response.toString();
            }

            long amount;
            try {
                amount = Long.parseLong(amountStr);
            } catch (NumberFormatException e) {
                response.put("success", false);
                response.put("errorCode", "4012");
                return response.toString();
            }

            if (amount < 10000) {
                response.put("success", false);
                response.put("errorCode", "4013");
                return response.toString();
            }

            // SUN-1140: max single-deposit lowered from 10M to 4M per QC.
            if (amount > 4000000) {
                response.put("success", false);
                response.put("errorCode", "4014");
                return response.toString();
            }

            // Amount must be a multiple of 10,000 won
            if (amount % 10000 != 0) {
                response.put("success", false);
                response.put("errorCode", "4016");
                return response.toString();
            }

            // Check user has bank info
            UserBankDao userBankDao = new UserBankDaoImpl();
            if (!userBankDao.userHasBank(userId)) {
                response.put("success", false);
                response.put("errorCode", "4010");
                return response.toString();
            }

            // Check pending deposit count < 2
            DepositTransactionDao depositDao = new DepositTransactionDaoImpl();
            int pendingCount = depositDao.countUserPendingTransactions(userId);
            if (pendingCount >= 2) {
                response.put("success", false);
                response.put("errorCode", "4011");
                return response.toString();
            }

            // Rate limit check
            DepositLockService lockService = new DepositLockServiceImpl();
            if (!lockService.checkRateLimit(userId)) {
                response.put("success", false);
                response.put("errorCode", "4015");
                return response.toString();
            }

            // Get user bank info
            Map<String, Object> bankInfo = userBankDao.getUserBank(userId);
            if (bankInfo == null) {
                response.put("success", false);
                response.put("errorCode", "4010");
                return response.toString();
            }

            String bankName = (String) bankInfo.get("bank_name");
            String bankNumber = (String) bankInfo.get("bank_number");
            String bankCode = (String) bankInfo.get("code");
            String holderName = (String) bankInfo.get("customer_name");

            // Generate idempotency key
            String requestId = request.getParameter("request_id");
            String idempotencyKey;
            if (requestId != null && !requestId.isEmpty()) {
                idempotencyKey = userId + ":" + requestId;
            } else {
                idempotencyKey = userId + ":" + amount + ":" + (System.currentTimeMillis() / 5000);
            }

            // SUN-601 fix: resolve the active company bank ONCE, reuse its id
            // both for the transaction row AND the user-facing response. Before
            // this fix, company_bank_id was hardcoded to 0 and the response
            // re-queried the active bank separately — if admin switched the
            // active company bank between steps, the user saw a different bank
            // than what admin later reconciled against. Now the deposit row is
            // locked to the exact company_bank shown to the user.
            Map<String, Object> companyBank = null;
            int companyBankId = 0;
            try {
                @SuppressWarnings({"rawtypes", "unchecked"})
                com.vinplay.vbee.common.cache.DistCache<String, Map> companyBankCache =
                        com.vinplay.vbee.common.cache.CacheFactory.get("companyBankCache", Map.class);
                Map raw = companyBankCache.get(COMPANY_BANK_CACHE_KEY);
                companyBank = raw;
                if (companyBank == null) {
                    companyBank = userBankDao.getActiveCompanyBank();
                    if (companyBank != null) {
                        companyBankCache.put(COMPANY_BANK_CACHE_KEY, companyBank,
                                COMPANY_BANK_CACHE_TTL_MIN, TimeUnit.MINUTES);
                    }
                }
                if (companyBank != null && companyBank.get("id") != null) {
                    companyBankId = ((Number) companyBank.get("id")).intValue();
                }
            } catch (Exception e) {
                logger.warn("CreateDepositProcessor failed to resolve company bank", e);
            }

            // Create transaction — stamp company_bank_id so admin sees the exact
            // bank the user was shown, regardless of later active-bank changes.
            long txId = depositDao.createTransaction(userId, nickname, amount, "BANK",
                    bankName, bankNumber, bankCode, companyBankId, idempotencyKey);

            if (txId <= 0) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            String txCode = depositDao.generateTxCode(txId);

            // Publish to queue (non-blocking — if RabbitMQ fails, CMS will see pending row)
            DepositQueueService queueService = new DepositQueueServiceImpl();
            try {
                ((com.vinplay.dal.service.impl.DepositQueueServiceImpl) queueService)
                        .publishNewDeposit(txId, txCode, userId, nickname, amount, bankName, bankNumber, holderName);
            } catch (Exception e) {
                logger.warn("CreateDepositProcessor failed to publish to queue txId=" + txId, e);
            }

            // Insert audit log
            try {
                depositDao.insertAuditLog(txId, txCode, "CREATED", null, nickname, "USER", null);
            } catch (Exception e) {
                logger.warn("CreateDepositProcessor failed to insert audit log txId=" + txId, e);
            }

            // Build response
            JSONObject data = new JSONObject();
            data.put("tx_code", txCode);
            data.put("amount", amount);
            data.put("status", "PENDING");
            data.put("created_at", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));

            // SUN-601 fix: use the SAME companyBank resolved above (don't re-query).
            // This guarantees the user-visible bank matches the stored company_bank_id.
            if (companyBank != null) {
                JSONObject companyBankJson = new JSONObject();
                companyBankJson.put("id", companyBankId);
                companyBankJson.put("bank_name", companyBank.get("bank_name"));
                companyBankJson.put("bank_number", companyBank.get("bank_number"));
                companyBankJson.put("account_holder", companyBank.get("account_holder"));
                companyBankJson.put("code", companyBank.get("code"));
                data.put("company_bank", companyBankJson);
            }

            response.put("success", true);
            response.put("data", data);
        } catch (Exception e) {
            logger.error("CreateDepositProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}

