package com.vinplay.api.backend.processors.user;

import com.vinplay.api.backend.services.AgentHierarchyHelper;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ListUsersProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            if (AdminUserSupport.requireAdmin(request, response) == null) {
                return response.toString();
            }

            int page = Math.max(1, parseInt(request.getParameter("page"), 1));
            int limit = Math.min(100, Math.max(1, parseInt(request.getParameter("limit"), 20)));
            int offset = (page - 1) * limit;

            String q = AdminUserSupport.nullableTrim(request.getParameter("q"));
            String userName = AdminUserSupport.nullableTrim(request.getParameter("un"));
            String nickName = AdminUserSupport.nullableTrim(request.getParameter("nn"));
            String mobile = AdminUserSupport.nullableTrim(request.getParameter("mobile"));
            String email = AdminUserSupport.nullableTrim(request.getParameter("email"));
            String referralCode = AdminUserSupport.nullableTrim(request.getParameter("referral_code"));
            String parentAgentId = AdminUserSupport.nullableTrim(request.getParameter("parent_agent_id"));
            String isBot = AdminUserSupport.nullableTrim(request.getParameter("is_bot"));
            String status = AdminUserSupport.nullableTrim(request.getParameter("status"));

            // hide_bot: ẩn user bot khỏi danh sách. Mặc định true (ẩn bot).
            // Truyền hide_bot=0 hoặc hide_bot=false để xem cả bot.
            // Nếu is_bot được truyền tường minh thì hide_bot bị bỏ qua.
            String hideBotParam = AdminUserSupport.nullableTrim(request.getParameter("hide_bot"));
            final boolean hideBot = AdminUserSupport.isNotBlank(isBot)
                    ? false  // is_bot explicit → caller controls filter directly
                    : (hideBotParam == null || !("0".equals(hideBotParam) || "false".equalsIgnoreCase(hideBotParam)));

            String validationError = AdminUserSupport.validateInteger(parentAgentId, "parent_agent_id");
            if (validationError != null) {
                return error(response, "4002", validationError);
            }
            validationError = AdminUserSupport.validateTinyIntFlag(isBot, "is_bot");
            if (validationError != null) {
                return error(response, "4002", validationError);
            }
            validationError = AdminUserSupport.validateInteger(status, "status");
            if (validationError != null) {
                return error(response, "4002", validationError);
            }

            String sortBy = resolveSortBy(request.getParameter("sort_by"));
            String sortDir = "asc".equalsIgnoreCase(request.getParameter("sort_dir")) ? "ASC" : "DESC";

            StringBuilder where = new StringBuilder(" WHERE 1=1");
            List<Object> params = new ArrayList<Object>();

            if (AdminUserSupport.isNotBlank(q)) {
                // Standard LIKE search across personal fields
                String pattern = "%" + q + "%";
                where.append(" AND (u.user_name LIKE ? OR u.nick_name LIKE ? OR u.mobile LIKE ? OR u.email LIKE ? OR u.parrentUser LIKE ?");
                params.add(pattern);
                params.add(pattern);
                params.add(pattern);
                params.add(pattern);
                params.add(pattern);

                // SUN-XXXX: If q also matches an agent (nickname / code / username),
                // expand the search to include ALL downline users via parent_agent_id.
                // This fixes CompanyAgent returning only itself while SpecialAccount
                // (the root) returned all users — the flat parrentUser LIKE was the culprit.
                AgentHierarchyHelper.AgentInfo agentMatch = AgentHierarchyHelper.resolveAgent(q);
                if (agentMatch != null) {
                    List<Integer> subtreeIds = AgentHierarchyHelper.getSubtreeAgentIdsIncludingSelf(agentMatch.id);
                    if (!subtreeIds.isEmpty()) {
                        where.append(" OR u.parent_agent_id IN (")
                             .append(AgentHierarchyHelper.inPlaceholders(subtreeIds.size()))
                             .append(")");
                        for (Integer sid : subtreeIds) {
                            params.add(sid);
                        }
                    }
                }
                where.append(")");  // close the AND (...) group
            }
            if (AdminUserSupport.isNotBlank(userName)) {
                where.append(" AND u.user_name = ?");
                params.add(userName);
            }
            if (AdminUserSupport.isNotBlank(nickName)) {
                where.append(" AND u.nick_name = ?");
                params.add(nickName);
            }
            if (AdminUserSupport.isNotBlank(mobile)) {
                where.append(" AND u.mobile = ?");
                params.add(mobile);
            }
            if (AdminUserSupport.isNotBlank(email)) {
                where.append(" AND u.email = ?");
                params.add(email);
            }
            if (AdminUserSupport.isNotBlank(referralCode)) {
                where.append(" AND u.referral_code = ?");
                params.add(referralCode);
            }
            if (AdminUserSupport.isNotBlank(parentAgentId)) {
                where.append(" AND u.parent_agent_id = ?");
                params.add(AdminUserSupport.parseOptionalInteger(parentAgentId));
            }
            if (AdminUserSupport.isNotBlank(isBot)) {
                where.append(" AND u.is_bot = ?");
                params.add(AdminUserSupport.parseOptionalInteger(isBot));
            } else if (hideBot) {
                where.append(" AND u.is_bot = 0");
            }
            if (AdminUserSupport.isNotBlank(status)) {
                where.append(" AND u.status = ?");
                params.add(AdminUserSupport.parseOptionalInteger(status));
            }

            try (Connection conn = AdminUserSupport.getUserConnection()) {
                int totalRecords = 0;
                JSONObject summary = new JSONObject();
                String countSql = "SELECT COUNT(1) AS cnt, "
                        + "COALESCE(SUM(u.vin), 0) AS sum_vin, "
                        + "COALESCE(SUM(u.t_nap), 0) AS sum_t_nap, "
                        + "COALESCE(SUM(u.t_rut), 0) AS sum_t_rut, "
                        + "COALESCE(SUM(u.t_nap - u.t_rut), 0) AS sum_profit, "
                        + "COALESCE(SUM(aw.balance), 0) AS sum_aw, "
                        + "COALESCE(SUM(cw.balance), 0) AS sum_cw, "
                        // u.safe column was removed from the users schema. The safe-vault
                        // wallet is no longer tracked at the user-row level — sum_total now
                        // = vin + agency_wallet + credit_wallet only.
                        + "COALESCE(SUM(u.vin + COALESCE(aw.balance, 0) + COALESCE(cw.balance, 0)), 0) AS sum_total "
                        + "FROM users u "
                        + // dai_ly>0 covers all agent levels (1=L1, 2=L2, 3=L3 etc). Using `=1` here was the
// reason credit_wallet_balance and agency_wallet_balance always showed 0 for level-2/3
// agents (Bahai88 incident, 2026-05-11). useragent has rows for every agent tier; only
// players have dai_ly=0 — for those there's no useragent row anyway, so the LEFT JOIN
// just yields NULLs and COALESCE returns 0 (correct).
"LEFT JOIN vinplay_admin.useragent ua_self ON u.dai_ly > 0 AND ua_self.nickname COLLATE utf8mb4_general_ci = u.nick_name "
                        + "LEFT JOIN vinplay.agency_wallet aw ON aw.agent_id = ua_self.id "
                        + "LEFT JOIN vinplay.credit_wallet cw ON cw.agent_id = ua_self.id "
                        + where;
                try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                    bind(ps, params);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            totalRecords = rs.getInt("cnt");
                            summary.put("sum_vin", rs.getLong("sum_vin"));
                            summary.put("sum_t_nap", rs.getLong("sum_t_nap"));
                            summary.put("sum_t_rut", rs.getLong("sum_t_rut"));
                            summary.put("sum_profit", rs.getLong("sum_profit"));
                            summary.put("sum_agency_wallet", rs.getLong("sum_aw"));
                            summary.put("sum_credit_wallet", rs.getLong("sum_cw"));
                            summary.put("sum_total_wallet", rs.getLong("sum_total"));
                        }
                    }
                }
                response.put("summary", summary);

                String dataSql = "SELECT u.id, u.user_name, u.nick_name, u.email, u.mobile, u.address, u.identification, "
                        // Schema cleanup: xu/vin_total/xu_total/safe/recharge_money/vip_point/
                        // vip_point_save no longer columns on users. Returned as 0 so the FE
                        // contract stays stable (these were already deprecated client-side).
                        + "u.vin, 0 AS xu, 0 AS vin_total, 0 AS xu_total, 0 AS safe, 0 AS recharge_money, 0 AS vip_point, 0 AS vip_point_save, "
                        + "u.dai_ly, u.status, u.is_bot, u.is_verify_mobile, 0 AS is_live, u.referral_code, u.parent_agent_id, "
                        + "u.parrentUser, u.t_nap, u.t_rut, u.nap_times, u.rut_times, u.create_time, u.last_login, "
                        + "ua.nickname AS parent_agent_nickname, ua.code AS parent_agent_code, "
                        + "COALESCE(aw.balance, 0) AS agency_wallet_balance, "
                        + "COALESCE(cw.balance, 0) AS credit_wallet_balance "
                        + "FROM users u "
                        + "LEFT JOIN vinplay_admin.useragent ua ON ua.id = u.parent_agent_id "
                        + // dai_ly>0 covers all agent levels (1=L1, 2=L2, 3=L3 etc). Using `=1` here was the
