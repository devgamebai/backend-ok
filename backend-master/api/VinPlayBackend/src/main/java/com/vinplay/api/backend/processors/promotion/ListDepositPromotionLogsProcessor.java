package com.vinplay.api.backend.processors.promotion;

import com.vinplay.dal.dao.DepositPromotionDao;
import com.vinplay.dal.dao.impl.DepositPromotionDaoImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * Admin API: List deposit promotion claim logs.
 * Command ID: 9643
 *
 * Params:
 *   at         - admin token (required)
 *   promo_id   - (optional) filter by promotion ID (number)
 *   user_id    - (optional) filter by user ID (number). If a non-numeric value is
 *                passed, it is treated as a nickname and forwarded to nick_name filter.
 *   nick_name  - (optional) filter by nickname (partial match, LIKE %nick_name%).
 *                Takes precedence if both user_id (non-numeric) and nick_name are given.
 *   date_from  - (optional) yyyy-MM-dd
 *   date_to    - (optional) yyyy-MM-dd
 *   page       - (optional, default 1)
 *   limit      - (optional, default 20, max 100)
 */
public class ListDepositPromotionLogsProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            // promo_id — must be a number if provided
            String promoIdStr = trim(request.getParameter("promo_id"));
            Long promoId = null;
            if (promoIdStr != null) {
                try {
                    promoId = Long.parseLong(promoIdStr);
                } catch (NumberFormatException e) {
                    response.put("success", false);
                    response.put("errorCode", "4002");
                    response.put("message", "promo_id phải là số nguyên");
                    return response.toString();
                }
            }

            // user_id — nếu là số thì filter theo user_id,
            // nếu không phải số thì fallback sang nick_name filter
            String userIdStr = trim(request.getParameter("user_id"));
            Long userId = null;
            String nickNameFromUserId = null;
            if (userIdStr != null) {
                try {
                    userId = Long.parseLong(userIdStr);
                } catch (NumberFormatException e) {
                    // Caller truyền nickname vào user_id → treat as nick_name
                    nickNameFromUserId = userIdStr;
                }
            }

            // nick_name param tường minh (ưu tiên hơn fallback từ user_id)
            String nickNameParam = trim(request.getParameter("nick_name"));
            String nickName = (nickNameParam != null) ? nickNameParam : nickNameFromUserId;

            String dateFrom = trim(request.getParameter("date_from"));
            String dateTo   = trim(request.getParameter("date_to"));

            int page = parseIntSafe(request.getParameter("page"), 1);
            if (page < 1) page = 1;

            int limit = parseIntSafe(request.getParameter("limit"), 20);
            if (limit < 1) limit = 1;
            if (limit > 100) limit = 100;

            DepositPromotionDao dao = new DepositPromotionDaoImpl();
            List<Map<String, Object>> logs = dao.listClaimLogs(promoId, userId, nickName,
                    dateFrom, dateTo, page, limit);

            JSONArray arr = new JSONArray();
            for (Map<String, Object> row : logs) {
                arr.put(new JSONObject(row));
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", arr);
            response.put("page", page);
            response.put("limit", limit);

        } catch (Exception e) {
            logger.error("ListDepositPromotionLogsProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", e.getMessage() != null ? e.getMessage() : "Internal error");
        }
        return response.toString();
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static int parseIntSafe(String s, int defaultVal) {
        if (s == null || s.trim().isEmpty()) return defaultVal;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return defaultVal; }
    }
}
