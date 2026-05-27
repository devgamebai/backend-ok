package com.vinplay.api.backend.processors.promotion;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.dao.DepositPromotionDao;
import com.vinplay.dal.dao.impl.DepositPromotionDaoImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * Admin API: Create a new deposit promotion config.
 * Command ID: 9640
 *
 * Params: at, promo_type, gate (ALL/BANK/CRYPTO, default ALL),
 *         bonus_percent, max_users, turnover_factor,
 *         max_bonus_per_tx, max_claims_daily, max_bonus_daily,
 *         start_time, end_time
 */
public class CreateDepositPromotionProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            // Auth is handled by admin auth interceptor (aat param)

            // Parse parameters
            int promoType = Integer.parseInt(request.getParameter("promo_type"));
            if (promoType != 1 && promoType != 2) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "promo_type must be 1 or 2");
                return response.toString();
            }

            // Gate: kênh nạp tiền (ALL, BANK, CRYPTO)
            String gate = request.getParameter("gate");
            if (gate == null || gate.isEmpty()) gate = "ALL";
            gate = gate.toUpperCase();
            if (!gate.equals("BANK") && !gate.equals("CRYPTO") && !gate.equals("ALL")) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "gate must be ALL, BANK, or CRYPTO");
                return response.toString();
            }

            double bonusPercent = Double.parseDouble(request.getParameter("bonus_percent"));
            int maxUsers = Integer.parseInt(request.getParameter("max_users"));
            double turnoverFactor = Double.parseDouble(request.getParameter("turnover_factor"));
            long maxBonusPerTx = Long.parseLong(request.getParameter("max_bonus_per_tx"));

            String maxClaimsDailyStr = request.getParameter("max_claims_daily");
            Integer maxClaimsDaily = (maxClaimsDailyStr != null && !maxClaimsDailyStr.isEmpty())
                    ? Integer.parseInt(maxClaimsDailyStr) : null;

            String maxBonusDailyStr = request.getParameter("max_bonus_daily");
            Long maxBonusDaily = (maxBonusDailyStr != null && !maxBonusDailyStr.isEmpty())
                    ? Long.parseLong(maxBonusDailyStr) : null;

            String startTime = request.getParameter("start_time");
            String endTime = request.getParameter("end_time");

            String title = request.getParameter("title");
            String description = request.getParameter("description");
            String condition = request.getParameter("condition");

            if (startTime == null || endTime == null) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "start_time and end_time are required");
                return response.toString();
            }

            if (title == null || title.trim().isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "title is required");
                return response.toString();
            }

            if (description == null || description.trim().isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "description is required");
                return response.toString();
            }

            DepositPromotionDao dao = new DepositPromotionDaoImpl();
            long id = dao.createPromotion(promoType, gate, bonusPercent, maxUsers,
                    turnoverFactor, maxBonusPerTx, maxClaimsDaily, maxBonusDaily,
                    startTime, endTime, title, description, condition);

            if (id > 0) {
                response.put("success", true);
                response.put("id", id);
            } else {
                response.put("success", false);
                response.put("errorCode", "1001");
            }
        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("errorCode", "4001");
            response.put("message", "Invalid parameter format");
        } catch (Exception e) {
            logger.error("CreateDepositPromotionProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}
