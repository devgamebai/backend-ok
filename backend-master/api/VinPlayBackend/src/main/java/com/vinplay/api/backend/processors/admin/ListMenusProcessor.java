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

public class ListMenusProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                JSONArray list = new JSONArray();
                String sql = "SELECT * FROM menu ORDER BY Parrent_ID, Param, Name";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject item = new JSONObject();
                            item.put("id", rs.getInt("id"));
                            item.put("name", rs.getString("Name") != null ? rs.getString("Name") : "");
                            item.put("link", rs.getString("Link") != null ? rs.getString("Link") : "");
                            item.put("parentId", rs.getInt("Parrent_ID"));
                            item.put("param", rs.getInt("Param"));
                            item.put("isThuong", rs.getInt("isThuong"));
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
            logger.error("ListMenusProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
