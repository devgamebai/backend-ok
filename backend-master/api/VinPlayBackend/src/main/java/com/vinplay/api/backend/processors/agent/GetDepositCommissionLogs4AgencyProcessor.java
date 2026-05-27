package com.vinplay.api.backend.processors.agent;

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
import java.util.ArrayList;
import java.util.List;

/**
 * c=9846 — List lịch sử hoa hồng tiền nạp cho đại lý.
 *
 * Params:
 *   rc   = agent referral code (mã đại lý đang đăng nhập)
 *   nn   = agent nickname filter (optional - xem hoa hồng của cấp dưới cụ thể)
 *   ft   = from date (YYYY-MM-DD)
 *   et   = to date (YYYY-MM-DD)
 *   pg   = page (default 1)
 *   size = page size (default 20)
 *
 * Response:
 *   {success, data: [...], totals: {total_deposit, total_commission}, total, page, limit}
 */
public class GetDepositCommissionLogs4AgencyProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        JSONObject response = new JSONObject();

        try {
            String agentCode = request.getParameter("rc");
            String filterNickname = request.getParameter("nn"); // filter by agent nickname
            String fromDate = request.getParameter("ft");
            String toDate = request.getParameter("et");

            int page = 1, limit = 20;
            try { if (request.getParameter("pg") != null) page = Integer.parseInt(request.getParameter("pg")); } catch (Exception e) {}
            try { if (request.getParameter("size") != null) limit = Integer.parseInt(request.getParameter("size")); } catch (Exception e) {}
            if (page < 1) page = 1;
            if (limit < 1 || limit > 200) limit = 20;
            int offset = (page - 1) * limit;

            if (agentCode == null || agentCode.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1002");
                response.put("message", "Missing agent code rc");
                return response.toString();
            }

            // Lấy agent id & info từ code
            int agentId = -1;
            String agentNickname = "";
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, nickname FROM vinplay_admin.useragent WHERE code = ? LIMIT 1");
                ps.setString(1, agentCode);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    agentId = rs.getInt("id");
                    agentNickname = rs.getString("nickname");
                }
                rs.close(); ps.close();
            }

            if (agentId == -1) {
                response.put("success", false);
                response.put("errorCode", "1002");
                response.put("message", "Agent not found");
                return response.toString();
            }

            // Xây dựng WHERE clause dùng PreparedStatement params (tránh SQL injection)
            // Agent chỉ thấy hoa hồng của chính mình và các tuyến dưới
            StringBuilder where = new StringBuilder(
                "WHERE agent_id IN (SELECT id FROM vinplay_admin.useragent WHERE id = ? OR FIND_IN_SET(?, ancestors) > 0)");

            List<Object> params = new ArrayList<>();
            params.add(agentId);  // id = ?
            params.add(agentId);  // FIND_IN_SET(?, ancestors)

            // Optional: filter by specific agent nickname
            if (filterNickname != null && !filterNickname.isEmpty()) {
                where.append(" AND agent_nickname LIKE ?");
                params.add("%" + filterNickname + "%");
            }
            // BUG FIX: dùng PreparedStatement binding thay vì string concatenation
            if (fromDate != null && !fromDate.isEmpty()) {
                where.append(" AND created_at >= ?");
                params.add(fromDate + " 00:00:00");
            }
            if (toDate != null && !toDate.isEmpty()) {
                where.append(" AND created_at <= ?");
                params.add(toDate + " 23:59:59");
            }

            JSONArray arr = new JSONArray();
            long totalRecords = 0;
            long sumDeposit = 0, sumCommission = 0;

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                // Count + Totals
                PreparedStatement cntPs = conn.prepareStatement(
                    "SELECT COUNT(*), SUM(deposit_amount), SUM(commission_amount) " +
                    "FROM vinplay_admin.deposit_commission_logs " + where);
                // Bind all params
                for (int i = 0; i < params.size(); i++) {
                    Object p = params.get(i);
                    if (p instanceof Integer) cntPs.setInt(i + 1, (Integer) p);
                    else cntPs.setString(i + 1, String.valueOf(p));
                }
                ResultSet cntRs = cntPs.executeQuery();
                if (cntRs.next()) {
                    totalRecords = cntRs.getLong(1);
                    sumDeposit = cntRs.getLong(2);
                    sumCommission = cntRs.getLong(3);
                }
                cntRs.close(); cntPs.close();

                // Data
                String dataSql = "SELECT id, agent_id, agent_nickname, agent_level, " +
                    "source_user_id, source_nickname, transaction_id, deposit_amount, " +
                    "own_rate, child_rate, diff_rate, commission_amount, status, created_at " +
                    "FROM vinplay_admin.deposit_commission_logs " + where +
                    " ORDER BY created_at DESC LIMIT " + limit + " OFFSET " + offset;

                PreparedStatement dataPs = conn.prepareStatement(dataSql);
                for (int i = 0; i < params.size(); i++) {
                    Object p = params.get(i);
                    if (p instanceof Integer) dataPs.setInt(i + 1, (Integer) p);
                    else dataPs.setString(i + 1, String.valueOf(p));
                }
                ResultSet rs = dataPs.executeQuery();
                while (rs.next()) {
                    JSONObject row = new JSONObject();
                    row.put("id", rs.getLong("id"));
                    row.put("agent_id", rs.getInt("agent_id"));
                    row.put("agent_nickname", rs.getString("agent_nickname"));
                    row.put("agent_level", rs.getInt("agent_level"));
                    row.put("source_nickname", rs.getString("source_nickname"));
                    row.put("transaction_id", rs.getString("transaction_id"));
                    row.put("deposit_amount", rs.getLong("deposit_amount"));
                    // SUN-1098: emit rate columns as 2-decimal strings.
                    row.put("own_rate",   com.vinplay.dal.utils.PctFormatter.formatRs(rs, "own_rate"));
                    row.put("child_rate", com.vinplay.dal.utils.PctFormatter.formatRs(rs, "child_rate"));
                    row.put("diff_rate",  com.vinplay.dal.utils.PctFormatter.formatRs(rs, "diff_rate"));
                    row.put("commission_amount", rs.getLong("commission_amount"));
                    row.put("status", rs.getString("status"));
                    row.put("created_at", rs.getString("created_at"));
                    arr.put(row);
                }
                rs.close(); dataPs.close();
            }

            JSONObject totals = new JSONObject();
            totals.put("total_deposit", sumDeposit);
            totals.put("total_commission", sumCommission);

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", arr);
            response.put("totals", totals);
            response.put("total", totalRecords);
            response.put("page", page);
            response.put("limit", limit);
            response.put("agent_id", agentId);
            response.put("agent_nickname", agentNickname);

        } catch (Exception e) {
            logger.error("GetDepositCommissionLogs4AgencyProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
            response.put("message", "Internal error: " + e.getMessage());
        }
        return response.toString();
    }
}
