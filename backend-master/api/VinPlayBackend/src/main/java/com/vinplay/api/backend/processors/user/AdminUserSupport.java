package com.vinplay.api.backend.processors.user;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.api.backend.auth.AdminAuthHelper;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.StatusUser;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;

public final class AdminUserSupport {
    private static final String USERNAME_REGEX = "^[a-zA-Z0-9]{6,16}$";
    private static final String NICKNAME_REGEX = "^[a-zA-Z0-9_]{6,16}$";
    private static final String PASSWORD_REGEX = "^[a-zA-Z0-9@#$%^&*]{6,16}$";
    private static final String EMAIL_REGEX = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    private static final String MOBILE_REGEX = "^\\+?\\d{9,15}$";
    private static final String IDENTIFICATION_REGEX = "^[0-9]{9,12}$";
    private static final int ADDRESS_MAX_LENGTH = 100;
    private static final List<String> NICKNAME_CONTAIN_INVALID = Arrays.asList(
            "admin", "ad_min", "master", "vinplay", "vin_play", "19006896");
    private static final List<String> NICKNAME_START_INVALID = Arrays.asList(
            "gamemaster", "gm", "bot", "mod", "bocongan", "bo_cong_an", "hcm", "hochiminh",
            "ho_chi_minh", "dcs", "dangcongsan", "dang_cong_san", "dmcs", "nhacai", "hethong",
            "mas_ter", "game_master", "daily", "dai_ly", "fuck", "matlon", "12lieugiai");

    static final class UserRef {
        long id;
        String userName;
        String nickName;
    }

    static final class AgentRef {
        Integer id;
        String code;
        String nickname;
    }

    private static volatile Boolean agentCodeHistoryTableExists = null;

    public static final class AgentDownlineInfo {
        public final List<Integer> agentIds;
        public final List<String> referralCodes;

        public AgentDownlineInfo(List<Integer> agentIds, List<String> referralCodes) {
            this.agentIds = agentIds;
            this.referralCodes = referralCodes;
        }

        public boolean isEmpty() {
            return agentIds.isEmpty() && referralCodes.isEmpty();
        }
    }

    private AdminUserSupport() {
    }

    public static AgentDownlineInfo loadDownlineAgentInfo(String nickname) {
        List<Integer> agentIds = new java.util.ArrayList<>();
        List<String> refCodes = new java.util.ArrayList<>();
        if (!isNotBlank(nickname)) {
            return new AgentDownlineInfo(agentIds, refCodes);
        }
        try (Connection adminConn = getAdminConnection()) {
            String getAgentSql = "SELECT id, code FROM useragent WHERE nickname = ?";
            int matchedAgentId = -1;
            String matchedAgentCode = null;
            try (PreparedStatement ps = adminConn.prepareStatement(getAgentSql)) {
                ps.setString(1, nickname.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        matchedAgentId = rs.getInt("id");
                        matchedAgentCode = rs.getString("code");
                    }
                }
            }
            if (matchedAgentId > 0) {
                agentIds.add(matchedAgentId);
                if (matchedAgentCode != null && !matchedAgentCode.isEmpty()) {
                    refCodes.add(matchedAgentCode);
                }
                String getDescendantsSql = "SELECT id, code FROM useragent WHERE FIND_IN_SET(?, IFNULL(ancestors,'')) > 0";
                try (PreparedStatement ps = adminConn.prepareStatement(getDescendantsSql)) {
                    ps.setInt(1, matchedAgentId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            agentIds.add(rs.getInt("id"));
                            String c = rs.getString("code");
                            if (c != null && !c.isEmpty()) {
                                refCodes.add(c);
                            }
                        }
                    }
                }
                // Check agent_code_history existence with memory cache
                boolean hasHist = false;
                if (agentCodeHistoryTableExists != null) {
                    hasHist = agentCodeHistoryTableExists;
                } else {
                    try (PreparedStatement ps = adminConn.prepareStatement("SHOW TABLES LIKE 'agent_code_history'");
                         ResultSet rs = ps.executeQuery()) {
                        hasHist = rs.next();
                        agentCodeHistoryTableExists = hasHist;
                    } catch (Exception ignored) {
                        agentCodeHistoryTableExists = false;
                    }
                }
                if (hasHist) {
                    try {
                        String histSql = "SELECT h.old_code FROM agent_code_history h " +
                                         "JOIN useragent ua ON ua.id = h.agent_id " +
                                         "WHERE (ua.id = ? OR FIND_IN_SET(?, IFNULL(ua.ancestors,'')) > 0) " +
                                         "AND h.old_code IS NOT NULL AND h.old_code <> ''";
                        try (PreparedStatement ps = adminConn.prepareStatement(histSql)) {
                            ps.setInt(1, matchedAgentId);
                            ps.setInt(2, matchedAgentId);
                            try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                    String c = rs.getString("old_code");
                                    if (c != null && !c.isEmpty()) {
                                        refCodes.add(c);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        org.apache.log4j.Logger.getLogger("backend").warn("AdminUserSupport: agent_code_history fetch failed", e);
                    }
                }
            }
        } catch (Exception e) {
            org.apache.log4j.Logger.getLogger("backend").warn("AdminUserSupport: loadDownlineAgentInfo failed", e);
        }
        return new AgentDownlineInfo(agentIds, refCodes);
    }

