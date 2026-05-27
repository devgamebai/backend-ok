package com.vinplay.api.backend.processors.agent;

import com.vinplay.dal.dao.AgentDAO;
import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.dal.entities.agent.UserAgentModel;
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

public class DetailMemberOfAgencyProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    private static final class ParentInfo {
        private final String nickname;
        private final String code;

        private ParentInfo(String nickname, String code) {
            this.nickname = nickname;
            this.code = code;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static ParentInfo resolveParentInfoForAgent(UserAgentModel targetAgent) throws Exception {
        if (targetAgent == null || targetAgent.getParentid() == null || targetAgent.getParentid() <= 0) {
            return new ParentInfo("", "");
        }

        try (Connection adminConn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpool_admin");
             PreparedStatement stm = adminConn.prepareStatement(
                     "SELECT nickname, code FROM vinplay_admin.useragent WHERE id = ? LIMIT 1")) {
            stm.setInt(1, targetAgent.getParentid());
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    String parentCode = safe(rs.getString("code"));
                    if ("0".equals(parentCode)) {
                        return new ParentInfo("", "");
                    }
                    return new ParentInfo(safe(rs.getString("nickname")), parentCode);
                }
            }
        }

        return new ParentInfo("", "");
    }

    private static boolean csvContainsId(String csv, Integer id) {
        if (csv == null || csv.trim().isEmpty() || id == null) {
            return false;
        }
        String needle = String.valueOf(id);
        for (String part : csv.split(",")) {
            if (needle.equals(part.trim())) {
                return true;
            }
        }
        return false;
    }

    private static UserAgentModel resolveRequesterAgent(AgentDAO agentDAO, String rc) throws Exception {
        if (rc == null || rc.trim().isEmpty()) {
            return null;
        }

        try (Connection adminConn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
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

    private static boolean isUserInAgentTree(UserAgentModel requesterAgent, String usernameToFind) throws Exception {
        List<String> subtreeCodes = new ArrayList<>();
        List<String> subtreeNicks = new ArrayList<>();
        List<Integer> subtreeIds = new ArrayList<>();
        if (requesterAgent.getCode() != null && !requesterAgent.getCode().trim().isEmpty()) {
            subtreeCodes.add(requesterAgent.getCode());
        }
        if (requesterAgent.getNickname() != null && !requesterAgent.getNickname().trim().isEmpty()) {
            subtreeNicks.add(requesterAgent.getNickname());
        }
        if (requesterAgent.getId() != null) {
            subtreeIds.add(requesterAgent.getId());
        }

        try (Connection adminConn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
            try (PreparedStatement ps = adminConn.prepareStatement(
                    "SELECT id, code, nickname FROM vinplay_admin.useragent WHERE id = ? OR FIND_IN_SET(?, ancestors) > 0")) {
                ps.setInt(1, requesterAgent.getId());
                ps.setString(2, String.valueOf(requesterAgent.getId()));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        String code = rs.getString("code");
                        String nick = rs.getString("nickname");
                        if (!subtreeIds.contains(id)) {
                            subtreeIds.add(id);
                        }
                        if (code != null && !code.trim().isEmpty() && !subtreeCodes.contains(code)) {
                            subtreeCodes.add(code);
                        }
                        if (nick != null && !nick.trim().isEmpty() && !subtreeNicks.contains(nick)) {
                            subtreeNicks.add(nick);
                        }
                    }
                }
            }
        }

        if (subtreeCodes.isEmpty() && subtreeNicks.isEmpty() && subtreeIds.isEmpty()) {
            return false;
        }

        StringBuilder codePlaceholders = new StringBuilder();
        for (int i = 0; i < subtreeCodes.size(); i++) {
            if (i > 0) codePlaceholders.append(",");
            codePlaceholders.append("?");
        }

        StringBuilder nickPlaceholders = new StringBuilder();
        for (int i = 0; i < subtreeNicks.size(); i++) {
            if (i > 0) nickPlaceholders.append(",");
            nickPlaceholders.append("?");
        }

        StringBuilder idPlaceholders = new StringBuilder();
        for (int i = 0; i < subtreeIds.size(); i++) {
            if (i > 0) idPlaceholders.append(",");
            idPlaceholders.append("?");
        }

        String sql = "SELECT 1 FROM users WHERE nick_name = ? AND (" +
                (subtreeCodes.isEmpty() ? "0=1" : "referral_code IN (" + codePlaceholders + ")") +
                " OR " +
                (subtreeNicks.isEmpty() ? "0=1" : "parrentUser IN (" + nickPlaceholders + ")") +
                " OR " +
                (subtreeIds.isEmpty() ? "0=1" : "parent_agent_id IN (" + idPlaceholders + ")") +
                ") LIMIT 1";

        try (Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            int index = 1;
            stm.setString(index++, usernameToFind);
            for (String code : subtreeCodes) {
                stm.setString(index++, code);
            }
            for (String nick : subtreeNicks) {
                stm.setString(index++, nick);
            }
            for (Integer id : subtreeIds) {
                stm.setInt(index++, id);
            }
            try (ResultSet rs = stm.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        JSONObject response = new JSONObject();

        try {
            String rc = request.getParameter("rc");
            String usernameToFind = request.getParameter("nn");

            if (rc == null || rc.isEmpty() || usernameToFind == null || usernameToFind.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1002");
                response.put("message", "Missing rc or target nickname");
                return response.toString();
            }

            AgentDAO agentDAO = new AgentDAOImpl();
            UserAgentModel requesterAgent = resolveRequesterAgent(agentDAO, rc);
            if (requesterAgent == null) {
                response.put("success", false);
                response.put("errorCode", "1002");
                response.put("message", "Agent not found for rc=" + rc);
                return response.toString();
            }

            UserAgentModel targetAgent = agentDAO.DetailUserAgentByNickName(usernameToFind);
            // SUN-1198/SUN-1192: legacy rows can have useragent.nickname != users.nick_name.
            // Support both user_name-keyed and nick_name-keyed inputs before falling
            // through to the non-agent "User" defaults.
            if (targetAgent == null) {
                try {
                    targetAgent = agentDAO.DetailUserAgentByUserName(usernameToFind);
                } catch (Exception ignored) {
                    // fall through to the canonical users.nick_name -> user_name lookup
                }
            }
            if (targetAgent == null) {
                try (Connection fallbackConn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname");
                     PreparedStatement fallbackPs = fallbackConn.prepareStatement(
                             "SELECT user_name FROM users WHERE nick_name = ? LIMIT 1")) {
                    fallbackPs.setString(1, usernameToFind);
                    try (ResultSet fallbackRs = fallbackPs.executeQuery()) {
                        if (fallbackRs.next()) {
                            String userName = fallbackRs.getString("user_name");
                            if (userName != null && !userName.trim().isEmpty()) {
                                targetAgent = agentDAO.DetailUserAgentByUserName(userName);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("DetailMemberOfAgency: username fallback failed for nn=" + usernameToFind, e);
                }
            }
            boolean isAgent = false;

            if (targetAgent != null) {
                if (requesterAgent.getNickname() != null &&
                        requesterAgent.getNickname().equalsIgnoreCase(targetAgent.getNickname())) {
                    isAgent = true;
                } else if (csvContainsId(targetAgent.getAncestors(), requesterAgent.getId())) {
                    isAgent = true;
                }
            }

            if (!isAgent && !isUserInAgentTree(requesterAgent, usernameToFind)) {
                response.put("success", false);
                response.put("errorCode", "1002");
                response.put("message", "User not found or not in your downline");
                return response.toString();
            }

            JSONObject userData = new JSONObject();
            String directParentNickname = "";
            String directParentCode = "";
            String playerReferralCode = "";
            try (Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                PreparedStatement stm = conn.prepareStatement(
                        "SELECT id, user_name, nick_name, vin, 0 AS xu, create_time, email, mobile, client, last_login, " + // SUN-13xx: xu dropped, kept alias for response parity
                        "parrentUser, referral_code " +
                        "FROM users WHERE nick_name = ?");
                stm.setString(1, usernameToFind);
                ResultSet rs = stm.executeQuery();
                if (rs.next()) {
                    userData.put("id", rs.getLong("id"));
                    userData.put("user_name", rs.getString("user_name"));
                    userData.put("nick_name", rs.getString("nick_name"));
                    long dbVin = rs.getLong("vin");
                    long dbXu  = 0L;
                    // SUN-842 pattern (same as MR #82 on c=9460): overlay
                    // live balance from Hazelcast users cache because
                    // UserServiceImpl.updateMoney writes cache first, DB second
                    // (via RMQ cmd=16). Without this overlay the agency portal
                    // detail page lags the game HUD for a few seconds per bet.
                    try {
                        com.hazelcast.core.IMap<String, com.vinplay.vbee.common.models.cache.UserCacheModel> usersMap =
                                com.vinplay.vbee.common.hazelcast.HazelcastClientFactory.getInstance().getMap("users");
                        com.vinplay.vbee.common.models.cache.UserCacheModel cached =
                                usersMap.get(rs.getString("nick_name"));
                        if (cached != null) {
                            dbVin = cached.getVin();
                            dbXu  = cached.getXu();
                        }
                    } catch (Exception liveLookupErr) {
                        logger.warn("DetailMemberOfAgency: users cache overlay failed for " + rs.getString("nick_name")
                                + " — falling back to DB values", liveLookupErr);
                    }
                    userData.put("vin", dbVin);
                    userData.put("xu",  dbXu);
                    userData.put("create_time", rs.getString("create_time"));

                    String mobile = rs.getString("mobile");
                    if (mobile != null && mobile.length() > 5) {
                        mobile = mobile.substring(0, 3) + "***" + mobile.substring(mobile.length() - 2);
                    }
                    userData.put("mobile", mobile != null ? mobile : "");
                    userData.put("email", rs.getString("email") != null && !rs.getString("email").isEmpty() ? "***@***" : "");
                    userData.put("ip_register", rs.getString("client") != null ? rs.getString("client") : "");
                    userData.put("last_login_ip", rs.getString("client") != null ? rs.getString("client") : "");
                    userData.put("last_login_time", rs.getString("last_login") != null ? rs.getString("last_login") : "");
                    directParentNickname = safe(rs.getString("parrentUser"));
                    playerReferralCode = safe(rs.getString("referral_code"));
                    directParentCode = playerReferralCode;
                }
                rs.close();
                stm.close();
            }

            if (userData.length() == 0 && !isAgent) {
                response.put("success", false);
                response.put("errorCode", "1002");
                response.put("message", "User not found");
                return response.toString();
            }

            // SUN-1192: when the lookup by nick_name miss but targetAgent exists,
            // try a second lookup keyed on user_name = useragent.username before
            // giving up. Reason: 8 agents on 2026-04-29 have a divergent
            // useragent.nickname vs users.nick_name (e.g. nickname='Nguyenthekr'
            // but the row in users has nick_name='SunKr_56789'). The previous
            // code ran the SpecialAccount fallback below and returned vin=0
            // even though the player wallet exists — making the same agent
            // show two different balances depending on which name the FE used.
            if (!userData.has("nick_name") && targetAgent != null
                    && targetAgent.getUsername() != null && !targetAgent.getUsername().isEmpty()) {
                try (Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname");
                     PreparedStatement stm = conn.prepareStatement(
                             "SELECT id, user_name, nick_name, vin, 0 AS xu, create_time, email, mobile, client, last_login, " // SUN-13xx: xu dropped, kept alias for response parity
                             + "parrentUser, referral_code FROM users WHERE user_name = ?")) {
                    stm.setString(1, targetAgent.getUsername());
                    try (ResultSet rs = stm.executeQuery()) {
                        if (rs.next()) {
                            userData.put("id", rs.getLong("id"));
                            userData.put("user_name", rs.getString("user_name"));
                            userData.put("nick_name", rs.getString("nick_name"));
                            long dbVin = rs.getLong("vin");
                            long dbXu  = 0L;
                            try {
                                com.hazelcast.core.IMap<String, com.vinplay.vbee.common.models.cache.UserCacheModel> usersMap =
                                        com.vinplay.vbee.common.hazelcast.HazelcastClientFactory.getInstance().getMap("users");
                                com.vinplay.vbee.common.models.cache.UserCacheModel cached = usersMap.get(rs.getString("nick_name"));
                                if (cached != null) {
                                    dbVin = cached.getVin();
                                    dbXu  = cached.getXu();
                                }
                            } catch (Exception liveLookupErr) {
                                logger.warn("DetailMemberOfAgency [SUN-1192]: users cache overlay failed for " + rs.getString("nick_name"), liveLookupErr);
                            }
                            userData.put("vin", dbVin);
                            userData.put("xu",  dbXu);
                            userData.put("create_time", rs.getString("create_time"));
                            String mobile = rs.getString("mobile");
                            if (mobile != null && mobile.length() > 5) {
                                mobile = mobile.substring(0, 3) + "***" + mobile.substring(mobile.length() - 2);
                            }
                            userData.put("mobile", mobile != null ? mobile : "");
                            userData.put("email", rs.getString("email") != null && !rs.getString("email").isEmpty() ? "***@***" : "");
                            userData.put("ip_register", rs.getString("client") != null ? rs.getString("client") : "");
                            userData.put("last_login_ip", rs.getString("client") != null ? rs.getString("client") : "");
                            userData.put("last_login_time", rs.getString("last_login") != null ? rs.getString("last_login") : "");
                            directParentNickname = safe(rs.getString("parrentUser"));
                            playerReferralCode = safe(rs.getString("referral_code"));
                            directParentCode = playerReferralCode;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("DetailMemberOfAgency [SUN-1192]: user_name fallback failed for " + targetAgent.getUsername(), e);
                }
            }

            // Final fallback for SpecialAccount or agents that genuinely don't exist in the users table
            if (!userData.has("nick_name") && targetAgent != null) {
                userData.put("id", targetAgent.getId());
                userData.put("user_name", safe(targetAgent.getUsername()));
                userData.put("nick_name", safe(targetAgent.getNickname()));
                userData.put("vin", 0);
                userData.put("xu", 0);
                userData.put("create_time", "");
                userData.put("mobile", "");
                userData.put("email", "");
                userData.put("ip_register", "");
                userData.put("last_login_ip", "");
                userData.put("last_login_time", targetAgent.getLast_login_time() != null ? targetAgent.getLast_login_time().toString() : "");
            }

            // Always emit level + agent fields with safe defaults for stable FE schema.
            // level: 1=TĐL, 2=ĐL1, 3=ĐL2, 4=User thường (không phải agent)
            int level = 4;
            if (targetAgent != null) {
                level = targetAgent.getLevel(); // 1=TĐL, 2=ĐL1, 3=ĐL2
                ParentInfo parentInfo = resolveParentInfoForAgent(targetAgent);
                directParentNickname = parentInfo.nickname;
                directParentCode = parentInfo.code;
                userData.put("bank_account_owner", targetAgent.getNameaccount() != null ? targetAgent.getNameaccount() : "");
                userData.put("status", targetAgent.getActive() == 1 ? "HD" : "KHOA");
                userData.put("login_times", targetAgent.getLogin_times());
                userData.put("referral_code", safe(targetAgent.getCode()));
                if (targetAgent.getLast_login_time() != null) {
                    userData.put("last_login_time", targetAgent.getLast_login_time().toString());
                }
            } else {
                userData.put("bank_account_owner", "");
                userData.put("status", "HD");
                userData.put("login_times", 0);
                userData.put("referral_code", playerReferralCode);
            }
            userData.put("level", level);
            userData.put("parent_nickname", directParentNickname);
            userData.put("parent_code", directParentCode);

            String effectiveNickname = userData.optString("nick_name", usernameToFind);
            if (effectiveNickname == null || effectiveNickname.trim().isEmpty()) {
                effectiveNickname = targetAgent != null ? safe(targetAgent.getNickname()) : usernameToFind;
            }

            // can_promote — delegated to shared AgentPermissionService (issue #8)
            boolean canPromote = false;
            if (targetAgent == null && requesterAgent.getLevel() != null && requesterAgent.getLevel() < 3) {
                canPromote = com.vinplay.dal.service.AgentPermissionService
                        .canPromote(requesterAgent.getId(), requesterAgent.getLevel(), effectiveNickname);
            }
            userData.put("can_promote", canPromote);

            // Agency wallet balance and Credit wallet balance — only agents have them
            long creditWalletBalance = 0;
            if (targetAgent != null && targetAgent.getId() != null) {
                userData.put("agency_wallet_balance",
                        com.vinplay.dal.rebate.RebateService.getAgencyWalletBalance(targetAgent.getId()));
                
                try {
                    com.vinplay.dal.dao.CreditWalletDao creditWalletDao = new com.vinplay.dal.dao.impl.CreditWalletDaoImpl();
                    creditWalletBalance = creditWalletDao.getBalance(targetAgent.getId());
                } catch (Exception e) {
                    logger.warn("DetailMemberOfAgencyProcessor: credit_wallet lookup failed for target agent " + targetAgent.getId(), e);
                }
            } else {
                userData.put("agency_wallet_balance", 0);
            }
            userData.put("credit_wallet_balance", creditWalletBalance);

            try (Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Get recharge_money + t_nap/t_rut from users table (same source as c=9839)
                long rechargeMoney = 0, tNap = 0, tRut = 0;
                try (PreparedStatement uStm = conn.prepareStatement(
                        "SELECT 0 AS recharge_money, t_nap, t_rut FROM users WHERE nick_name = ?")) { // SUN-13xx: recharge_money dropped, kept alias for response parity
                    uStm.setString(1, effectiveNickname);
                    try (ResultSet uRs = uStm.executeQuery()) {
                        if (uRs.next()) {
                            rechargeMoney = uRs.getLong("recharge_money");
                            tNap = uRs.getLong("t_nap");
                            tRut = uRs.getLong("t_rut");
                        }
                    }
                }
                long totalDeposit = Math.max(rechargeMoney, tNap);
                long userVin = userData.has("vin") ? userData.getLong("vin") : 0;
                userData.put("total_deposit", totalDeposit);
                userData.put("total_withdraw", tRut);
                userData.put("net_profit", totalDeposit - tRut);
                userData.put("profit", totalDeposit - tRut - userVin);
            }

            JSONArray activeCommissions = new JSONArray();
            List<JSONObject> commissionsList = new ArrayList<>();
            try (Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT game_key, rate FROM game_commission_rate WHERE agent_nickname = ? AND rate > 0 ORDER BY game_key ASC")) {
                    ps.setString(1, effectiveNickname);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject rateObj = new JSONObject();
                            String gameKey = rs.getString("game_key");
                            rateObj.put("game_key", gameKey);
                            // Agency FE requires 2-decimal-place rate ("1.20", not "1.2").
                            // org.json.JSONObject.numberToString() strips trailing zeros
                            // from every Number subtype (BigDecimal included), so we cannot
                            // preserve scale via a numeric field — emit as a formatted String.
                            java.math.BigDecimal _r = rs.getBigDecimal("rate");
                            rateObj.put("rate", _r != null
                                    ? _r.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
                                    : "0.00");
                            if (gameKey != null) {
                                if (gameKey.startsWith("offline_cat_")) {
                                    rateObj.put("game_name", gameKey);
                                    rateObj.put("category", gameKey.substring("offline_cat_".length()));
                                    rateObj.put("category_scope", "OFFLINE");
                                } else if (gameKey.startsWith("live_cat_")) {
                                    rateObj.put("game_name", gameKey);
                                    rateObj.put("category", gameKey.substring("live_cat_".length()));
                                    rateObj.put("category_scope", "LIVE");
                                } else {
                                    rateObj.put("game_name", com.vinplay.api.backend.processors.agent.ListGameCommissionConfigProcessor.getGameMap().getOrDefault(gameKey, gameKey));
                                }
                            }
                            commissionsList.add(rateObj);
                        }
                    }
                }
                commissionsList.sort((o1, o2) -> {
                    String k1 = o1.optString("game_key", "");
                    String k2 = o2.optString("game_key", "");
                    return com.vinplay.api.backend.processors.agent.ListGameCommissionConfigProcessor.compareDynamicKeys(k1, k2);
                });
                for (JSONObject obj : commissionsList) {
                    activeCommissions.put(obj);
                }
            } catch (Exception e) {
                logger.warn("DetailMemberOfAgency: Failed to load active commissions for " + effectiveNickname, e);
            }
            userData.put("commissions", activeCommissions);

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", userData);

        } catch (Exception e) {
            logger.error("Error in DetailMemberOfAgencyProcessor", e);
            response.put("success", false);
            response.put("errorCode", "1001");
            response.put("message", e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
        return response.toString();
    }
}
