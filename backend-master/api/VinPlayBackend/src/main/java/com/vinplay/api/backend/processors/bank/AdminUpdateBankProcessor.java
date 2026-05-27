package com.vinplay.api.backend.processors.bank;

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

public class AdminUpdateBankProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String adminToken = request.getParameter("at");
            if (adminToken == null || adminToken.isEmpty()) adminToken = request.getParameter("aat");
            // Accept both new params and legacy params (nn, cn, bnum, bn, br, ann)
            String nickName = request.getParameter("nick_name");
            if (nickName == null) nickName = request.getParameter("nn");
            String bankIdStr = request.getParameter("bank_id");
            String bankNameDirect = request.getParameter("bn"); // legacy sends bank name string
            String customerName = request.getParameter("customer_name");
            if (customerName == null) customerName = request.getParameter("cn");
            String bankNumber = request.getParameter("bank_number");
            if (bankNumber == null) bankNumber = request.getParameter("bnum");
            String branch = request.getParameter("branch");
            if (branch == null) branch = request.getParameter("br");
            String reason = request.getParameter("reason");
            String adminNickname = request.getParameter("admin_nn");
            if (adminNickname == null) adminNickname = request.getParameter("ann");
            String statusStr = request.getParameter("st");

            // Validate required params
            if (nickName == null || nickName.isEmpty()
                    || (bankIdStr == null || bankIdStr.isEmpty()) && (bankNameDirect == null || bankNameDirect.isEmpty())
                    || customerName == null || customerName.isEmpty()
                    || bankNumber == null || bankNumber.isEmpty()) {
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

            // Validate bank_number: digits only, 6-20 chars
            bankNumber = bankNumber.trim();
            if (!bankNumber.matches("^\\d{6,20}$")) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            // Parse bank_id or resolve from bank name
            int bankId = -1;
            if (bankIdStr != null && !bankIdStr.isEmpty()) {
                try {
                    bankId = Integer.parseInt(bankIdStr);
                } catch (NumberFormatException e) {
                    // not a number, ignore
                }
            }
            // If bank_id not provided, legacy sends bank name directly (bn param)
            // In that case, use the bank name directly (skip bank_id lookup)
            String resolvedBankName = null;
            if (bankId <= 0 && bankNameDirect != null && !bankNameDirect.isEmpty()) {
                resolvedBankName = bankNameDirect; // use directly as bank name
            }

            // Find user by nick_name: try Hazelcast cache first, fallback to DB
            long userId = -1;
            IMap<String, UserCacheModel> userMap = HazelcastClientFactory.getInstance().getMap("users");
            UserCacheModel userCache = userMap.get(nickName);
            if (userCache != null) {
                userId = userCache.getId();
            } else {
                // Fallback: lookup by nick_name in DB
                try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                    try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE nick_name = ?")) {
                        ps.setString(1, nickName);
                        try (java.sql.ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                userId = rs.getLong("id");
                            }
                        }
                    }
                }
            }
            if (userId <= 0) {
                response.put("success", false);
                response.put("errorCode", "1001");
                response.put("message", "User not found");
                return response.toString();
            }
            UserBankDao dao = new UserBankDaoImpl();

            // Resolve bank name: either from bank_id or direct bank name
            String bankName;
            if (resolvedBankName != null) {
                bankName = resolvedBankName; // legacy: bank name sent directly
            } else {
                bankName = dao.getBankNameById(bankId);
            }
            if (bankName == null || bankName.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "3002");
                return response.toString();
            }

            // SUN-1389: block same customer_name on same bank_id across
            // different users. Account-number duplication across banks
            // stays allowed (SUN-602) so the legacy global
            // bankNumberExists check is no longer used here — it would
            // reject legitimate cross-bank account-number reuse while
            // missing the actual multi-account abuse pattern (same name
            // + same bank). The check excludes the current user's row,
            // so editing one's own entry doesn't self-collide.
            Map<String, Object> currentBank = dao.getUserBank(userId);
            if (dao.nameOnBankExistsForOtherUser(bankId, customerName, userId)) {
                response.put("success", false);
                response.put("errorCode", "3003");
                return response.toString();
            }

            // Determine editor name
            String editor = (adminNickname != null && !adminNickname.isEmpty()) ? adminNickname : "admin";

            // Update or insert user bank. bankId may be -1 when admin used the
            // legacy bn=<name> form — in that case the row's bank_id stays NULL
            // and the read path falls back to name-string match (see
            // UserBankDaoImpl.getUserBank, 2026-05-12 migration).
            boolean success;
            if (currentBank != null) {
                success = dao.updateUserBank(userId, bankName, customerName, bankNumber, branch, editor, bankId);
                // Update status if st param provided
                if (success && statusStr != null && !statusStr.isEmpty()) {
                    try {
                        int newStatus = Integer.parseInt(statusStr);
                        dao.updateUserBankStatus(userId, newStatus, editor);
                    } catch (NumberFormatException ignored) {}
                }
            } else {
                success = dao.insertUserBank(userId, nickName, bankName, customerName, bankNumber, branch, bankId);
            }

            if (success) {
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
                    data.put("last_editor", bankInfo.get("last_editor") != null ? bankInfo.get("last_editor") : JSONObject.NULL);
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
            logger.error("AdminUpdateBankProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}
