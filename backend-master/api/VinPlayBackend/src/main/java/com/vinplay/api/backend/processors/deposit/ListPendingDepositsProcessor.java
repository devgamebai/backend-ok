package com.vinplay.api.backend.processors.deposit;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.dao.DepositTransactionDao;
import com.vinplay.dal.dao.impl.DepositTransactionDaoImpl;
import com.vinplay.dal.service.DepositLockService;
import com.vinplay.dal.service.impl.DepositLockServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public class ListPendingDepositsProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String accessToken = request.getParameter("at");
            if (accessToken == null || accessToken.isEmpty()) accessToken = request.getParameter("aat");

            // Validate admin token
            if (accessToken == null || accessToken.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = instance.getMap("cacheToken");
            if (!tokenMap.containsKey(accessToken)) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            // Parse parameters
            String status = request.getParameter("status");
            // Allow all valid statuses, or null/empty for ALL records
            if (status != null && !status.isEmpty()) {
                if (!"PENDING".equals(status) && !"PROCESSING".equals(status)
                        && !"APPROVED".equals(status) && !"REJECTED".equals(status)
                        && !"EXPIRED".equals(status) && !"CANCELLED".equals(status)) {
                    response.put("success", false);
                    response.put("errorCode", "4001");
                    response.put("message", "Invalid status");
                    return response.toString();
                }
            }

            int page = 1;
            int limit = 50;
            try {
                String pageStr = request.getParameter("page");
                if (pageStr != null && !pageStr.isEmpty()) {
                    page = Integer.parseInt(pageStr);
                }
                String limitStr = request.getParameter("limit");
                if (limitStr != null && !limitStr.isEmpty()) {
                    limit = Integer.parseInt(limitStr);
                }
            } catch (NumberFormatException e) {
                // use defaults
            }
            if (page < 1) page = 1;
            if (limit < 1) limit = 1;
            if (limit > 100) limit = 100;

            DepositTransactionDao txDao = new DepositTransactionDaoImpl();
            DepositLockService lockService = new DepositLockServiceImpl();

            List<Map<String, Object>> transactions = txDao.getTransactionsByStatus(status, page, limit);

            JSONArray dataArray = new JSONArray();
            for (Map<String, Object> tx : transactions) {
                JSONObject txObj = new JSONObject();
                Object txId = tx.get("id");
                txObj.put("id", txId != null ? txId : JSONObject.NULL);
                txObj.put("tx_code", tx.get("tx_code") != null ? tx.get("tx_code") : JSONObject.NULL);
                txObj.put("user_id", tx.get("user_id") != null ? tx.get("user_id") : JSONObject.NULL);
                txObj.put("nick_name", tx.get("nick_name") != null ? tx.get("nick_name") : JSONObject.NULL);
                txObj.put("amount", tx.get("amount") != null ? tx.get("amount") : 0);
                txObj.put("deposit_type", tx.get("deposit_type") != null ? tx.get("deposit_type") : JSONObject.NULL);
                txObj.put("status", tx.get("status") != null ? tx.get("status") : JSONObject.NULL);
                txObj.put("user_bank_name", tx.get("user_bank_name") != null ? tx.get("user_bank_name") : JSONObject.NULL);
                txObj.put("user_bank_number", tx.get("user_bank_number") != null ? tx.get("user_bank_number") : JSONObject.NULL);
                txObj.put("user_bank_code", tx.get("user_bank_code") != null ? tx.get("user_bank_code") : JSONObject.NULL);
                txObj.put("company_bank_id", tx.get("company_bank_id") != null ? tx.get("company_bank_id") : JSONObject.NULL);
                txObj.put("created_at", tx.get("created_at") != null ? tx.get("created_at").toString() : JSONObject.NULL);
                txObj.put("updated_at", tx.get("updated_at") != null ? tx.get("updated_at").toString() : JSONObject.NULL);
                txObj.put("force_approved", tx.get("force_approved") != null ? tx.get("force_approved") : 0);
                txObj.put("force_approved_by", tx.get("force_approved_by") != null ? tx.get("force_approved_by") : JSONObject.NULL);
                txObj.put("force_approved_at", tx.get("force_approved_at") != null ? tx.get("force_approved_at").toString() : JSONObject.NULL);

                // Check lock info
                if (txId != null) {
                    long id = ((Number) txId).longValue();
                    Map<String, Object> lockInfo = lockService.getLockInfo(id);
                    if (lockInfo != null) {
                        txObj.put("locked_by", lockInfo.get("operator"));
                        txObj.put("lock_platform", lockInfo.get("platform"));
                    } else {
                        txObj.put("locked_by", JSONObject.NULL);
                        txObj.put("lock_platform", JSONObject.NULL);
                    }
                } else {
                    txObj.put("locked_by", JSONObject.NULL);
                    txObj.put("lock_platform", JSONObject.NULL);
                }

                dataArray.put(txObj);
            }

            response.put("success", true);
            response.put("data", dataArray);
        } catch (Exception e) {
            logger.error("ListPendingDepositsProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}