    static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 error", e);
        }
    }

    static String normalizePassword(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.length() == 32 && trimmed.matches("[a-fA-F0-9]+")) {
            return trimmed.toLowerCase();
        }
        return md5(trimmed);
    }

    static UserRef findUser(Connection conn, String idStr, String userName, String nickName) throws Exception {
        String sql = "SELECT id, user_name, nick_name FROM users WHERE "
                + (isNotBlank(idStr) ? "id = ?" : isNotBlank(userName) ? "user_name = ?" : "nick_name = ?")
                + " LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (isNotBlank(idStr)) {
                ps.setLong(1, Long.parseLong(idStr));
            } else if (isNotBlank(userName)) {
                ps.setString(1, userName.trim());
            } else {
                ps.setString(1, nickName.trim());
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                UserRef ref = new UserRef();
                ref.id = rs.getLong("id");
                ref.userName = rs.getString("user_name");
                ref.nickName = rs.getString("nick_name");
                return ref;
            }
        }
    }

    static AgentRef findAgentById(Connection conn, Integer agentId) throws Exception {
        if (agentId == null || agentId <= 0) {
            return null;
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, code, nickname FROM vinplay_admin.useragent WHERE id = ? LIMIT 1")) {
            ps.setInt(1, agentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                AgentRef ref = new AgentRef();
                ref.id = rs.getInt("id");
                ref.code = rs.getString("code");
                ref.nickname = rs.getString("nickname");
                return ref;
            }
        }
    }

    static AgentRef findAgentByCode(Connection conn, String referralCode) throws Exception {
        if (!isNotBlank(referralCode)) {
            return null;
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, code, nickname FROM vinplay_admin.useragent WHERE code = ? LIMIT 1")) {
            ps.setString(1, referralCode.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                AgentRef ref = new AgentRef();
                ref.id = rs.getInt("id");
                ref.code = rs.getString("code");
                ref.nickname = rs.getString("nickname");
                return ref;
            }
        }
    }

    static AgentRef resolveBinding(Connection conn, String referralCode, String parentAgentIdStr, boolean fallbackCompanyAgent)
            throws Exception {
        AgentRef fromId = null;
        AgentRef fromCode = null;

        if (isNotBlank(parentAgentIdStr)) {
            fromId = findAgentById(conn, Integer.parseInt(parentAgentIdStr.trim()));
            if (fromId == null) {
                throw new IllegalArgumentException("parent_agent_id not found");
            }
        }

        if (isNotBlank(referralCode)) {
            fromCode = findAgentByCode(conn, referralCode.trim());
            if (fromCode == null) {
                throw new IllegalArgumentException("referral_code not found");
            }
        }

        if (fromId != null && fromCode != null && !fromId.id.equals(fromCode.id)) {
            throw new IllegalArgumentException("parent_agent_id and referral_code do not match");
        }

        if (fromId != null) {
            return fromId;
        }
        if (fromCode != null) {
            return fromCode;
        }
        if (!fallbackCompanyAgent) {
            return null;
        }
        return findAgentByCode(conn, "1");
    }

    public static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    static boolean hasAnyIdentity(String idStr, String userName, String nickName) {
        return isNotBlank(idStr) || isNotBlank(userName) || isNotBlank(nickName);
    }

    public static String nullableTrim(String value) {
        return isNotBlank(value) ? value.trim() : null;
    }

    static int parseTinyInt(String value, int defaultValue) {
        if (!isNotBlank(value)) {
            return defaultValue;
        }
        return Integer.parseInt(value.trim());
    }

    static Integer parseOptionalInteger(String value) {
        if (!isNotBlank(value)) {
            return null;
        }
        return Integer.parseInt(value.trim());
    }

    static String validateUserName(String userName) {
        if (!isNotBlank(userName)) {
            return "un is required";
        }
        if (!userName.matches(USERNAME_REGEX)) {
            return "Invalid username format";
        }
        return null;
    }

    static String validateNickName(String nickName) {
        if (!isNotBlank(nickName)) {
            return "Nickname cannot be empty";
        }
        if (!nickName.matches(NICKNAME_REGEX)) {
            return "Invalid nickname format";
        }
        String lower = nickName.toLowerCase();
        for (String entry : NICKNAME_CONTAIN_INVALID) {
            if (lower.contains(entry)) {
                return "Nickname contains forbidden keyword";
            }
        }
        for (String entry : NICKNAME_START_INVALID) {
            if (lower.startsWith(entry)) {
                return "Nickname starts with forbidden prefix";
            }
        }
        return null;
    }

    static String validatePassword(String password) {
        // SUN-851: delegate to the centralised PasswordPolicy so admin user
        // creation uses the same rule as player change-password and
        // registration. Old PASSWORD_REGEX (^[a-zA-Z0-9@#$%^&*]{6,16}$) was
        // narrower than what QC now allows — PasswordPolicy widens the set
        // and lifts max length to 32.
        return com.vinplay.vbee.common.utils.PasswordPolicy.validate(password);
    }

    static String validateEmail(String email) {
        if (!isNotBlank(email)) {
            return null;
        }
        if (email.length() >= 100 || !email.matches(EMAIL_REGEX)) {
            return "Invalid email format";
        }
        return null;
    }

    static String validateMobile(String mobile) {
        if (!isNotBlank(mobile)) {
            return null;
        }
        if (!mobile.matches(MOBILE_REGEX)) {
            return "Invalid mobile format";
        }
        return null;
    }

    static String validateIdentification(String identification) {
        if (!isNotBlank(identification)) {
            return null;
        }
        if ((identification.length() != 9 && identification.length() != 12)
                || !identification.matches(IDENTIFICATION_REGEX)) {
            return "Invalid identification format";
        }
        return null;
    }

    static String validateAddress(String address) {
        if (!isNotBlank(address)) {
            return null;
        }
        if (address.length() > ADDRESS_MAX_LENGTH) {
            return "Address is too long";
        }
        return null;
    }

    static String validateTinyIntFlag(String value, String fieldName) {
        if (!isNotBlank(value)) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed != 0 && parsed != 1) {
                return fieldName + " must be 0 or 1";
            }
            return null;
        } catch (NumberFormatException e) {
            return fieldName + " must be 0 or 1";
        }
    }

    static String validateZeroOnlyFlag(String value, String fieldName) {
        String validationError = validateTinyIntFlag(value, fieldName);
        if (validationError != null) {
            return validationError;
        }
        if (!isNotBlank(value)) {
            return null;
        }
        if (Integer.parseInt(value.trim()) != 0) {
            return fieldName + " cannot be enabled via user CRUD API";
        }
        return null;
    }

    static String validateInteger(String value, String fieldName) {
        if (!isNotBlank(value)) {
            return null;
        }
        try {
            Integer.parseInt(value.trim());
            return null;
        } catch (NumberFormatException e) {
            return fieldName + " must be an integer";
        }
    }

    static String validateNonNegativeInteger(String value, String fieldName) {
        String validationError = validateInteger(value, fieldName);
        if (validationError != null) {
            return validationError;
        }
        if (!isNotBlank(value)) {
            return null;
        }
        if (Integer.parseInt(value.trim()) < 0) {
            return fieldName + " must be >= 0";
        }
        return null;
    }

    static String validateUserStatus(String value) {
        String validationError = validateNonNegativeInteger(value, "status");
        if (validationError != null) {
            return validationError;
        }
        if (!isNotBlank(value)) {
            return null;
        }
        int status = Integer.parseInt(value.trim());
        int allowedMask = StatusUser.BAN_LOGIN
                | StatusUser.BAN_CASH_OUT
                | StatusUser.CAN_LOGIN_SANDBOX
                | StatusUser.BAN_TRANSFER_MONEY
                | StatusUser.HAS_MOBILE_SECURITY
                | StatusUser.HAS_EMAIL_SECURITY
                | StatusUser.HAS_APP_SECURITY
                | StatusUser.HAS_LOGIN_SECURITY;
        if ((status & ~allowedMask) != 0) {
            return "status contains unsupported bits";
        }
        return null;
    }

    static String validateUnsupportedField(String value, String fieldName) {
        if (!isNotBlank(value)) {
            return null;
        }
        return fieldName + " is not supported by current user schema";
    }

    static boolean existsByUserName(Connection conn, String userName, Long excludeId) throws Exception {
        String sql = "SELECT id FROM users WHERE user_name = ?" + (excludeId != null ? " AND id <> ?" : "") + " LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userName);
            if (excludeId != null) {
                ps.setLong(2, excludeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    static boolean existsByNickName(Connection conn, String nickName, Long excludeId) throws Exception {
        String sql = "SELECT id FROM users WHERE nick_name = ?" + (excludeId != null ? " AND id <> ?" : "") + " LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nickName);
            if (excludeId != null) {
                ps.setLong(2, excludeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    static Connection getUserConnection() throws Exception {
        return ConnectionPool.getInstance().getConnection("mysqlpoolname");
    }

    static Connection getAdminConnection() throws Exception {
        return ConnectionPool.getInstance().getConnection("mysqlpool_admin");
    }

    public static String requireAdmin(javax.servlet.http.HttpServletRequest request, JSONObject response) {
        String aat = nullableTrim(request.getParameter("aat"));
        if (!isNotBlank(aat)) {
            response.put("success", false);
            response.put("errorCode", "1001");
            response.put("message", "aat required");
            return null;
        }
        if (!AdminAuthHelper.verifyToken(aat)) {
            response.put("success", false);
            response.put("errorCode", "1001");
            response.put("message", "Unauthorized");
            return null;
        }
        String actor = getAdminUsernameByToken(aat);
        return isNotBlank(actor) ? actor : "system";
    }

    static String getAdminUsernameByToken(String aat) {
        if (!isNotBlank(aat)) {
            return null;
        }
        try {
            HazelcastInstance hz = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = hz.getMap("cacheToken");
            return tokenMap.get(aat);
        } catch (Exception e) {
            return null;
        }
    }

    static void logUserCrudAction(String actor, String action, String accountName, String reason, String status) {
        try (Connection conn = getAdminConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO log_admin (username, action, quantity, money, account_name, money_type, reason, status) " +
                             "VALUES (?,?,?,?,?,?,?,?)")) {
            ps.setString(1, actor);
            ps.setString(2, action);
            ps.setInt(3, 0);
            ps.setLong(4, 0L);
            ps.setString(5, accountName);
            ps.setString(6, "user");
            ps.setString(7, reason);
            ps.setString(8, status);
            ps.executeUpdate();
        } catch (Exception ignored) {
        }
    }
}
