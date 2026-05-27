package com.vinplay.api.backend.processors.promotion;

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
 * Admin API: List deposit promotion configs.
 * Command ID: 9641
 *
 * Params: at, promo_type (optional, 0=all), gate (optional: ALL/BANK/CRYPTO)
 */
public class ListDepositPromotionProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            // Auth is handled by admin auth interceptor (aat param)

            String promoTypeStr = request.getParameter("promo_type");
            int promoType = (promoTypeStr != null && !promoTypeStr.isEmpty())
                    ? Integer.parseInt(promoTypeStr) : 0;

            // Filter theo gate (optional): null/empty = all, BANK, CRYPTO, ALL
            String gate = request.getParameter("gate");

            DepositPromotionDao dao = new DepositPromotionDaoImpl();
            List<Map<String, Object>> list = dao.listPromotions(promoType, gate);

            JSONArray arr = new JSONArray();
            for (Map<String, Object> row : list) {
                if (row.containsKey("condition_text")) {
                    row.put("condition", row.remove("condition_text"));
                }
                JSONObject item = new JSONObject(row);
                arr.put(item);
            }

            response.put("success", true);
            response.put("data", arr);
            response.put("total", list.size());
            if (gate != null && !gate.isEmpty()) {
                response.put("filter_gate", gate.toUpperCase());
            }
        } catch (Exception e) {
            logger.error("ListDepositPromotionProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}
