package com.vinplay.api.processors.promotion;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.dao.DepositPromotionDao;
import com.vinplay.dal.dao.impl.DepositPromotionDaoImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * Portal API: Get user's promotion claim history and current status.
 * Command ID: 3061
 *
 * Requires auth (access token).
 * Params: at (required), page (optional), limit (optional)
 */
public class GetUserPromotionStatusProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String accessToken = request.getParameter("at");

            if (accessToken == null || accessToken.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                response.put("message", "Access token is required");
                return response.toString();
            }

            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = instance.getMap("cacheToken");
            if (!tokenMap.containsKey(accessToken)) {
                response.put("success", false);
                response.put("errorCode", "1001");
                response.put("message", "Invalid access token");
                return response.toString();
            }
            String nickname = tokenMap.get(accessToken);

            // Resolve userId from nickname
            IMap<String, Object> userMap = instance.getMap("users");
            long userId = 0;
            if (userMap.containsKey(nickname)) {
                Object userCache = userMap.get(nickname);
                try {
                    java.lang.reflect.Method getId = userCache.getClass().getMethod("getId");
                    userId = ((Number) getId.invoke(userCache)).longValue();
                } catch (Exception e) {
                    logger.warn("GetUserPromotionStatusProcessor: cannot resolve userId for " + nickname, e);
                }
            }

            if (userId <= 0) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "Cannot resolve user");
                return response.toString();
            }

            DepositPromotionDao dao = new DepositPromotionDaoImpl();

            // Eligibility summary
            JSONObject eligibility = new JSONObject();
            boolean hasFirstClaim = dao.hasFirstDepositClaim(userId);
            boolean hasFirstClaimToday = dao.hasFirstDepositClaimToday(userId);
            eligibility.put("first_deposit_claimed", hasFirstClaim);
            eligibility.put("first_deposit_claimed_today", hasFirstClaimToday);
            eligibility.put("eligible_first_deposit", !hasFirstClaim);
            eligibility.put("eligible_daily_deposit", hasFirstClaim && !hasFirstClaimToday);

            // Daily promo usage
            Map<String, Object> dailyPromo = dao.getActivePromotion(2, "ALL");
            if (dailyPromo != null) {
                long dailyPromoId = ((Number) dailyPromo.get("id")).longValue();
                Object maxClaimsDailyObj = dailyPromo.get("max_claims_daily");
                Object maxBonusDailyObj = dailyPromo.get("max_bonus_daily");

                int claimsToday = dao.countUserClaimsToday(userId, dailyPromoId);
                long bonusToday = dao.sumUserBonusToday(userId, dailyPromoId);

                eligibility.put("daily_claims_today", claimsToday);
                eligibility.put("daily_bonus_today", bonusToday);

                if (maxClaimsDailyObj != null) {
                    int maxClaims = ((Number) maxClaimsDailyObj).intValue();
                    eligibility.put("daily_claims_remaining", Math.max(0, maxClaims - claimsToday));
                }
                if (maxBonusDailyObj != null) {
                    long maxBonus = ((Number) maxBonusDailyObj).longValue();
                    eligibility.put("daily_bonus_remaining", Math.max(0, maxBonus - bonusToday));
                }
            }

            // Recent claim history
            String pageStr = request.getParameter("page");
            int page = (pageStr != null && !pageStr.isEmpty()) ? Integer.parseInt(pageStr) : 1;
            String limitStr = request.getParameter("limit");
            int limit = (limitStr != null && !limitStr.isEmpty()) ? Integer.parseInt(limitStr) : 10;
            if (limit > 50) limit = 50;

            List<Map<String, Object>> logs = dao.listClaimLogs(null, userId, null, null, null, page, limit);
            JSONArray history = new JSONArray();
            for (Map<String, Object> log : logs) {
                JSONObject item = new JSONObject();
                item.put("id", ((Number) log.get("id")).longValue());
                item.put("promo_type", ((Number) log.get("promo_type")).intValue());
                item.put("deposit_amount", ((Number) log.get("deposit_amount")).longValue());
                item.put("bonus_amount", ((Number) log.get("bonus_amount")).longValue());
                item.put("claim_date", log.get("claim_date") != null ? log.get("claim_date").toString() : "");
                item.put("created_at", log.get("created_at") != null ? log.get("created_at").toString() : "");
                history.put(item);
            }

            response.put("success", true);
            response.put("nickname", nickname);
            response.put("eligibility", eligibility);
            response.put("history", history);
            response.put("page", page);
            response.put("limit", limit);

        } catch (Exception e) {
            logger.error("GetUserPromotionStatusProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}
