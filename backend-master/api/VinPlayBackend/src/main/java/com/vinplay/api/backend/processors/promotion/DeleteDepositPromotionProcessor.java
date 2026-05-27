package com.vinplay.api.backend.processors.promotion;

import com.vinplay.dal.dao.DepositPromotionDao;
import com.vinplay.dal.dao.impl.DepositPromotionDaoImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * Admin API: Delete deposit promotion config.
 * Command ID: 9644
 *
 * Hard-deletes if no claims exist; rejects if promotion has claim history.
 * Params: aat, id
 */
public class DeleteDepositPromotionProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            String idStr = request.getParameter("id");
            if (idStr == null || idStr.trim().isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "id is required");
                return response.toString();
            }

            long promoId = Long.parseLong(idStr);

            DepositPromotionDao dao = new DepositPromotionDaoImpl();

            int claimUsersCount = dao.countDistinctUsers(promoId);
            if (claimUsersCount > 0) {
                response.put("success", false);
                response.put("errorCode", "5011");
                response.put("message", "Cannot physical delete promotion that already has claim history. Please deactivate it instead.");
                return response.toString();
            }

            boolean ok = dao.deletePromotion(promoId);

            response.put("success", ok);
            if (!ok) {
                response.put("errorCode", "5010");
                response.put("message", "Promotion not found or could not be deleted");
            }
        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("errorCode", "4001");
            response.put("message", "Invalid parameter format");
        } catch (Exception e) {
            logger.error("DeleteDepositPromotionProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }
}
