package com.vinplay.api.backend.processors.agent;

import com.vinplay.dal.dao.AgentDAO;
import com.vinplay.dal.dao.LogMoneyUserDao;
import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.dal.dao.impl.LogMoneyUserDaoImpl;
import com.vinplay.dal.entities.agent.UserAgentModel;
import com.vinplay.vbee.common.balancehistory.BalanceHistoryCategory;
import com.vinplay.vbee.common.balancehistory.BalanceHistoryCategoryMapper;
import com.vinplay.vbee.common.balancehistory.BalanceHistoryRow;
import com.vinplay.vbee.common.balancehistory.BalanceHistorySummary;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

/**
 * c=9974 — User Balance History (Agency, rc-gated).
 * Originally drafted as c=9973 but renumbered during rebase: staging took
 * 9973 for SearchLoDeBettingHistory4AgencyProcessor first.
 * Agency-facing twin of c=9972 UserBalanceHistoryProcessor.
 * Auth is via the agency's {@code rc} (referral code) parameter instead of an
 * admin {@code aat} token. Same query logic, same response shape.
 *
 * Spec: docs/superpowers/specs/2026-05-14-user-balance-history-api-design.md
 *
 * Error codes (per project convention):
 *   1001 user not in agent subtree (auth gate)
 *   1002 agent/user not found
 *   4001 missing required param    4002 invalid format       4003 invalid range
 *   4004 range exceeded            4005 invalid category     4006 identifier mismatch
 *   9999 internal
 */
public class UserBalanceHistoryAgencyProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 200;
    private static final int DEFAULT_LOOKBACK_DAYS = 7;
    private static final int MAX_RANGE_DAYS = 365;
    private static final int MAX_PAGE = 10_000;
    private static final TimeZone TZ_SEOUL = TimeZone.getTimeZone("Asia/Seoul");

    /**
     * Copied verbatim from DetailMemberOfAgencyProcessor.resolveRequesterAgent —
     * resolves an agency's referral code or nickname to a UserAgentModel.
     * Returns null if the rc does not map to any agent row.
     */
    private static UserAgentModel resolveRequesterAgent(AgentDAO agentDAO, String rc) throws Exception {
        if (rc == null || rc.trim().isEmpty()) {
            return null;
        }

        try (Connection adminConn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
            try (PreparedStatement stm = adminConn.prepareStatement(
                    "SELECT nickname FROM vinplay_admin.useragent WHERE code = ? OR nickname = ? LIMIT 1")) {
                stm.setString(1, rc);
                stm.setString(2, rc);
                try (ResultSet rs = stm.executeQuery()) {
                    if (rs.next()) {
                        return agentDAO.DetailUserAgentByNickName(rs.getString("nickname"));
                    }
                }
            }

            // Legacy fallback: map public referral code back to agent nickname via users.parrentUser.
            try (PreparedStatement stm = adminConn.prepareStatement(
                    "SELECT ua.nickname " +
                    "FROM vinplay.users u " +
                    "JOIN vinplay_admin.useragent ua ON ua.nickname COLLATE utf8mb3_unicode_ci = u.parrentUser COLLATE utf8mb3_unicode_ci " +
                    "WHERE u.referral_code = ? AND u.parrentUser IS NOT NULL AND u.parrentUser <> '' " +
                    "LIMIT 1")) {
                stm.setString(1, rc);
                try (ResultSet rs = stm.executeQuery()) {
                    if (rs.next()) {
                        return agentDAO.DetailUserAgentByNickName(rs.getString("nickname"));
                    }
                }
            }
        }

        return null;
    }

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        long startNs = System.nanoTime();
        JSONObject response = new JSONObject();
        try {
            // ── Agency auth via rc ─────────────────────────────────────
            String rc = trimToNull(request.getParameter("rc"));
            if (rc == null) {
                return errorResponse(response, "4001", "Thiếu tham số rc (referral code)");
            }
            AgentDAO agentDAO = new AgentDAOImpl();
            UserAgentModel requesterAgent;
            try {
                requesterAgent = resolveRequesterAgent(agentDAO, rc);
            } catch (Exception agentEx) {
                logger.warn("balance-history-agency: agent lookup failed rc=" + rc, agentEx);
                requesterAgent = null;
            }
            if (requesterAgent == null) {
                return errorResponse(response, "1002", "Agent not found for rc=" + rc);
            }

            String nn = trimToNull(request.getParameter("nn"));
            String uidParam = trimToNull(request.getParameter("uid"));
            if (nn == null && uidParam == null) {
                return errorResponse(response, "4001", "Thiếu nickname hoặc user_id");
            }

            // ── Date range — validated before user lookup ──────────────
            String from = trimToNull(request.getParameter("from"));
            String to   = trimToNull(request.getParameter("to"));
            SimpleDateFormat dayFmt = new SimpleDateFormat("yyyy-MM-dd");
            dayFmt.setTimeZone(TZ_SEOUL);
            Calendar cal = Calendar.getInstance(TZ_SEOUL);
            Date today = cal.getTime();
            if (to == null) to = dayFmt.format(today);
            if (from == null) {
                cal.add(Calendar.DAY_OF_MONTH, -DEFAULT_LOOKBACK_DAYS);
                from = dayFmt.format(cal.getTime());
            }
            Date fromDate, toDate;
            try {
                fromDate = dayFmt.parse(from);
                toDate   = dayFmt.parse(to);
            } catch (Exception e) {
                return errorResponse(response, "4002", "Định dạng ngày không hợp lệ (yyyy-MM-dd)");
            }
            if (fromDate.after(toDate)) {
                return errorResponse(response, "4003", "Khoảng thời gian không hợp lệ");
            }
            long rangeDays = (toDate.getTime() - fromDate.getTime()) / (1000L * 60 * 60 * 24);
            if (rangeDays > MAX_RANGE_DAYS) {
                return errorResponse(response, "4004", "Tối đa " + MAX_RANGE_DAYS + " ngày 1 lần truy vấn");
            }
            String fromTs = from + " 00:00:00";
            String toTs   = to   + " 23:59:59";

            // ── Category filter — validated before user lookup ─────────
            String catCsv = trimToNull(request.getParameter("category"));
            Set<BalanceHistoryCategory> categories = BalanceHistoryCategoryMapper.parseCsv(catCsv);
            if (categories == null) {
                return errorResponse(response, "4005", "Category không hợp lệ: " + catCsv);
            }

            LogMoneyUserDao dao = new LogMoneyUserDaoImpl();

            // ── Resolve target user ────────────────────────────────────
            UserModel target = null;
            try {
                if (nn != null) {
                    target = dao.getUserByNickName(nn);
                }
                if (target == null && uidParam != null) {
                    long uidLong;
                    try {
                        uidLong = Long.parseLong(uidParam);
                    } catch (NumberFormatException nfe) {
                        return errorResponse(response, "4002", "uid không hợp lệ (phải là số)");
                    }
                    target = dao.getUserById(uidLong);
                }
            } catch (Exception lookupEx) {
                logger.warn("balance-history-agency: user lookup failed nn=" + nn + " uid=" + uidParam, lookupEx);
            }
            if (target == null) {
                return errorResponse(response, "1002", "Không tìm thấy user");
            }
            String resolvedNick = target.getNickname();
            if (nn != null && uidParam != null && !nn.equals(resolvedNick)) {
                return errorResponse(response, "4006", "nickname và user_id không khớp");
            }

            // ── Subtree authorization (MR review C-1) ──────────────────
            //   The rc check above only proves the caller is some agent.
            //   Without this gate, agent_a could pass rc=agent_a&nn=<player under agent_b>
            //   and read another agent's player wallet history. Reuse the
            //   FIND_IN_SET ancestor walk + referral_code fallback from the
            //   c=9842 sibling (SearchGame3rdBettingHistory4AgencyProcessor).
            List<String> subtreeNicks = getSubtreePlayerNicknames(requesterAgent.getNickname(), null, false);
            if (!subtreeNicks.contains(resolvedNick)) {
                logger.warn("balance-history-agency: subtree gate rejected "
                        + "agent=" + requesterAgent.getNickname() + " target=" + resolvedNick);
                return errorResponse(response, "1001", "User không thuộc subtree của agent");
            }

            // ── Pagination ─────────────────────────────────────────────
            int page  = parseIntOr(request.getParameter("page"),  1);
            int limit = parseIntOr(request.getParameter("limit"), DEFAULT_LIMIT);
            if (page  < 1) page  = 1;
            if (page  > MAX_PAGE) page  = MAX_PAGE;
            if (limit < 1) limit = DEFAULT_LIMIT;
            if (limit > MAX_LIMIT) limit = MAX_LIMIT;

            // ── Query ──────────────────────────────────────────────────
            List<BalanceHistoryRow> rows = dao.queryBalanceHistory(
                    resolvedNick, fromTs, toTs, categories, page, limit);
            long total = dao.countBalanceHistory(
                    resolvedNick, fromTs, toTs, categories);
            // Summary aggregates across the full date range, ignoring the category
            // filter (same as c=9972, per spec line 100).
            BalanceHistorySummary summary = dao.summarizeBalanceHistory(
                    resolvedNick, fromTs, toTs);

            // ── Build response payload ─────────────────────────────────
            JSONArray list = new JSONArray();
            for (BalanceHistoryRow r : rows) {
                r.nickname = resolvedNick;
                list.put(r.toJson());
            }

            JSONObject pagination = new JSONObject();
            pagination.put("page", page);
            pagination.put("limit", limit);
            pagination.put("total", total);
            pagination.put("total_pages", limit == 0 ? 0 : (long) Math.ceil(total / (double) limit));

            JSONObject data = new JSONObject();
            data.put("list", list);
            data.put("pagination", pagination);
            data.put("summary", summary.toJson());

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("message", "");
            response.put("data", data);

            long durMs = (System.nanoTime() - startNs) / 1_000_000L;
            logger.info("balance-history-agency rc=" + rc + " agent=" + requesterAgent.getNickname()
                    + " user=" + resolvedNick
                    + " from=" + from + " to=" + to
                    + " cat=" + catCsv + " rows=" + rows.size()
                    + " total=" + total + " dur_ms=" + durMs);

            return response.toString();
        } catch (Exception e) {
            logger.error("balance-history-agency failed", e);
            return errorResponse(response, "9999", "Lỗi truy vấn dữ liệu");
        }
    }

    private static String errorResponse(JSONObject response, String code, String message) {
        response.put("success", false);
        response.put("errorCode", code);
        response.put("message", message);
        if (response.has("data")) {
            response.remove("data");
        }
        return response.toString();
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static int parseIntOr(String s, int def) {
        if (s == null) return def;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }

    /**
     * Returns every player nick_name that lives under {@code agentNick} in the
     * agency hierarchy. Used as the subtree authorization gate.
     *
     * Logic copied verbatim from
     * {@code SearchGame3rdBettingHistory4AgencyProcessor.getSubtreePlayerNicknames}
     * (c=9842) — battle-tested for the FIND_IN_SET ancestor walk plus the
     * legacy {@code referral_code} fallback for users that pre-date
     * {@code parent_agent_id}. The copy is intentional: per the design choice
     * for c=9974 we do not extract a shared base class in v1.
     *
     * @param hideBot when true, adds {@code AND is_bot = 0} so bot accounts
     *                are excluded from the membership set.
     */
    private List<String> getSubtreePlayerNicknames(String agentNick, String playerFilter, boolean hideBot) {
        List<String> nicks = new ArrayList<>();
        try (Connection adminConn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
            int agentId = -1;
            try (PreparedStatement ps = adminConn.prepareStatement("SELECT id FROM useragent WHERE nickname=?")) {
                ps.setString(1, agentNick);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) agentId = rs.getInt(1); }
            }
            if (agentId <= 0) return nicks;

            List<Integer> ids = new ArrayList<>();
            ids.add(agentId);
            try (PreparedStatement ps = adminConn.prepareStatement(
                    "SELECT id FROM useragent WHERE FIND_IN_SET(?, ancestors) > 0")) {
                ps.setInt(1, agentId);
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) ids.add(rs.getInt(1)); }
            }

            List<String> codes = new ArrayList<>();
            StringBuilder ph = new StringBuilder();
            for (int i = 0; i < ids.size(); i++) { if (i > 0) ph.append(","); ph.append("?"); }
            try (PreparedStatement ps = adminConn.prepareStatement(
                    "SELECT code FROM useragent WHERE id IN (" + ph + ") AND code IS NOT NULL AND code!=''")) {
                for (int i = 0; i < ids.size(); i++) ps.setInt(i + 1, ids.get(i));
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) codes.add(rs.getString(1)); }
            }

            try (PreparedStatement ps = adminConn.prepareStatement(
                    "SELECT old_code FROM agent_code_history WHERE agent_id IN (" + ph + ")")) {
                for (int i = 0; i < ids.size(); i++) ps.setInt(i + 1, ids.get(i));
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) codes.add(rs.getString(1)); }
            }

            StringBuilder idPh = new StringBuilder();
            for (int i = 0; i < ids.size(); i++) { if (i > 0) idPh.append(","); idPh.append("?"); }

            final String botCond = hideBot ? " AND is_bot = 0" : "";

            String sql = "SELECT nick_name FROM users WHERE " +
                    "(parent_agent_id IN (" + idPh + ")" + botCond;

            boolean hasCodes = !codes.isEmpty();
            if (hasCodes) {
                StringBuilder codePh = new StringBuilder();
                for (int i = 0; i < codes.size(); i++) { if (i > 0) codePh.append(","); codePh.append("?"); }
                sql += " OR referral_code COLLATE utf8mb4_general_ci IN (" + codePh + ")" + botCond;
            }
            sql += ")";

            if (playerFilter != null && !playerFilter.isEmpty()) sql += " AND nick_name LIKE ?";

            try (Connection userConn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = userConn.prepareStatement(sql)) {
                int idx = 1;
                for (int id : ids) ps.setInt(idx++, id);
                if (hasCodes) {
                    for (String c : codes) ps.setString(idx++, c);
                }
                if (playerFilter != null && !playerFilter.isEmpty()) ps.setString(idx, "%" + playerFilter + "%");
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) nicks.add(rs.getString(1)); }
            }
        } catch (Exception e) {
            logger.warn("balance-history-agency: getSubtreePlayerNicknames error", e);
        }
        return nicks;
    }
}
