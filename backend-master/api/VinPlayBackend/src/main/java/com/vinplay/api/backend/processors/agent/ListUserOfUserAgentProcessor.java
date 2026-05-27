package com.vinplay.api.backend.processors.agent;

import bitzero.util.common.business.Debug;
import com.vinplay.dal.dao.AgentDAO;
import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.dal.entities.agent.DetailUserModel;
import com.vinplay.dal.entities.agent.UserAgentModel;
import com.vinplay.dal.entities.agent.UserOfAgentModel;
import com.vinplay.payment.utils.Constant;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import org.apache.http.util.TextUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListUserOfUserAgentProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String serPath = request.getServletPath();

        String nickname = request.getParameter("nn");
        String fromTime = request.getParameter("ft");
        String endTime = request.getParameter("et");
        String code = request.getParameter("cd");
        int page, maxItem;
        AgentDAO dao = new AgentDAOImpl();
        // SUN-CR: search includes parrentUser (referrer name)
        // When nickname is provided, first try to match it as an agent nickname.
        // If matched → use agent's code to filter users of that agent.
        // If NOT matched AND code is empty → pass nickname through for LIKE search
        // on nick_name OR parrentUser in the DAO.
        boolean searchByParrentUser = false;
        if(!TextUtils.isEmpty(nickname)){
            try {
                UserAgentModel agent = dao.DetailUserAgentByNickName(nickname);
                if(agent != null) {
                    code = agent.getCode();
                    nickname = "";
                } else if (code == null || code.isEmpty()) {
                    // nickname doesn't match any agent and no code filter
                    // → search users by nick_name OR parrentUser LIKE
                    searchByParrentUser = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if((code==null || code.isEmpty()) && !searchByParrentUser){
            return BaseResponse.error("-1", "nhập thiếu mã code");
        }

        if(!(fromTime==null || fromTime.isEmpty())){
            try {
                Date date = new SimpleDateFormat("yyyy-MM-dd").parse(fromTime);
            } catch (ParseException e) {
                return BaseResponse.error("-1", "ngày bắt đầu sai định dạng yyyy-MM-dd hh:mm:ss");
            }
        }

        if(!(endTime==null || endTime.isEmpty())){
            try {
                Date date = new SimpleDateFormat("yyyy-MM-dd").parse(endTime);
            } catch (ParseException e) {
                return BaseResponse.error("-1", "ngày kết thúc sai định dạng yyyy-MM-dd hh:mm:ss");
            }
        }

        try {
            page = Integer.parseInt(request.getParameter("pg"));
        } catch (NumberFormatException e) {
            page = 1;
        }

        try {
            maxItem = Integer.parseInt(request.getParameter("mi"));
        } catch (NumberFormatException e) {
            maxItem = 10;
        }


        try {
            // SUN-CR: search by nick_name OR parrentUser when no agent code filter
            if (searchByParrentUser) {
                return searchByNickNameOrParrentUser(nickname, fromTime, endTime, page, maxItem);
            }

//            Long totalRecord = dao.countUserOfUserAgent(code, nickname, fromTime, endTime);
//            List<DetailUserModel> users = dao.listUserOfUserAgent(code, nickname, fromTime, endTime, page, maxItem);
//        	  return BaseResponse.success(users, totalRecord);

            List<Map<String, Object>> data = new ArrayList<>();
            if(code.equals("referral_code_default")) {
            	code = "";
            }
            
            data = dao.reportUserPlay4Agent(code, nickname, fromTime, endTime, page, maxItem);
            
            if (data == null || data.size() < 2) {
                 Map<String, Object> emptyMap = new HashMap<>();
                 emptyMap.put("total_nap", 0L);
                 emptyMap.put("total_rut", 0L);
                 emptyMap.put("total_doanhthu", 0L);
                 emptyMap.put("total_km", 0L);
                 emptyMap.put("total_net_loss", 0L);
                 emptyMap.put("listData", new ArrayList<>());
                 return BaseResponse.success(emptyMap, 0);
            }
            
            int dataSize = data.size();
            Map<String, Object> countMap = data.get(dataSize - 1);
            int totalRecord = 0;
            if (countMap.containsKey("total")) {
                totalRecord = Integer.parseInt(countMap.get("total").toString());
            }
            data.remove(dataSize - 1);
            
            dataSize = data.size();
            Map<String, Object> totalMap = data.get(dataSize - 1);
            long totalNap = totalMap.containsKey("total_nap") ? Long.parseLong(totalMap.get("total_nap").toString()) : 0L;
            long totalRut = totalMap.containsKey("total_rut") ? Long.parseLong(totalMap.get("total_rut").toString()) : 0L;
            long totalKM = totalMap.containsKey("total_km") ? Long.parseLong(totalMap.get("total_km").toString()) : 0L;
            long totalVin = totalMap.containsKey("total_vin") ? Long.parseLong(totalMap.get("total_vin").toString()) : 0L;
            data.remove(dataSize - 1);

            Map<String, Object> map = new HashMap<>();
            map.put("total_nap", totalNap);
            map.put("total_rut", totalRut);
            map.put("total_doanhthu", totalNap - totalRut - totalKM);
            map.put("total_km", totalKM);
            map.put("total_net_loss", (totalRut + totalVin) - (totalNap + totalKM));
            map.put("listData", data);
            return BaseResponse.success(map, totalRecord);
        }
        catch (Exception e) {
            return BaseResponse.error("-1", e.getMessage());
        }
    }

    /**
     * SUN-CR: Search users by nick_name OR parrentUser LIKE pattern.
     * Used when no agent code is specified and the search term doesn't match any agent.
     */
    private String searchByNickNameOrParrentUser(String keyword, String fromTime, String endTime, int page, int maxItem) {
        try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            int offset = (page < 1 ? 0 : page - 1) * maxItem;
            String likePattern = "%" + keyword + "%";

            StringBuilder where = new StringBuilder(" WHERE is_bot = false AND (nick_name LIKE ? OR parrentUser LIKE ?)");
            List<Object> params = new ArrayList<>();
            params.add(likePattern);
            params.add(likePattern);

            if (fromTime != null && !fromTime.isEmpty()) {
                where.append(" AND create_time >= ?");
                params.add(fromTime + " 00:00:00");
            }
            if (endTime != null && !endTime.isEmpty()) {
                where.append(" AND create_time <= ?");
                params.add(endTime + " 23:59:59");
            }

            // Count
            int totalRecord = 0;
            String countSql = "SELECT COUNT(*) AS cnt FROM users" + where;
            try (java.sql.PreparedStatement ps = conn.prepareStatement(countSql)) {
                int idx = 1;
                for (Object p : params) ps.setString(idx++, String.valueOf(p));
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) totalRecord = rs.getInt("cnt");
                }
            }

            // Data
            String dataSql = "SELECT nick_name, vin, IFNULL(t_nap,0) AS t_nap, IFNULL(t_rut,0) AS t_rut, " +
                    "create_time, parrentUser FROM users" + where +
                    " ORDER BY id DESC LIMIT ? OFFSET ?";
            List<Map<String, Object>> rows = new ArrayList<>();
            try (java.sql.PreparedStatement ps = conn.prepareStatement(dataSql)) {
                int idx = 1;
                for (Object p : params) ps.setString(idx++, String.valueOf(p));
                ps.setInt(idx++, maxItem);
                ps.setInt(idx, offset);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        long tNap = rs.getLong("t_nap");
                        long tRut = rs.getLong("t_rut");
                        long vin = rs.getLong("vin");
                        row.put("nick_name", rs.getString("nick_name"));
                        row.put("vin", vin);
                        row.put("t_nap", tNap);
                        row.put("t_rut", tRut);
                        row.put("t_km", 0L);
                        row.put("create_time", rs.getString("create_time"));
                        row.put("net_loss", (tRut + vin) - tNap);
                        row.put("parrentUser", rs.getString("parrentUser") != null ? rs.getString("parrentUser") : "");
                        rows.add(row);
                    }
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("total_nap", 0L);
            result.put("total_rut", 0L);
            result.put("total_doanhthu", 0L);
            result.put("total_km", 0L);
            result.put("total_net_loss", 0L);
            result.put("listData", rows);
            return BaseResponse.success(result, totalRecord);
        } catch (Exception e) {
            logger.error("searchByNickNameOrParrentUser error", e);
            return BaseResponse.error("-1", e.getMessage());
        }
    }
}