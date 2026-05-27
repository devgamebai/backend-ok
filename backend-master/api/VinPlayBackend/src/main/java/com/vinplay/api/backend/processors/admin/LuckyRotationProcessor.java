package com.vinplay.api.backend.processors.admin;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

public class LuckyRotationProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String nn = request.getParameter("nn");

            if (nn == null || nn.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame")) {
                JSONArray list = new JSONArray();
                String sql = "SELECT * FROM rotate_slot_free WHERE nick_name = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, nn);
                    try (ResultSet rs = ps.executeQuery()) {
                        ResultSetMetaData meta = rs.getMetaData();
                        while (rs.next()) {
                            JSONObject item = new JSONObject();
                            for (int i = 1; i <= meta.getColumnCount(); i++) {
                                String colName = meta.getColumnName(i);
                                Object val = rs.getObject(i);
                                item.put(colName, val != null ? val : JSONObject.NULL);
                            }
                            list.put(item);
                        }
                    }
                }

                JSONObject data = new JSONObject();
                data.put("list", list);
                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", data.toString());
            }
        } catch (Exception e) {
            logger.error("LuckyRotationProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
