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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserGameListProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    private static final Set<String> ALLOWED_SORT_COLS = new HashSet<String>(Arrays.asList(
        "id", "user_name", "nick_name", "vin", "xu", "safe", "vip_point", "create_time"
    ));

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
            String nn = request.getParameter("nn");
            String mobile = request.getParameter("m");
            String sort = request.getParameter("sort");
            String order = request.getParameter("order");

            int offset = (page - 1) * limit;

            StringBuilder where = new StringBuilder(" WHERE 1=1");
            List<String> params2 = new ArrayList<String>();
            if (un != null && !un.isEmpty()) {
                where.append(" AND user_name LIKE ?");
                params2.add("%" + un + "%");
            }
            if (nn != null && !nn.isEmpty()) {
                where.append(" AND nick_name LIKE ?");
                params2.add("%" + nn + "%");
            }
            if (mobile != null && !mobile.isEmpty()) {
                where.append(" AND mobile LIKE ?");
                params2.add("%" + mobile + "%");
            }

            // Validate sort column
            String orderBy = " ORDER BY id DESC";
            if (sort != null && ALLOWED_SORT_COLS.contains(sort)) {
                String dir = "ASC";
                if (order != null && order.equalsIgnoreCase("desc")) {
                    dir = "DESC";
                }
                orderBy = " ORDER BY " + sort + " " + dir;
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                int total = 0;
                try (PreparedStatement countPs = conn.prepareStatement("SELECT COUNT(*) FROM users" + where.toString())) {
                    for (int i = 0; i < params2.size(); i++) {
                        countPs.setString(i + 1, params2.get(i));
                    }
                    try (ResultSet countRs = countPs.executeQuery()) {
                        if (countRs.next()) total = countRs.getInt(1);
                    }
                }

                JSONArray list = new JSONArray();
                String sql = "SELECT * FROM users" + where.toString() + orderBy + " LIMIT ?,?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    int idx = 1;
                    for (String p : params2) {
                        ps.setString(idx++, p);
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
            logger.error("UserGameListProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
