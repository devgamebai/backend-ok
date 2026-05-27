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
 * Admin API: Update deposit promotion config.
 * Command ID: 9642
 *
 * Params: at, id, gate (ALL/BANK/CRYPTO, default ALL),
 *         bonus_percent, max_users, turnover_factor,
 *         max_bonus_per_tx, max_claims_daily, max_bonus_daily,
 *         start_time, end_time, status
 */
public class UpdateDepositPromotionProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            // Auth is handled by admin auth interceptor (aat param)

            long promoId = Long.parseLong(request.getParameter("id"));

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
            int status = Integer.parseInt(request.getParameter("status"));

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
            boolean ok = dao.updatePromotion(promoId, gate, bonusPercent, maxUsers,
                    turnoverFactor, maxBonusPerTx, maxClaimsDaily, maxBonusDaily,
                    startTime, endTime, status, title, description, condition);

            response.put("success", ok);
            if (!ok) {
                response.put("errorCode", "5010");
                response.put("message", "Promotion not found or not updated");
            }
        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("errorCode", "4001");
            response.put("message", "Invalid parameter format");
        } catch (Exception e) {
            logger.error("UpdateDepositPromotionProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}
