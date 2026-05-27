package com.vinplay.api.processors.bank;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.dao.UserBankDao;
import com.vinplay.dal.dao.impl.UserBankDaoImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class SubmitUserBankProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String accessToken = request.getParameter("at");

            // FE may send params as JSON body with Content-Type: application/json
            // Jetty won't parse that as form params, so we read the body manually
            java.util.Map<String, String> bodyParams = new java.util.HashMap<>();
            String contentType = request.getContentType();
            if (contentType != null && (contentType.contains("application/json") || contentType.contains("application/x-www-form-urlencoded"))) {
                try {
                    StringBuilder sb = new StringBuilder();
                    java.io.BufferedReader reader = request.getReader();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    String body = sb.toString().trim();
                    if (body.startsWith("{")) {
                        // JSON body: {"bank_id":"90","customer_name":"..."}
                        org.json.JSONObject jsonBody = new org.json.JSONObject(body);
                        for (String key : jsonBody.keySet()) {
                            bodyParams.put(key, jsonBody.optString(key, null));
                        }
                    } else if (!body.isEmpty()) {
                        // Form-encoded body: bank_id=90&customer_name=...
                        for (String pair : body.split("&")) {
                            String[] kv = pair.split("=", 2);
                            if (kv.length == 2) {
                                bodyParams.put(java.net.URLDecoder.decode(kv[0], "UTF-8"),
                                               java.net.URLDecoder.decode(kv[1], "UTF-8"));
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("SubmitUserBankProcessor: failed to parse request body", e);
                }
            }

            // Read params: query string first, then body fallback
            String bankIdStr = request.getParameter("bank_id");
            if (bankIdStr == null) bankIdStr = request.getParameter("bankId");
            if (bankIdStr == null) bankIdStr = bodyParams.get("bank_id");
            if (bankIdStr == null) bankIdStr = bodyParams.get("bankId");

            String customerName = request.getParameter("customer_name");
            if (customerName == null) customerName = request.getParameter("customerName");
            if (customerName == null) customerName = bodyParams.get("customer_name");
            if (customerName == null) customerName = bodyParams.get("customerName");

            String bankNumber = request.getParameter("bank_number");
            if (bankNumber == null) bankNumber = request.getParameter("bankNumber");
            if (bankNumber == null) bankNumber = bodyParams.get("bank_number");
            if (bankNumber == null) bankNumber = bodyParams.get("bankNumber");

            String branch = request.getParameter("branch");
            if (branch == null) branch = bodyParams.get("branch");

            // Validate required params
            if (accessToken == null || accessToken.isEmpty()
                    || bankIdStr == null || bankIdStr.isEmpty()
                    || customerName == null || customerName.isEmpty()
                    || bankNumber == null || bankNumber.isEmpty()) {
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

            // Validate customer_name length
            customerName = customerName.trim();
            if (customerName.isEmpty() || customerName.length() > 200) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            // Validate bank_number: digits only, 9-15 chars (Korean bank format)
            bankNumber = bankNumber.trim();
            if (!bankNumber.matches("^\\d{9,15}$")) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            // Parse bank_id
            int bankId;
            try {
                bankId = Integer.parseInt(bankIdStr);
            } catch (NumberFormatException e) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            String nickname = (String) tokenMap.get(accessToken);

            // Get user from Hazelcast cache
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
            UserBankDao dao = new UserBankDaoImpl();

            // Validate bank_id exists in banks table
            String bankName = dao.getBankNameById(bankId);
            if (bankName == null) {
                response.put("success", false);
                response.put("errorCode", "3002");
                return response.toString();
            }

            // Check if user already has a bank
            if (dao.userHasBank(userId)) {
                response.put("success", false);
                response.put("errorCode", "3001");
                return response.toString();
            }

            // SUN-602: account numbers are scoped per-bank, so global
            // bank_number uniqueness is NOT enforced (two banks may
            // legitimately issue the same short account number).
            //
            // SUN-1389: but the same customer_name at the same bank_id
            // registered by a different user IS the multi-account signal
            // we want to block. Operators saw 100+ such rings in
            // production (one with 26 user accounts sharing one
            // (name, bank) tuple). userHasBank already prevents THIS
            // user from holding more than one bank.
            if (dao.nameOnBankExistsForOtherUser(bankId, customerName, userId)) {
                response.put("success", false);
                response.put("errorCode", "3003");
                return response.toString();
            }

            // Insert user bank. Pass bankId so users_bank.bank_id is set —
            // future admin renames of banks.bank_name flow through this row
            // via the FK JOIN in getUserBank (2026-05-12 migration).
            boolean inserted = dao.insertUserBank(userId, nickname, bankName, customerName, bankNumber, branch, bankId);
            if (inserted) {
                // Return success with bank info
                Map<String, Object> bankInfo = dao.getUserBank(userId);
                JSONObject data = new JSONObject();
                if (bankInfo != null) {
                    data.put("id", bankInfo.get("id"));
                    data.put("user_id", bankInfo.get("user_id"));
                    data.put("nick_name", bankInfo.get("nick_name"));
                    data.put("bank_name", bankInfo.get("bank_name"));
                    data.put("customer_name", bankInfo.get("customer_name"));
                    data.put("bank_number", bankInfo.get("bank_number"));
                    data.put("status", bankInfo.get("status"));
                    data.put("is_locked", bankInfo.get("is_locked"));
                    data.put("branch", bankInfo.get("branch") != null ? bankInfo.get("branch") : JSONObject.NULL);
                    data.put("code", bankInfo.get("code") != null ? bankInfo.get("code") : JSONObject.NULL);
                    data.put("logo", bankInfo.get("logo") != null ? bankInfo.get("logo") : JSONObject.NULL);
                }
                response.put("success", true);
                response.put("data", data);
            } else {
                response.put("success", false);
                response.put("errorCode", "1001");
            }
        } catch (Exception e) {
            logger.error("SubmitUserBankProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}
