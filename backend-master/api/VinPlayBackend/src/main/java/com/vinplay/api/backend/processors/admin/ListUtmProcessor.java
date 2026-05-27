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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ListUtmProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    private static final Set<String> ALLOWED_TYPES = new HashSet<String>(Arrays.asList("campain", "source", "medium"));

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String type = request.getParameter("type");

            if (type == null || !ALLOWED_TYPES.contains(type)) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            int page = 1;
            int limit = 10;
            try { String s = request.getParameter("pn"); if (s != null && !s.isEmpty()) page = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            try { String s = request.getParameter("l"); if (s != null && !s.isEmpty()) limit = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            if (page < 1) page = 1;
            if (limit < 1) limit = 10;
            if (limit > 100) limit = 100;

            int offset = (page - 1) * limit;
            String tableName = "utm_" + type;

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                int total = 0;
                try (PreparedStatement countPs = conn.prepareStatement("SELECT COUNT(*) FROM " + tableName)) {
                    try (ResultSet countRs = countPs.executeQuery()) {
                        if (countRs.next()) total = countRs.getInt(1);
                    }
                }

                JSONArray list = new JSONArray();
                String sql = "SELECT * FROM " + tableName + " ORDER BY id DESC LIMIT ?,?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, offset);
                    ps.setInt(2, limit);
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
                data.put("total", total);
                data.put("list", list);
                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", data.toString());
            }
        } catch (Exception e) {
            logger.error("ListUtmProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
