package com.vinplay.api.processors.bank;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.dao.UserBankDao;
import com.vinplay.dal.dao.impl.UserBankDaoImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public class BankListProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String accessToken = request.getParameter("at");

            if (accessToken == null || accessToken.isEmpty()) {
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

            UserBankDao dao = new UserBankDaoImpl();
            List<Map<String, Object>> banks = dao.getActiveBanks();

            JSONArray bankArray = new JSONArray();
            for (Map<String, Object> bank : banks) {
                JSONObject bankObj = new JSONObject();
                bankObj.put("id", bank.get("id"));
                bankObj.put("bank_name", bank.get("bank_name"));
                bankObj.put("code", bank.get("code"));
                bankObj.put("logo", bank.get("logo") != null ? bank.get("logo") : JSONObject.NULL);
                bankArray.put(bankObj);
            }

            response.put("success", true);
            response.put("data", bankArray);
        } catch (Exception e) {
            logger.error("BankListProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}
