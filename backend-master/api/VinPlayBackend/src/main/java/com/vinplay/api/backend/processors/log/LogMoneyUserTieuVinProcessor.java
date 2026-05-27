package com.vinplay.api.backend.processors.log;

import com.vinplay.dal.service.impl.LogMoneyUserServiceImpl;
import com.vinplay.vbee.common.response.LogUserMoneyResponse;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class LogMoneyUserTieuVinProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String nn = request.getParameter("nn"); if (nn == null) nn = "";
            String an = request.getParameter("an"); if (an == null) an = "";
            String sn = request.getParameter("sn"); if (sn == null) sn = "";
            String ts = request.getParameter("ft"); if (ts == null) ts = request.getParameter("ts"); if (ts == null) ts = "";
            String te = request.getParameter("et"); if (te == null) te = request.getParameter("te"); if (te == null) te = "";
            int page = 1;
            try { String s = request.getParameter("p"); if (s != null && !s.isEmpty()) page = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            if (page < 1) page = 1;

            LogMoneyUserServiceImpl service = new LogMoneyUserServiceImpl();
            List<LogUserMoneyResponse> results = service.searchLogMoneyUser(nn, "", "vin", sn, an, ts, te, page, 1, 20);
            int total = service.countsearchLogMoneyUser(nn, "vin", sn, an, ts, te, 1);

            JSONArray arr = new JSONArray();
            if (results != null) {
                for (LogUserMoneyResponse r : results) {
                    JSONObject item = new JSONObject();
                    item.put("nickName", r.nickName != null ? r.nickName : "");
                    item.put("actionName", r.actionName != null ? r.actionName : "");
                    item.put("serviceName", r.serviceName != null ? r.serviceName : "");
                    item.put("currentMoney", r.currentMoney);
                    item.put("moneyExchange", r.moneyExchange);
                    item.put("fee", r.fee);
                    item.put("timeLog", r.transactionTime != null ? r.transactionTime : "");
                    item.put("description", r.description != null ? r.description : "");
                    arr.put(item);
                }
            }
            response.put("success", true);
            response.put("errorCode", "0");
            response.put("transactions", arr);
            response.put("data", arr);
            response.put("total", total);
            response.put("totalRecords", total);
            response.put("page", page);
        } catch (Exception e) {
            logger.error("LogMoneyUserTieuVinProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
            response.put("message", "Internal error");
        }
        return response.toString();
    }
}
