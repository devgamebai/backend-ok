package com.vinplay.api.backend.processors.agent;

import com.vinplay.dal.rebate.RebateService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

public class GetRebateConfig4AgencyProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest) param.get();
        JSONObject response = new JSONObject();
        
        try {
            String agentCode = request.getParameter("rc");
            String nickName = request.getParameter("nn");

            if ((agentCode == null || agentCode.isEmpty()) && (nickName == null || nickName.isEmpty())) {
                response.put("success", false);
                response.put("errorCode", "1002");
                response.put("message", "Missing agent code or nickname");
                return response.toString();
            }

            int agentId = -1;
            double depositRate = 0;
            try (Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                PreparedStatement stm;
                if (agentCode != null && !agentCode.isEmpty()) {
                    stm = conn.prepareStatement(
                        "SELECT id, IFNULL(deposit_rate, 0) as deposit_rate FROM vinplay_admin.useragent WHERE code = ?");
                    stm.setString(1, agentCode);
                } else {
                    stm = conn.prepareStatement(
                        "SELECT id, IFNULL(deposit_rate, 0) as deposit_rate FROM vinplay_admin.useragent WHERE nickname = ?");
                    stm.setString(1, nickName);
                }
                ResultSet rs = stm.executeQuery();
                if (rs.next()) {
                    agentId = rs.getInt("id");
                    depositRate = rs.getDouble("deposit_rate");
                }
                rs.close();
                stm.close();
            }

            if (agentId == -1) {
                response.put("success", false);
                response.put("errorCode", "1002");
                response.put("message", "Agent not found");
                return response.toString();
            }

            // 2-decimal preservation for agency FE (so 0.50 renders as "0.50", not "0.5").
            // org.json strips trailing zeros from every Number subtype, so emit as String.
            String depositRate2dp = java.math.BigDecimal.valueOf(depositRate)
                    .setScale(2, java.math.RoundingMode.HALF_UP)
                    .toPlainString();

            // Retrieve rebate config read-only
            Map<String, Object> config = RebateService.getConfig(agentId);
            if (config != null) {
                JSONObject data = new JSONObject(config);
                data.put("deposit_rate", depositRate2dp);
                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", data);
            } else {
                // Trả tối thiểu deposit_rate kể cả khi chưa có rebate_config
                JSONObject data = new JSONObject();
                data.put("deposit_rate", depositRate2dp);
                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", data);
            }

        } catch (Exception e) {
            logger.error("GetRebateConfig4AgencyProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}
