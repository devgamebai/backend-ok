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
import java.util.ArrayList;
import java.util.List;

public class ListLoginLogsProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            int page = 1;
            int limit = 10;
            try { String s = request.getParameter("pn"); if (s != null && !s.isEmpty()) page = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            try { String s = request.getParameter("l"); if (s != null && !s.isEmpty()) limit = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            if (page < 1) page = 1;
            if (limit < 1) limit = 10;
            if (limit > 100) limit = 100;

            String un = request.getParameter("un");
            String fd = request.getParameter("fd");
            String td = request.getParameter("td");

            int offset = (page - 1) * limit;

            StringBuilder where = new StringBuilder(" WHERE 1=1");
            List<Object> params2 = new ArrayList<Object>();
            if (un != null && !un.isEmpty()) {
                where.append(" AND username LIKE ?");
                params2.add("%" + un + "%");
            }
            if (fd != null && !fd.isEmpty()) {
                where.append(" AND create_date >= ?");
                params2.add(fd);
            }
            if (td != null && !td.isEmpty()) {
                where.append(" AND create_date <= ?");
                params2.add(td);
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                int total = 0;
                try (PreparedStatement countPs = conn.prepareStatement("SELECT COUNT(*) FROM log_loginadmin" + where.toString())) {
                    for (int i = 0; i < params2.size(); i++) {
                        countPs.setString(i + 1, params2.get(i).toString());
                    }
                    try (ResultSet countRs = countPs.executeQuery()) {
                        if (countRs.next()) total = countRs.getInt(1);
                    }
                }

                JSONArray list = new JSONArray();
                String sql = "SELECT * FROM log_loginadmin" + where.toString() + " ORDER BY id DESC LIMIT ?,?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    int idx = 1;
                    for (Object p : params2) {
                        ps.setString(idx++, p.toString());
                    }
                    ps.setInt(idx++, offset);
                    ps.setInt(idx, limit);
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
            logger.error("ListLoginLogsProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
