package com.vinplay.api.backend.processors.admin;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

public class GetAdminInfoProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String aidStr = request.getParameter("aid");

            if (aidStr == null || aidStr.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            int aid = Integer.parseInt(aidStr);

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                String sql = "SELECT * FROM user WHERE ID = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, aid);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            JSONObject data = new JSONObject();
                            ResultSetMetaData meta = rs.getMetaData();
                            for (int i = 1; i <= meta.getColumnCount(); i++) {
                                String colName = meta.getColumnName(i);
                                // Strip sensitive fields from response
                                if ("Password".equalsIgnoreCase(colName)) continue;
                                Object val = rs.getObject(i);
                                data.put(colName, val != null ? val : JSONObject.NULL);
                            }
                            response.put("success", true);
                            response.put("errorCode", "0");
                            response.put("data", data.toString());
                        } else {
                            response.put("success", false);
                            response.put("errorCode", "1005");
                        }
                    }
                }
            }
        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("errorCode", "1001");
        } catch (Exception e) {
            logger.error("GetAdminInfoProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
