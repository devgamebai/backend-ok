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
 * Portal API: Fetch active promotions for player display (Events popup).
 * Command ID: 3060
 *
 * Auth: Optional. If access token provided, includes user-specific eligibility info.
 * Params: at (optional)
 */
public class GetPromotionsProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String accessToken = request.getParameter("at");

            // Resolve user if token provided
            String nickname = null;
            long userId = 0;
            if (accessToken != null && !accessToken.isEmpty()) {
                try {
                    HazelcastInstance instance = HazelcastClientFactory.getInstance();
                    IMap<String, String> tokenMap = instance.getMap("cacheToken");
                    if (tokenMap.containsKey(accessToken)) {
                        nickname = tokenMap.get(accessToken);
                    }
                } catch (Exception e) {
                    logger.warn("GetPromotionsProcessor: token lookup failed", e);
                }
            }

            DepositPromotionDao dao = new DepositPromotionDaoImpl();
            JSONArray promotions = new JSONArray();

            // ── Active promotions only: status=1 AND within time range ──
            List<Map<String, Object>> activePromos = dao.listPromotions(0, "ALL");
            for (Map<String, Object> promo : activePromos) {
                int status = ((Number) promo.get("status")).intValue();
                if (status != 1) continue;

                // Skip promos outside their active time window
                try {
                    String startTime = promo.get("start_time") != null ? promo.get("start_time").toString() : null;
                    String endTime = promo.get("end_time") != null ? promo.get("end_time").toString() : null;
                    if (startTime != null && !startTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        java.util.Date now = new java.util.Date();
                        java.util.Date start = sdf.parse(startTime);
                        java.util.Date end = sdf.parse(endTime);
                        if (now.before(start) || now.after(end)) continue; // not yet started or expired
                    }
                } catch (Exception timeErr) {
                    logger.warn("GetPromotionsProcessor: time range parse error promoId=" + promo.get("id"), timeErr);
                }

                JSONObject item = new JSONObject();
                int promoType = ((Number) promo.get("promo_type")).intValue();
                double bonusPercent = ((Number) promo.get("bonus_percent")).doubleValue();
                long maxBonusPerTx = ((Number) promo.get("max_bonus_per_tx")).longValue();
                long promoId = ((Number) promo.get("id")).longValue();

                Object maxClaimsDaily = promo.get("max_claims_daily");
                Object maxBonusDaily = promo.get("max_bonus_daily");

                item.put("id", promoId);
                item.put("promo_type", promoType);
                item.put("bonus_percent", bonusPercent);
                item.put("max_bonus_per_tx", maxBonusPerTx);
                item.put("start_time", promo.get("start_time") != null ? promo.get("start_time").toString() : "");
                item.put("end_time", promo.get("end_time") != null ? promo.get("end_time").toString() : "");

                if (promo.get("turnover_factor") != null) {
                    item.put("turnover_factor", ((Number) promo.get("turnover_factor")).doubleValue());
                }
                if (promo.get("gate") != null) {
                    item.put("gate", promo.get("gate").toString());
                }

                // Read title/description from DB
                String title = promo.get("title") != null ? promo.get("title").toString() : "";
                if (!title.trim().isEmpty()) {
                    item.put("title", title);
                } else {
                    if (promoType == 1) item.put("title", "Khuyến mãi nạp đầu");
                    else if (promoType == 2) item.put("title", "Khuyến mãi nạp ngày");
                    else if (promoType == 0) item.put("title", "Hoàn trả");
                }

                if (promo.get("title_en") != null) {
                    item.put("title_en", promo.get("title_en").toString());
                }

                item.put("description", promo.get("description") != null ? promo.get("description").toString() : "");
                item.put("condition", promo.get("condition_text") != null ? promo.get("condition_text").toString() : "");

                if (promo.get("max_users") != null) {
                    item.put("max_users", ((Number) promo.get("max_users")).intValue());
                }

                if (promoType == 0) {
                    // Rebate (Hoàn Thua)
                    item.put("category", "rebate");
                    item.put("rebate_percent", bonusPercent);
                    item.put("max_rebate", maxBonusPerTx);
                    if (promo.get("payout_day") != null) item.put("payout_day", promo.get("payout_day").toString());
                    if (promo.get("payout_period") != null) item.put("payout_period", promo.get("payout_period").toString());

                } else if (promoType == 1) {
                    // First Deposit
                    item.put("category", "deposit");

                } else if (promoType == 2) {
                    // Daily Deposit
                    item.put("category", "deposit");
                    if (maxClaimsDaily != null) item.put("max_claims_daily", ((Number) maxClaimsDaily).intValue());
                    if (maxBonusDaily != null) item.put("max_bonus_daily", ((Number) maxBonusDaily).longValue());
                }

                // User-specific eligibility (if authenticated, deposit types only)
                if (nickname != null && userId > 0 && promoType > 0) {
                    try {
                        if (promoType == 1) {
                            boolean claimed = dao.hasFirstDepositClaim(userId);
                            item.put("user_claimed", claimed);
                            item.put("user_eligible", !claimed);
                        } else if (promoType == 2) {
                            if (maxClaimsDaily != null) {
                                int claimsToday = dao.countUserClaimsToday(userId, promoId);
                                item.put("user_claims_today", claimsToday);
                            }
                            if (maxBonusDaily != null) {
                                long bonusToday = dao.sumUserBonusToday(userId, promoId);
                                item.put("user_bonus_today", bonusToday);
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("GetPromotionsProcessor: eligibility check failed", e);
                    }
                }

                promotions.put(item);
            }

            response.put("success", true);
            response.put("data", promotions);
            response.put("total", promotions.length());

        } catch (Exception e) {
            logger.error("GetPromotionsProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }

    private String formatKRW(long amount) {
        if (amount >= 1000000) {
            return (amount / 1000000) + "M won";
        } else if (amount >= 1000) {
            return (amount / 1000) + "K won";
        }
        return amount + " won";
    }
}