// reason credit_wallet_balance and agency_wallet_balance always showed 0 for level-2/3
// agents (Bahai88 incident, 2026-05-11). useragent has rows for every agent tier; only
// players have dai_ly=0 — for those there's no useragent row anyway, so the LEFT JOIN
// just yields NULLs and COALESCE returns 0 (correct).
"LEFT JOIN vinplay_admin.useragent ua_self ON u.dai_ly > 0 AND ua_self.nickname COLLATE utf8mb4_general_ci = u.nick_name "
                        + "LEFT JOIN vinplay.agency_wallet aw ON aw.agent_id = ua_self.id "
                        + "LEFT JOIN vinplay.credit_wallet cw ON cw.agent_id = ua_self.id "
                        + where
                        + " ORDER BY " + sortBy + " " + sortDir + " LIMIT ? OFFSET ?";

                JSONArray data = new JSONArray();
                try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                    int idx = bind(ps, params);
                    ps.setInt(idx++, limit);
                    ps.setInt(idx, offset);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject item = new JSONObject();
                            item.put("id", rs.getLong("id"));
                            item.put("user_name", safe(rs.getString("user_name")));
                            item.put("nick_name", safe(rs.getString("nick_name")));
                            item.put("email", safe(rs.getString("email")));
                            item.put("mobile", safe(rs.getString("mobile")));
                            item.put("address", safe(rs.getString("address")));
                            item.put("identification", safe(rs.getString("identification")));
                            item.put("vin", rs.getLong("vin"));
                            item.put("xu", rs.getLong("xu"));
                            item.put("vin_total", rs.getLong("vin_total"));
                            item.put("xu_total", rs.getLong("xu_total"));
                            item.put("safe", rs.getLong("safe"));
                            item.put("recharge_money", rs.getLong("recharge_money"));
                            item.put("vip_point", rs.getInt("vip_point"));
                            item.put("vip_point_save", rs.getInt("vip_point_save"));
                            item.put("dai_ly", rs.getInt("dai_ly"));
                            item.put("status", rs.getInt("status"));
                            item.put("is_bot", rs.getInt("is_bot"));
                            item.put("is_verify_mobile", rs.getObject("is_verify_mobile") == null ? 0 : rs.getInt("is_verify_mobile"));
                            item.put("is_live", rs.getObject("is_live") == null ? 0 : rs.getInt("is_live"));
                            item.put("referral_code", safe(rs.getString("referral_code")));
                            item.put("parent_agent_id", rs.getObject("parent_agent_id") == null ? 0 : rs.getInt("parent_agent_id"));
                            item.put("parent_agent_nickname", safe(rs.getString("parent_agent_nickname")));
                            item.put("parent_agent_code", safe(rs.getString("parent_agent_code")));
                            item.put("parrentUser", safe(rs.getString("parrentUser")));
                            item.put("t_nap", rs.getLong("t_nap"));
                            item.put("t_rut", rs.getLong("t_rut"));
                            item.put("profit", rs.getLong("t_nap") - rs.getLong("t_rut"));
                            item.put("nap_times", rs.getInt("nap_times"));
                            item.put("rut_times", rs.getInt("rut_times"));
                            item.put("create_time", safe(rs.getString("create_time")));
                            item.put("last_login", safe(rs.getString("last_login")));
                            item.put("agency_wallet_balance", rs.getLong("agency_wallet_balance"));
                            item.put("credit_wallet_balance", rs.getLong("credit_wallet_balance"));
                            
                            long totalWallet = rs.getLong("vin") + rs.getLong("agency_wallet_balance") + rs.getLong("credit_wallet_balance");
                            item.put("total_wallet_balance", totalWallet);
                            
                            data.put(item);
                        }
                    }
                }

                response.put("data", data);
                response.put("success", true);
                response.put("errorCode", "0");
                response.put("page", page);
                response.put("limit", limit);
                response.put("totalRecords", totalRecords);
                response.put("totalPages", totalRecords == 0 ? 0 : (int) Math.ceil((double) totalRecords / limit));
                response.put("hide_bot", hideBot);
            }
        } catch (Exception e) {
            logger.error("Error in ListUsersProcessor", e);
            return error(response, "9999", e.getMessage());
        }
        return response.toString();
    }

    private static int bind(PreparedStatement ps, List<Object> params) throws Exception {
        int idx = 1;
        for (Object param : params) {
            if (param instanceof Integer) {
                ps.setInt(idx++, (Integer) param);
            } else if (param instanceof Long) {
                ps.setLong(idx++, (Long) param);
            } else {
                ps.setString(idx++, String.valueOf(param));
            }
        }
        return idx;
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            return value == null ? defaultValue : Integer.parseInt(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String resolveSortBy(String sortBy) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return "u.id";
        }
        if ("user_name".equalsIgnoreCase(sortBy)) return "u.user_name";
        if ("nick_name".equalsIgnoreCase(sortBy)) return "u.nick_name";
        if ("vin".equalsIgnoreCase(sortBy)) return "u.vin";
        // vin_total / recharge_money columns dropped from users schema — fall back to vin.
        if ("vin_total".equalsIgnoreCase(sortBy)) return "u.vin";
        if ("recharge_money".equalsIgnoreCase(sortBy)) return "u.t_nap";
        if ("last_login".equalsIgnoreCase(sortBy)) return "u.last_login";
        if ("create_time".equalsIgnoreCase(sortBy)) return "u.create_time";
        if ("agency_wallet".equalsIgnoreCase(sortBy)) return "agency_wallet_balance";
        if ("credit_wallet".equalsIgnoreCase(sortBy)) return "credit_wallet_balance";
        if ("total_wallet".equalsIgnoreCase(sortBy)) return "(u.vin + COALESCE(aw.balance, 0) + COALESCE(cw.balance, 0))";
        return "u.id";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private String error(JSONObject response, String code, String message) {
        response.put("success", false);
        response.put("errorCode", code);
        response.put("message", message);
        return response.toString();
    }
}
