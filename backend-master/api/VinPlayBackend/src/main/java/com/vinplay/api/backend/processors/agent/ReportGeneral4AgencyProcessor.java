package com.vinplay.api.backend.processors.agent;

import com.mongodb.client.MongoDatabase;
import com.vinplay.api.backend.response.ReportLogUserResponse;
import com.vinplay.api.backend.services.AgentHierarchyHelper;
import com.vinplay.api.backend.services.AgentHierarchyHelper.AgentInfo;
import com.vinplay.payment.utils.Constant;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.response.BaseResponse;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.bson.Document;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import java.util.Map;
import java.util.HashMap;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReportGeneral4AgencyProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        ReportLogUserResponse res = new ReportLogUserResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest) param.get();
        String serPath = request.getServletPath();

        boolean isAgentCall = "/api_agent".equals(serPath);
        boolean isAdminCall = "/api_backend".equals(serPath);
        if (!isAgentCall && !isAdminCall) {
            return BaseResponse.error(Constant.ERROR_PARAM, "Not allow access this api");
        }

        String requestedRc = request.getParameter("rc");
        boolean siteWide = isAdminCall && (requestedRc == null || requestedRc.trim().isEmpty());

        if (!siteWide && (requestedRc == null || requestedRc.trim().isEmpty())) {
            return BaseResponse.error(Constant.ERROR_PARAM, "Agency code not empty");
        }

        String timeParam = request.getParameter("t");
        String fromParam = firstNonEmpty(request.getParameter("startDate"), request.getParameter("ft"));
        String endParam = firstNonEmpty(request.getParameter("endDate"), request.getParameter("et"));
        String segment = request.getParameter("segment");

        String defaultDate = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
        String fromDate = normalizeDateOnly(fromParam);
        String endDate = normalizeDateOnly(endParam);
        if ((fromDate == null || fromDate.isEmpty()) && (endDate == null || endDate.isEmpty())) {
            String pointInTimeDate = normalizeDateOnly(timeParam);
            fromDate = pointInTimeDate != null ? pointInTimeDate : defaultDate;
            endDate = fromDate;
        } else {
            if (fromDate == null || fromDate.isEmpty()) fromDate = endDate;
            if (endDate == null || endDate.isEmpty()) endDate = fromDate;
        }
        String normalizedSegment = "date";
        if (segment != null && !segment.trim().isEmpty()) {
            normalizedSegment = segment.trim().toLowerCase();
            if (!"date".equals(normalizedSegment) && !"hour".equals(normalizedSegment)) normalizedSegment = "date";
        }

        try {
            if (siteWide) {
                return executeSiteWide(res, fromDate, endDate, normalizedSegment);
            }

            AgentInfo agent = AgentHierarchyHelper.resolveAgent(requestedRc);
            if (agent == null) {
                return BaseResponse.error(Constant.ERROR_PARAM, "Agency not found");
            }
            boolean isMaster = AgentHierarchyHelper.isSiteMaster(agent);
            // SUN-1297 guard removed 2026-05-13: agency UI authenticates via
            // agent session cookie, not admin token. Master access is still
            // hierarchy-checked through resolveAgent + subtree expansion.
            List<Integer> subtreeIds = isMaster
                    ? new ArrayList<Integer>()
                    : AgentHierarchyHelper.getSubtreeAgentIdsIncludingSelf(agent.id);
            // SUN-1108 Wave 2 Tier 4: response cache. Aggregates 26 game cols
            // over a date range — expensive enough that the same dashboard
            // re-render benefits from a 30s/300s cached payload.
            final String cacheKey = com.vinplay.vbee.common.cache.ResponseCacheHelper.key(
                    "ReportGeneral4Agency", agent.id, fromDate, endDate, normalizedSegment);
            String cachedResp = com.vinplay.vbee.common.cache.ResponseCacheHelper.get(cacheKey);
            if (cachedResp != null) {
                return cachedResp;
            }
            final boolean histOnly = com.vinplay.vbee.common.cache.ResponseCacheHelper.isHistoricalOnly(endDate);

            Map<String, Object> obj = new HashMap<>();

            // Core money summary follows the Admin finance formula:
            // bank + crypto + admin credit-wallet movements.
            long sumDep = getSumDeposit(subtreeIds, fromDate, endDate, isMaster);
            long sumWith = getSumWithdraw(subtreeIds, fromDate, endDate, isMaster);
            
            // Calculate previous period for Delta
            long prevSumDep = 0;
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                Date fDate = sdf.parse(fromDate);
                Date eDate = sdf.parse(endDate);
                long diffDays = java.util.concurrent.TimeUnit.DAYS.convert(Math.abs(eDate.getTime() - fDate.getTime()), java.util.concurrent.TimeUnit.MILLISECONDS) + 1;
                Calendar cal = Calendar.getInstance();
                cal.setTime(fDate);
                cal.add(Calendar.DAY_OF_MONTH, (int) -diffDays);
                String prevFromDate = sdf.format(cal.getTime());
                cal.setTime(eDate);
                cal.add(Calendar.DAY_OF_MONTH, (int) -diffDays);
                String prevEndDate = sdf.format(cal.getTime());
                prevSumDep = getSumDeposit(subtreeIds, prevFromDate, prevEndDate, isMaster);
            } catch (Exception ignored) {}
            
            double deltaDeposit = 0.0;
            if (sumDep > 0 && prevSumDep == 0) {
                deltaDeposit = 100.0;
            } else if (prevSumDep > 0) {
                deltaDeposit = ((double)(sumDep - prevSumDep) / prevSumDep) * 100.0;
            }
            deltaDeposit = Math.round(deltaDeposit * 10.0) / 10.0;
            
            obj.put("sumDeposit", sumDep);
            obj.put("sumDepositDelta", deltaDeposit);
            obj.put("sumWithdraw", sumWith);
            obj.put("totalProfit", sumDep - sumWith);

            long totalBet = getTotalSubtreeBetAmount(subtreeIds, fromDate, endDate, isMaster);
            long prevTotalBet = 0;
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                Date fDate = sdf.parse(fromDate);
                Date eDate = sdf.parse(endDate);
                long diffDays = java.util.concurrent.TimeUnit.DAYS.convert(Math.abs(eDate.getTime() - fDate.getTime()), java.util.concurrent.TimeUnit.MILLISECONDS) + 1;
                Calendar cal = Calendar.getInstance();
                cal.setTime(fDate);
                cal.add(Calendar.DAY_OF_MONTH, (int) -diffDays);
                String prevFromDate = sdf.format(cal.getTime());
                cal.setTime(eDate);
                cal.add(Calendar.DAY_OF_MONTH, (int) -diffDays);
                String prevEndDate = sdf.format(cal.getTime());
                prevTotalBet = getTotalSubtreeBetAmount(subtreeIds, prevFromDate, prevEndDate, isMaster);
            } catch (Exception ignored) {}

            double deltaBet = 0.0;
            if (totalBet > 0 && prevTotalBet == 0) deltaBet = 100.0;
            else if (prevTotalBet > 0) deltaBet = ((double)(totalBet - prevTotalBet) / prevTotalBet) * 100.0;
            deltaBet = Math.round(deltaBet * 10.0) / 10.0;

            long agentUserId = AgentHierarchyHelper.getAgentUserId(agent.nickname);
            int totalAgt = getTotalSubAgents(subtreeIds, agentUserId);
            int newAgt = getTotalSubAgentsNew(subtreeIds, agentUserId, fromDate, endDate);

            obj.put("totalMember", getTotalMembers(subtreeIds, isMaster));
            obj.put("totalAgents", totalAgt);
            obj.put("totalAgentsNew", newAgt);
            obj.put("totalUserBet", totalBet);
            obj.put("totalUserBetDelta", deltaBet);
            obj.put("totalBettors", getTotalBettors(subtreeIds, fromDate, endDate, isMaster));
            obj.put("totalUserRegisterNew", getTotalMembersNew(subtreeIds, fromDate, endDate, isMaster));
            obj.put("totalUserLocked", getTotalUserLocked(subtreeIds, isMaster));
            long partnerCommission = getSumPartnerCommission(subtreeIds, fromDate, endDate, isMaster);
            obj.put("partnerCommission", partnerCommission);
            obj.put("rakeCommission", 0);

            obj.put("totalUserOnline", getCurrentCcu());

            // Breakdown: nạp / rút / lợi nhuận per segment (hour or date)
            obj.put("dailyBreakdown", getBreakdown(subtreeIds, fromDate, endDate, normalizedSegment, isMaster));

            res.total = 1;
            res.setData(obj);
            res.setErrorCode("0");
            res.setSuccess(true);
            String json = res.toJson();
            com.vinplay.vbee.common.cache.ResponseCacheHelper.put(cacheKey, json, histOnly);
            return json;

        } catch (Exception e) {
            logger.error("ReportGeneral4AgencyProcessor error", e);
            return "{\"success\":false,\"errorCode\":\"1001\"}";
        }
    }

    private String normalizeDateOnly(String input) {
        if (input == null) {
            return null;
        }
        String value = input.trim();
        if (value.isEmpty()) {
            return null;
        }
        return value.length() >= 10 ? value.substring(0, 10) : value;
    }

    private long getSumDeposit(List<Integer> agentIds, String fromDate, String endDate, boolean isMaster) {
        if (!isMaster && (agentIds == null || agentIds.isEmpty())) return 0;
        String inIds = AgentHierarchyHelper.inPlaceholders(agentIds.size());
        String bankJoin = isMaster ? "" : " JOIN users u ON u.id = dt.user_id";
        String bankAgent = isMaster ? "" : " u.parent_agent_id IN (" + inIds + ") AND";
        String cryptoJoin = isMaster ? "" : " JOIN users u ON u.id = cd.user_id";
        String cryptoAgent = isMaster ? "" : " u.parent_agent_id IN (" + inIds + ") AND";
        String adminAgent = isMaster ? "" : " cwt.agent_id IN (" + inIds + ") AND";
        String sql = "SELECT IFNULL(SUM(amount),0) total FROM ("
                + " SELECT dt.amount AS amount FROM deposit_transactions dt" + bankJoin
                + " WHERE" + bankAgent + " dt.status IN ('APPROVED','COMPLETED','SUCCESS')"
                + " AND dt.created_at >= ? AND dt.created_at <= ?"
                + " UNION ALL"
                + " SELECT cd.amount_krw AS amount FROM crypto_deposits cd" + cryptoJoin
                + " WHERE" + cryptoAgent + " cd.status IN ('APPROVED','COMPLETED','SUCCESS')"
                + " AND cd.created_at >= ? AND cd.created_at <= ?"
                + " UNION ALL"
                + " SELECT cwt.amount AS amount FROM credit_wallet_transactions cwt"
                + " WHERE" + adminAgent + " cwt.type = 'ADMIN_CREDIT' AND cwt.direction = 'CREDIT'"
                + " AND cwt.created_at >= ? AND cwt.created_at <= ?"
                + ") money_in";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            int idx = 1;
            if (!isMaster) idx = AgentHierarchyHelper.setIntParams(stm, idx, agentIds);
            stm.setString(idx++, fromDate + " 00:00:00");
            stm.setString(idx++, endDate + " 23:59:59");
            if (!isMaster) idx = AgentHierarchyHelper.setIntParams(stm, idx, agentIds);
            stm.setString(idx++, fromDate + " 00:00:00");
            stm.setString(idx++, endDate + " 23:59:59");
            if (!isMaster) idx = AgentHierarchyHelper.setIntParams(stm, idx, agentIds);
            stm.setString(idx++, fromDate + " 00:00:00");
            stm.setString(idx, endDate + " 23:59:59");
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) return rs.getLong("total");
            }
        } catch (Exception e) {
            logger.error("getSumDeposit error", e);
        }
        return 0;
    }

    private long getSumWithdraw(List<Integer> agentIds, String fromDate, String endDate, boolean isMaster) {
        if (!isMaster && (agentIds == null || agentIds.isEmpty())) return 0;
        String inIds = AgentHierarchyHelper.inPlaceholders(agentIds.size());
        String bankJoin = isMaster ? "" : " JOIN users u ON u.id = bw.user_id";
        String bankAgent = isMaster ? "" : " u.parent_agent_id IN (" + inIds + ") AND";
        String cryptoJoin = isMaster ? "" : " JOIN users u ON u.id = cw.user_id";
        String cryptoAgent = isMaster ? "" : " u.parent_agent_id IN (" + inIds + ") AND";
        String adminAgent = isMaster ? "" : " cwt.agent_id IN (" + inIds + ") AND";
        String sql = "SELECT IFNULL(SUM(amount),0) total FROM ("
                + " SELECT bw.amount_krw AS amount FROM bank_withdrawals bw" + bankJoin
                + " WHERE" + bankAgent + " bw.status IN ('APPROVED','COMPLETED')"
                + " AND bw.created_at >= ? AND bw.created_at <= ?"
                + " UNION ALL"
                + " SELECT cw.amount_krw AS amount FROM crypto_withdrawals cw" + cryptoJoin
                + " WHERE" + cryptoAgent + " cw.status IN ('APPROVED','COMPLETED')"
                + " AND cw.created_at >= ? AND cw.created_at <= ?"
                + " UNION ALL"
                + " SELECT cwt.amount AS amount FROM credit_wallet_transactions cwt"
                + " WHERE" + adminAgent + " cwt.type = 'ADMIN_REVOKE' AND cwt.direction = 'DEBIT'"
                + " AND cwt.created_at >= ? AND cwt.created_at <= ?"
                + ") money_out";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            int idx = 1;
            if (!isMaster) idx = AgentHierarchyHelper.setIntParams(stm, idx, agentIds);
            stm.setString(idx++, fromDate + " 00:00:00");
            stm.setString(idx++, endDate + " 23:59:59");
            if (!isMaster) idx = AgentHierarchyHelper.setIntParams(stm, idx, agentIds);
            stm.setString(idx++, fromDate + " 00:00:00");
            stm.setString(idx++, endDate + " 23:59:59");
            if (!isMaster) idx = AgentHierarchyHelper.setIntParams(stm, idx, agentIds);
            stm.setString(idx++, fromDate + " 00:00:00");
            stm.setString(idx, endDate + " 23:59:59");
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) return rs.getLong("total");
            }
        } catch (Exception e) {
            logger.error("getSumWithdraw error", e);
        }
        return 0;
    }

    private List<Map<String, Object>> getBreakdown(List<Integer> agentIds, String fromDate, String endDate, String segment, boolean isMaster) {
        List<Map<String, Object>> arr = new ArrayList<>();
        if (!isMaster && (agentIds == null || agentIds.isEmpty())) return arr;
        String dateFormat = "hour".equals(segment) ? "%Y-%m-%d %H:00" : "%Y-%m-%d";
        String inIds = AgentHierarchyHelper.inPlaceholders(agentIds.size());
        String joinUserDT = isMaster ? "" : " JOIN users u ON u.id = dt.user_id";
        String whereAgentDT = isMaster ? "" : " u.parent_agent_id IN (" + inIds + ") AND";
        String joinUserCD = isMaster ? "" : " JOIN users u ON u.id = cd.user_id";
        String whereAgentCD = isMaster ? "" : " u.parent_agent_id IN (" + inIds + ") AND";
        String joinUserBW = isMaster ? "" : " JOIN users u ON u.id = bw.user_id";
        String whereAgentBW = isMaster ? "" : " u.parent_agent_id IN (" + inIds + ") AND";
        String joinUserCW = isMaster ? "" : " JOIN users u ON u.id = cw.user_id";
        String whereAgentCW = isMaster ? "" : " u.parent_agent_id IN (" + inIds + ") AND";
        String whereAgentCwt = isMaster ? "" : " cwt.agent_id IN (" + inIds + ") AND";

        String sql = "SELECT label, SUM(dep) as total_deposit, SUM(wit) as total_withdraw FROM ("
                + "  SELECT DATE_FORMAT(dt.created_at, '" + dateFormat + "') as label, dt.amount as dep, 0 as wit"
                + "  FROM deposit_transactions dt" + joinUserDT
                + "  WHERE" + whereAgentDT + " dt.status IN ('APPROVED','COMPLETED','SUCCESS')"
                + "  AND dt.created_at >= ? AND dt.created_at <= ?"
                + "  UNION ALL"
                + "  SELECT DATE_FORMAT(cd.created_at, '" + dateFormat + "') as label, cd.amount_krw as dep, 0 as wit"
                + "  FROM crypto_deposits cd" + joinUserCD
                + "  WHERE" + whereAgentCD + " cd.status IN ('APPROVED','COMPLETED','SUCCESS')"
                + "  AND cd.created_at >= ? AND cd.created_at <= ?"
                + "  UNION ALL"
                + "  SELECT DATE_FORMAT(cwt.created_at, '" + dateFormat + "') as label, cwt.amount as dep, 0 as wit"
                + "  FROM credit_wallet_transactions cwt"
                + "  WHERE" + whereAgentCwt + " cwt.type = 'ADMIN_CREDIT' AND cwt.direction = 'CREDIT'"
                + "  AND cwt.created_at >= ? AND cwt.created_at <= ?"
                + "  UNION ALL"
                + "  SELECT DATE_FORMAT(bw.created_at, '" + dateFormat + "') as label, 0 as dep, bw.amount_krw as wit"
                + "  FROM bank_withdrawals bw" + joinUserBW
                + "  WHERE" + whereAgentBW + " bw.status IN ('APPROVED','COMPLETED')"
                + "  AND bw.created_at >= ? AND bw.created_at <= ?"
                + "  UNION ALL"
                + "  SELECT DATE_FORMAT(cw.created_at, '" + dateFormat + "') as label, 0 as dep, cw.amount_krw as wit"
                + "  FROM crypto_withdrawals cw" + joinUserCW
                + "  WHERE" + whereAgentCW + " cw.status IN ('APPROVED','COMPLETED')"
                + "  AND cw.created_at >= ? AND cw.created_at <= ?"
                + "  UNION ALL"
                + "  SELECT DATE_FORMAT(cwt.created_at, '" + dateFormat + "') as label, 0 as dep, cwt.amount as wit"
                + "  FROM credit_wallet_transactions cwt"
                + "  WHERE" + whereAgentCwt + " cwt.type = 'ADMIN_REVOKE' AND cwt.direction = 'DEBIT'"
                + "  AND cwt.created_at >= ? AND cwt.created_at <= ?"
                + ") combined GROUP BY label ORDER BY label ASC";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            int idx = 1;
            if (!isMaster) idx = AgentHierarchyHelper.setIntParams(stm, idx, agentIds);
            stm.setString(idx++, fromDate + " 00:00:00");
            stm.setString(idx++, endDate + " 23:59:59");
            if (!isMaster) idx = AgentHierarchyHelper.setIntParams(stm, idx, agentIds);
            stm.setString(idx++, fromDate + " 00:00:00");
            stm.setString(idx++, endDate + " 23:59:59");
            if (!isMaster) idx = AgentHierarchyHelper.setIntParams(stm, idx, agentIds);
            stm.setString(idx++, fromDate + " 00:00:00");
            stm.setString(idx++, endDate + " 23:59:59");
            if (!isMaster) idx = AgentHierarchyHelper.setIntParams(stm, idx, agentIds);
            stm.setString(idx++, fromDate + " 00:00:00");
            stm.setString(idx++, endDate + " 23:59:59");
            if (!isMaster) idx = AgentHierarchyHelper.setIntParams(stm, idx, agentIds);
            stm.setString(idx++, fromDate + " 00:00:00");
            stm.setString(idx++, endDate + " 23:59:59");
            if (!isMaster) idx = AgentHierarchyHelper.setIntParams(stm, idx, agentIds);
            stm.setString(idx++, fromDate + " 00:00:00");
            stm.setString(idx, endDate + " 23:59:59");
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    long dep = rs.getLong("total_deposit");
                    long wit = rs.getLong("total_withdraw");
                    row.put("date", rs.getString("label"));
                    row.put("deposit", dep);
                    row.put("withdraw", wit);
                    row.put("profit", dep - wit);
                    arr.add(row);
                }
            }
        } catch (Exception e) {
            logger.error("getBreakdown error", e);
        }
        return arr;
    }

    private String firstNonEmpty(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private int getTotalSubAgents(List<Integer> agentIds, long agentUserId) {
        if (agentIds == null || agentIds.isEmpty()) return 0;
        String inIds = AgentHierarchyHelper.inPlaceholders(agentIds.size());
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(DISTINCT nick_name) FROM (");
        
        sql.append("  SELECT u1.nick_name FROM users u1 WHERE u1.parent_agent_id IN (").append(inIds).append(") AND u1.dai_ly > 0");
        if (agentUserId > 0) sql.append(" AND u1.id != ").append(agentUserId);
        
        sql.append("  UNION ");
        
        sql.append("  SELECT u2.nick_name FROM vinplay_admin.useragent ua ");
        sql.append("  JOIN users u2 ON u2.nick_name = ua.nickname COLLATE utf8mb4_general_ci ");
        sql.append("  WHERE ua.id IN (").append(inIds).append(")");
        if (agentUserId > 0) sql.append(" AND u2.id != ").append(agentUserId);
        
        sql.append(") AS combined");

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql.toString())) {
            
            int idx = 1;
            idx = AgentHierarchyHelper.setIntParams(stm, idx, agentIds);
            idx = AgentHierarchyHelper.setIntParams(stm, idx, agentIds);
            
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) { logger.error("getTotalSubAgents error", e); }
        return 0;
    }

    private int getTotalBettors(List<Integer> agentIds, String fromDate, String endDate, boolean isMaster) {
        if (!isMaster && (agentIds == null || agentIds.isEmpty())) return 0;
        String inIds = AgentHierarchyHelper.inPlaceholders(agentIds.size());
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(DISTINCT lr.nick_name) AS total FROM log_report_user lr ");
        
        if (isMaster) {
            sql.append("WHERE lr.nick_name NOT IN (SELECT nick_name FROM users WHERE is_bot = 1) AND ");
        } else {
            sql.append("JOIN users u ON lr.nick_name = u.nick_name ");
            sql.append("WHERE u.parent_agent_id IN (").append(inIds).append(") AND ");
        }
        
        sql.append("lr.time_report >= ? AND lr.time_report <= ? ")
           .append("AND (lr.wm > 0 OR lr.ag > 0 OR lr.ibc > 0 OR lr.cmd > 0 OR lr.sbo > 0 OR lr.ebet > 0 OR lr.fish > 0 OR lr.minipoker > 0 OR lr.taixiu > 0 OR lr.taixiu_st > 0 OR lr.tlmn > 0 OR lr.bacay > 0 OR lr.slot_pokemon > 0 OR lr.slot_bitcoin > 0 OR lr.slot_taydu > 0 OR lr.slot_angrybird > 0 OR lr.slot_thantai > 0 OR lr.slot_thanbai > 0 OR lr.slot_thethao > 0 OR lr.slot_chiemtinh > 0 OR lr.slot_bikini > 0 OR lr.slot_galaxy > 0 OR lr.caothap > 0 OR lr.baucua > 0 OR lr.xocdia > 0)");
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (!isMaster) idx = AgentHierarchyHelper.setIntParams(stm, idx, agentIds);
            stm.setString(idx++, fromDate + " 00:00:00");
            stm.setString(idx, endDate + " 23:59:59");
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) return rs.getInt("total");
            }
        } catch (Exception e) { logger.error("getTotalBettors error", e); }
        return 0;
    }

    private long getTotalSubtreeBetAmount(List<Integer> agentIds, String fromDate, String endDate, boolean isMaster) {
        if (!isMaster && (agentIds == null || agentIds.isEmpty())) return 0;
        StringBuilder sqlBet = new StringBuilder();
        if (isMaster) {
            sqlBet.append("SELECT IFNULL(SUM(IFNULL(wm,0) + IFNULL(ag,0) + IFNULL(ibc,0) + IFNULL(cmd,0) + IFNULL(sbo,0) + IFNULL(ebet,0) + IFNULL(fish,0) + IFNULL(slot_pokemon,0) + IFNULL(slot_bitcoin,0) + IFNULL(slot_taydu,0) + IFNULL(slot_angrybird,0) + IFNULL(slot_thantai,0) + IFNULL(slot_thanbai,0) + IFNULL(slot_thethao,0) + IFNULL(slot_chiemtinh,0) + IFNULL(slot_bikini,0) + IFNULL(slot_galaxy,0) + IFNULL(minipoker,0) + IFNULL(caothap,0) + IFNULL(baucua,0) + IFNULL(xocdia,0) + IFNULL(taixiu,0) + IFNULL(taixiu_st,0) + IFNULL(tlmn,0) + IFNULL(bacay,0)), 0) AS total_bet ");
            sqlBet.append("FROM log_report_user WHERE nick_name NOT IN (SELECT nick_name FROM users WHERE is_bot = 1) AND time_report >= ? AND time_report <= ?");
        } else {
            String inIds = AgentHierarchyHelper.inPlaceholders(agentIds.size());
            sqlBet.append("SELECT IFNULL(SUM(IFNULL(wm,0) + IFNULL(ag,0) + IFNULL(ibc,0) + IFNULL(cmd,0) + IFNULL(sbo,0) + IFNULL(ebet,0) + IFNULL(fish,0) + IFNULL(slot_pokemon,0) + IFNULL(slot_bitcoin,0) + IFNULL(slot_taydu,0) + IFNULL(slot_angrybird,0) + IFNULL(slot_thantai,0) + IFNULL(slot_thanbai,0) + IFNULL(slot_thethao,0) + IFNULL(slot_chiemtinh,0) + IFNULL(slot_bikini,0) + IFNULL(slot_galaxy,0) + IFNULL(minipoker,0) + IFNULL(caothap,0) + IFNULL(baucua,0) + IFNULL(xocdia,0) + IFNULL(taixiu,0) + IFNULL(taixiu_st,0) + IFNULL(tlmn,0) + IFNULL(bacay,0)), 0) AS total_bet ");
            sqlBet.append("FROM log_report_user WHERE nick_name IN (");
            sqlBet.append("  SELECT u1.nick_name FROM users u1 WHERE u1.parent_agent_id IN (").append(inIds).append(")");
            sqlBet.append("  UNION ");
            sqlBet.append("  SELECT ua.nickname COLLATE utf8mb4_general_ci FROM vinplay_admin.useragent ua WHERE ua.id IN (").append(inIds).append(")");
            sqlBet.append(") AND time_report >= ? AND time_report <= ?");
        }

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sqlBet.toString())) {
            int idx = 1;
            if (!isMaster) {
                idx = AgentHierarchyHelper.setIntParams(stm, idx, agentIds);
                idx = AgentHierarchyHelper.setIntParams(stm, idx, agentIds);
            }
            stm.setString(idx++, fromDate + " 00:00:00");
            stm.setString(idx, endDate + " 23:59:59");
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) return rs.getLong("total_bet");
            }
        } catch (Exception e) { logger.error("getTotalSubtreeBetAmount error", e); }
        return 0;
    }

    private int getTotalSubAgentsNew(List<Integer> agentIds, long agentUserId, String fromDate, String endDate) {
        if (agentIds == null || agentIds.isEmpty()) return 0;
        String inIds = AgentHierarchyHelper.inPlaceholders(agentIds.size());
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(DISTINCT nick_name) FROM (");
        
        sql.append("  SELECT u1.nick_name FROM users u1 WHERE u1.parent_agent_id IN (").append(inIds).append(") AND u1.dai_ly > 0");
        sql.append(" AND u1.create_time >= ? AND u1.create_time <= ?");
        if (agentUserId > 0) sql.append(" AND u1.id != ").append(agentUserId);
        
        sql.append("  UNION ");
        
        sql.append("  SELECT u2.nick_name FROM vinplay_admin.useragent ua ");
        sql.append("  JOIN users u2 ON u2.nick_name = ua.nickname COLLATE utf8mb4_general_ci ");
        sql.append("  WHERE ua.id IN (").append(inIds).append(")");
        sql.append(" AND u2.create_time >= ? AND u2.create_time <= ?");
        if (agentUserId > 0) sql.append(" AND u2.id != ").append(agentUserId);
        
        sql.append(") AS combined");

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql.toString())) {
            
            int idx = 1;
            idx = AgentHierarchyHelper.setIntParams(stm, idx, agentIds);
            stm.setString(idx++, fromDate + " 00:00:00");
            stm.setString(idx++, endDate + " 23:59:59");
            
            idx = AgentHierarchyHelper.setIntParams(stm, idx, agentIds);
            stm.setString(idx++, fromDate + " 00:00:00");
            stm.setString(idx++, endDate + " 23:59:59");
            
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) { logger.error("getTotalSubAgentsNew error", e); }
        return 0;
    }

    private int getTotalMembers(List<Integer> agentIds, boolean isMaster) {
        if (isMaster) {
            String sql = "SELECT COUNT(id) FROM users WHERE is_bot = 0";
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            } catch (Exception e) { logger.error("getTotalMembers error", e); }
            return 0;
        }
        // GitLab #38: canonical downline count. Bots excluded by default —
        // same definition as ListAllPlayersUnderAgent (A) and
        // ListOnlineOfflineMembers (B) so the three endpoints agree.
        return AgentHierarchyHelper.countDownline(agentIds, false);
    }

    private int getTotalMembersNew(List<Integer> agentIds, String fromDate, String endDate, boolean isMaster) {
        if (!isMaster && (agentIds == null || agentIds.isEmpty())) return 0;
        String inIds = AgentHierarchyHelper.inPlaceholders(agentIds.size());
        String whereAgent = isMaster ? "" : "parent_agent_id IN (" + inIds + ") AND ";
        String sql = "SELECT COUNT(id) FROM users WHERE " + whereAgent + "is_bot = 0 AND create_time >= ? AND create_time <= ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            int idx = 1;
            if (!isMaster) idx = AgentHierarchyHelper.setIntParams(stm, idx, agentIds);
            stm.setString(idx++, fromDate + " 00:00:00");
            stm.setString(idx, endDate + " 23:59:59");
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) { logger.error("getTotalMembersNew error", e); }
        return 0;
    }

    private int getTotalUserLocked(List<Integer> agentIds, boolean isMaster) {
        String inIds = AgentHierarchyHelper.inPlaceholders(agentIds.size());
        String whereAgent = isMaster ? "" : "parent_agent_id IN (" + inIds + ") AND ";
        String sql = "SELECT COUNT(id) AS total FROM users WHERE " + whereAgent + "status != 0";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            if (!isMaster) AgentHierarchyHelper.setIntParams(stm, 1, agentIds);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) return rs.getInt("total");
            }
        } catch (Exception e) { logger.error("getTotalUserLocked error", e); }
        return 0;
    }

    private long getSumPartnerCommission(List<Integer> agentIds, String fromDate, String endDate, boolean isMaster) {
        String inIds = AgentHierarchyHelper.inPlaceholders(agentIds.size());
        String whereAgent = isMaster ? "" : " agent_user_id IN (" + inIds + ") AND";
        String sql = "SELECT IFNULL(SUM(net_rebate),0) total FROM rebate_logs"
                + " WHERE" + whereAgent
                + " created_at >= ? AND created_at <= ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            int idx = 1;
            if (!isMaster) idx = AgentHierarchyHelper.setIntParams(stm, idx, agentIds);
            stm.setString(idx++, fromDate + " 00:00:00");
            stm.setString(idx, endDate + " 23:59:59");
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) return rs.getLong("total");
            }
        } catch (Exception e) { logger.error("getSumPartnerCommission error", e); }
        return 0;
    }

    private String executeSiteWide(ReportLogUserResponse res, String fromDate, String endDate, String segment) {
        final String cacheKey = com.vinplay.vbee.common.cache.ResponseCacheHelper.key(
                "AdminSiteWide", fromDate, endDate, segment);
        String cached = com.vinplay.vbee.common.cache.ResponseCacheHelper.get(cacheKey);
        if (cached != null) return cached;
        final boolean histOnly = com.vinplay.vbee.common.cache.ResponseCacheHelper.isHistoricalOnly(endDate);

        try {
            Map<String, Object> obj = new HashMap<>();

            long sumDep = getSiteWideDeposit(fromDate, endDate);
            long sumWith = getSiteWideWithdraw(fromDate, endDate);
            obj.put("sumDeposit", sumDep);
            obj.put("sumWithdraw", sumWith);
            obj.put("totalProfit", sumDep - sumWith);
            obj.put("sumDepositDelta", 0.0);
            obj.put("totalUserBet", 0L);
            obj.put("totalUserBetDelta", 0.0);
            obj.put("totalMember", getSiteWideMemberCount());
            obj.put("totalAgents", 0);
            obj.put("totalAgentsNew", 0);
            obj.put("totalBettors", 0);
            obj.put("totalUserRegisterNew", getSiteWideNewMembers(fromDate, endDate));
            obj.put("totalUserLocked", 0);
            obj.put("partnerCommission", getSiteWideCommission(fromDate, endDate));
            obj.put("rakeCommission", 0);
            obj.put("totalUserOnline", getCurrentCcu());
            obj.put("dailyBreakdown", getSiteWideBreakdown(fromDate, endDate, segment));

            res.total = 1;
            res.setData(obj);
            res.setErrorCode("0");
            res.setSuccess(true);
            String json = res.toJson();
            com.vinplay.vbee.common.cache.ResponseCacheHelper.put(cacheKey, json, histOnly);
            return json;
        } catch (Exception e) {
            logger.error("executeSiteWide error", e);
            return "{\"success\":false,\"errorCode\":\"1001\"}";
        }
    }

    private int getCurrentCcu() {
        try {
            com.vinplay.vbee.common.cache.DistCache<String, Integer> map =
                    com.vinplay.vbee.common.cache.CacheFactory.get("mapCheckCCU", Integer.class);
            Integer v = map.get("mapCheckCCU");
            if (v != null && v > 0) return v;
        } catch (Exception ignore) {}
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            Document latest = db.getCollection("log_ccu")
                    .find().sort(new Document("_id", -1)).limit(1).first();
            if (latest != null) return latest.getInteger("ccu", 0);
        } catch (Exception ignore) {}
        return 0;
    }

    private long getSiteWideDeposit(String fromDate, String endDate) {
        String sql = "SELECT IFNULL(SUM(amount),0) total FROM ("
                + " SELECT amount FROM deposit_transactions"
                + " WHERE status IN ('APPROVED','COMPLETED','SUCCESS') AND created_at >= ? AND created_at <= ?"
                + " UNION ALL"
                + " SELECT amount_krw AS amount FROM crypto_deposits"
                + " WHERE status IN ('APPROVED','COMPLETED','SUCCESS') AND created_at >= ? AND created_at <= ?"
                + " UNION ALL"
                + " SELECT amount FROM credit_wallet_transactions"
                + " WHERE type = 'ADMIN_CREDIT' AND direction = 'CREDIT' AND created_at >= ? AND created_at <= ?"
                + ") money_in";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setString(1, fromDate + " 00:00:00");
            stm.setString(2, endDate + " 23:59:59");
            stm.setString(3, fromDate + " 00:00:00");
            stm.setString(4, endDate + " 23:59:59");
            stm.setString(5, fromDate + " 00:00:00");
            stm.setString(6, endDate + " 23:59:59");
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) return rs.getLong("total");
            }
        } catch (Exception e) { logger.error("getSiteWideDeposit error", e); }
        return 0;
    }

    private long getSiteWideWithdraw(String fromDate, String endDate) {
        String sql = "SELECT IFNULL(SUM(amount),0) total FROM ("
                + " SELECT amount_krw AS amount FROM bank_withdrawals"
                + " WHERE status IN ('APPROVED','COMPLETED') AND created_at >= ? AND created_at <= ?"
                + " UNION ALL"
                + " SELECT amount_krw AS amount FROM crypto_withdrawals"
                + " WHERE status IN ('APPROVED','COMPLETED') AND created_at >= ? AND created_at <= ?"
                + " UNION ALL"
                + " SELECT amount FROM credit_wallet_transactions"
                + " WHERE type = 'ADMIN_REVOKE' AND direction = 'DEBIT' AND created_at >= ? AND created_at <= ?"
                + ") money_out";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setString(1, fromDate + " 00:00:00");
            stm.setString(2, endDate + " 23:59:59");
            stm.setString(3, fromDate + " 00:00:00");
            stm.setString(4, endDate + " 23:59:59");
            stm.setString(5, fromDate + " 00:00:00");
            stm.setString(6, endDate + " 23:59:59");
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) return rs.getLong("total");
            }
        } catch (Exception e) { logger.error("getSiteWideWithdraw error", e); }
        return 0;
    }

    private int getSiteWideMemberCount() {
        String sql = "SELECT COUNT(id) AS total FROM users WHERE dai_ly = 0 AND is_bot = 0";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql);
             ResultSet rs = stm.executeQuery()) {
            if (rs.next()) return rs.getInt("total");
        } catch (Exception e) { logger.error("getSiteWideMemberCount error", e); }
        return 0;
    }

    private int getSiteWideNewMembers(String fromDate, String endDate) {
        String sql = "SELECT COUNT(id) AS total FROM users WHERE dai_ly = 0 AND is_bot = 0"
                + " AND create_time >= ? AND create_time <= ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setString(1, fromDate + " 00:00:00");
            stm.setString(2, endDate + " 23:59:59");
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) return rs.getInt("total");
            }
        } catch (Exception e) { logger.error("getSiteWideNewMembers error", e); }
        return 0;
    }

    private long getSiteWideCommission(String fromDate, String endDate) {
        String sql = "SELECT IFNULL(SUM(net_rebate),0) total FROM rebate_logs"
                + " WHERE created_at >= ? AND created_at <= ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setString(1, fromDate + " 00:00:00");
            stm.setString(2, endDate + " 23:59:59");
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) return rs.getLong("total");
            }
        } catch (Exception e) { logger.error("getSiteWideCommission error", e); }
        return 0;
    }

    private List<Map<String, Object>> getSiteWideBreakdown(String fromDate, String endDate, String segment) {
        List<Map<String, Object>> arr = new ArrayList<>();
        String dateFormat = "hour".equals(segment) ? "%Y-%m-%d %H:00" : "%Y-%m-%d";
        String sql = "SELECT label, SUM(dep) AS total_deposit, SUM(wit) AS total_withdraw FROM ("
                + "  SELECT DATE_FORMAT(created_at, '" + dateFormat + "') AS label, amount AS dep, 0 AS wit"
                + "  FROM deposit_transactions WHERE status IN ('APPROVED','COMPLETED','SUCCESS')"
                + "  AND created_at >= ? AND created_at <= ?"
                + "  UNION ALL"
                + "  SELECT DATE_FORMAT(created_at, '" + dateFormat + "') AS label, amount_krw AS dep, 0 AS wit"
                + "  FROM crypto_deposits WHERE status IN ('APPROVED','COMPLETED','SUCCESS')"
                + "  AND created_at >= ? AND created_at <= ?"
                + "  UNION ALL"
                + "  SELECT DATE_FORMAT(created_at, '" + dateFormat + "') AS label, amount AS dep, 0 AS wit"
                + "  FROM credit_wallet_transactions WHERE type = 'ADMIN_CREDIT' AND direction = 'CREDIT'"
                + "  AND created_at >= ? AND created_at <= ?"
                + "  UNION ALL"
                + "  SELECT DATE_FORMAT(created_at, '" + dateFormat + "') AS label, 0 AS dep, amount_krw AS wit"
                + "  FROM bank_withdrawals WHERE status IN ('APPROVED','COMPLETED')"
                + "  AND created_at >= ? AND created_at <= ?"
                + "  UNION ALL"
                + "  SELECT DATE_FORMAT(created_at, '" + dateFormat + "') AS label, 0 AS dep, amount_krw AS wit"
                + "  FROM crypto_withdrawals WHERE status IN ('APPROVED','COMPLETED')"
                + "  AND created_at >= ? AND created_at <= ?"
                + "  UNION ALL"
                + "  SELECT DATE_FORMAT(created_at, '" + dateFormat + "') AS label, 0 AS dep, amount AS wit"
                + "  FROM credit_wallet_transactions WHERE type = 'ADMIN_REVOKE' AND direction = 'DEBIT'"
                + "  AND created_at >= ? AND created_at <= ?"
                + ") combined GROUP BY label ORDER BY label ASC";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setString(1, fromDate + " 00:00:00");
            stm.setString(2, endDate + " 23:59:59");
            stm.setString(3, fromDate + " 00:00:00");
            stm.setString(4, endDate + " 23:59:59");
            stm.setString(5, fromDate + " 00:00:00");
            stm.setString(6, endDate + " 23:59:59");
            stm.setString(7, fromDate + " 00:00:00");
            stm.setString(8, endDate + " 23:59:59");
            stm.setString(9, fromDate + " 00:00:00");
            stm.setString(10, endDate + " 23:59:59");
            stm.setString(11, fromDate + " 00:00:00");
            stm.setString(12, endDate + " 23:59:59");
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    long dep = rs.getLong("total_deposit");
                    long wit = rs.getLong("total_withdraw");
                    row.put("date", rs.getString("label"));
                    row.put("deposit", dep);
                    row.put("withdraw", wit);
                    row.put("profit", dep - wit);
                    arr.add(row);
                }
            }
        } catch (Exception e) { logger.error("getSiteWideBreakdown error", e); }
        return arr;
    }
}
