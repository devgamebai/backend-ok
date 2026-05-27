package com.vinplay.api.backend.processors.commission;

import com.vinplay.dal.entities.agent.AgentCommissionDaily;
import com.vinplay.dal.entities.agent.UserAgentModel;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.statics.MongoCollections;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/**
 * Shared commission calculation + DB loaders for CalculateAgentCommissionProcessor and realtime APIs.
 */
public final class CommissionCalcHelper {

    private CommissionCalcHelper() {
    }

    private static final Logger logger = Logger.getLogger("backend");

    public static final Map<Integer, String> GAME_COLUMN_MAP = new LinkedHashMap<>();

    static {
        GAME_COLUMN_MAP.put(Games.MINI_POKER.getId(), "minipoker");
        GAME_COLUMN_MAP.put(Games.TAI_XIU.getId(), "taixiu");
        GAME_COLUMN_MAP.put(Games.BAU_CUA.getId(), "baucua");
        GAME_COLUMN_MAP.put(Games.CAO_THAP.getId(), "caothap");
        GAME_COLUMN_MAP.put(Games.POKE_GO.getId(), "slot_pokemon");
        GAME_COLUMN_MAP.put(Games.SAM.getId(), "sam");
        GAME_COLUMN_MAP.put(Games.BA_CAY.getId(), "bacay");
        GAME_COLUMN_MAP.put(Games.BINH.getId(), "binh");
        GAME_COLUMN_MAP.put(Games.TLMN.getId(), "tlmn");
        GAME_COLUMN_MAP.put(Games.TA_LA.getId(), "tala");
        GAME_COLUMN_MAP.put(Games.LIENG.getId(), "lieng");
        GAME_COLUMN_MAP.put(Games.XI_TO.getId(), "xito");
        GAME_COLUMN_MAP.put(Games.XOC_DIA.getId(), "xocdia");
        GAME_COLUMN_MAP.put(Games.BAI_CAO.getId(), "baicao");
        GAME_COLUMN_MAP.put(Games.POKER.getId(), "poker");
        GAME_COLUMN_MAP.put(Games.XI_DZACH.getId(), "xidzach");
        GAME_COLUMN_MAP.put(Games.HAM_CA_MAP.getId(), "hamcamap");
        GAME_COLUMN_MAP.put(Games.TAI_XIU_SICBO.getId(), "taixiu_sicbo");
        GAME_COLUMN_MAP.put(Games.OVER_UNDER.getId(), "over_under");
        GAME_COLUMN_MAP.put(Games.SPARTAN.getId(), "slot_thantai");
        GAME_COLUMN_MAP.put(Games.AUDITION.getId(), "slot_taydu");
        GAME_COLUMN_MAP.put(Games.SAMTRUYEN.getId(), "samtruyen");
        GAME_COLUMN_MAP.put(Games.RANGE_ROVER.getId(), "range_rover");
        GAME_COLUMN_MAP.put(Games.MAYBACH.getId(), "slot_thethao");
        GAME_COLUMN_MAP.put(Games.TAMHUNG.getId(), "slot_angrybird");
        GAME_COLUMN_MAP.put(Games.BENLEY.getId(), "slot_bitcoin");
        GAME_COLUMN_MAP.put(Games.ROLL_ROYE.getId(), "slot_thanbai");
        GAME_COLUMN_MAP.put(Games.AG_GAMES.getId(), "ag");
        GAME_COLUMN_MAP.put(Games.WM_GAMES.getId(), "wm");
        GAME_COLUMN_MAP.put(Games.IBC2_GAMES.getId(), "ibc");
        GAME_COLUMN_MAP.put(Games.CMD_GAMES.getId(), "cmd");
        GAME_COLUMN_MAP.put(Games.TAI_XIU_ST.getId(), "taixiu_st");
        GAME_COLUMN_MAP.put(Games.CHIEM_TINH.getId(), "slot_chiemtinh");
        GAME_COLUMN_MAP.put(Games.SHOT_FISH.getId(), "fish");
        GAME_COLUMN_MAP.put(Games.EBET_GAMES.getId(), "ebet");
        GAME_COLUMN_MAP.put(Games.SBO_GAMES.getId(), "sbo");
        GAME_COLUMN_MAP.put(Games.SEXYGIRL.getId(), "sexygirl");
        GAME_COLUMN_MAP.put(Games.BIKINI.getId(), "slot_bikini");
        GAME_COLUMN_MAP.put(Games.GALAXY.getId(), "slot_galaxy");
        GAME_COLUMN_MAP.put(Games.LODE.getId(), "lode");
    }

    private static final String CATEGORY_CASINO = "casino";
    private static final String CATEGORY_SPORT = "sport";
    private static final String CATEGORY_GAME = "game";

    public static String getGameCategory(int gameId) {
        if (gameId == Games.WM_GAMES.getId() || gameId == Games.AG_GAMES.getId() || gameId == Games.EBET_GAMES.getId()) {
            return CATEGORY_CASINO;
        }
        if (gameId == Games.IBC2_GAMES.getId() || gameId == Games.CMD_GAMES.getId() || gameId == Games.SBO_GAMES.getId()) {
            return CATEGORY_SPORT;
        }
        return CATEGORY_GAME;
    }

    /**
     * Key dùng cho map đại lý / referral / phân phối HH khi {@code code} có thể null hoặc rỗng:
     * ưu tiên {@code code}, không có thì {@code nickname}, cuối cùng {@code id:...}.
     */
    public static String agentBusinessKey(UserAgentModel a) {
        if (a == null) {
            return "";
        }
        if (a.getCode() != null && !a.getCode().trim().isEmpty()) {
            return a.getCode().trim();
        }
        if (a.getNickname() != null && !a.getNickname().trim().isEmpty()) {
            return a.getNickname().trim();
        }
        if (a.getId() != null) {
            return "id:" + a.getId();
        }
        return "";
    }

    public static List<AgentCommissionDaily.AgentDistribution> buildDistributionsForGame(
            UserAgentModel directAgent, double userRate, long betAmount,
            Map<String, UserAgentModel> codeToAgent,
            Map<Integer, UserAgentModel> idToAgent) {

        List<AgentCommissionDaily.AgentDistribution> distributions = new ArrayList<>();
        double prevRate = userRate;
        UserAgentModel currentAgent = directAgent;

        while (currentAgent != null) {
            double agentRate = currentAgent.getCommission_rate() != null ? currentAgent.getCommission_rate() : 0.0;
            double earnRate = agentRate - prevRate;
            if (earnRate < 0) earnRate = 0;
            long commission = (long) (earnRate / 100.0 * betAmount);

            distributions.add(new AgentCommissionDaily.AgentDistribution(
                    agentBusinessKey(currentAgent),
                    currentAgent.getNickname(),
                    currentAgent.getLevel() != null ? currentAgent.getLevel() : 0,
                    agentRate, earnRate, commission));

            prevRate = agentRate;

            if (currentAgent.getParentid() == null || currentAgent.getParentid() <= 0) {
                break;
            }
            currentAgent = idToAgent.get(currentAgent.getParentid());
        }
        return distributions;
    }

    public static List<AgentCommissionDaily.AgentDistribution> buildTotalDistributions(
            UserAgentModel directAgent, Map<String, Long> agentTotalCommission,
            long totalBet, Map<Integer, UserAgentModel> idToAgent) {

        List<AgentCommissionDaily.AgentDistribution> distributions = new ArrayList<>();
        UserAgentModel currentAgent = directAgent;

        while (currentAgent != null) {
            double agentRate = currentAgent.getCommission_rate() != null ? currentAgent.getCommission_rate() : 0.0;
            long commission = agentTotalCommission.getOrDefault(agentBusinessKey(currentAgent), 0L);
            double effectiveEarnRate = totalBet > 0 ? (commission * 100.0 / totalBet) : 0;

            distributions.add(new AgentCommissionDaily.AgentDistribution(
                    agentBusinessKey(currentAgent),
                    currentAgent.getNickname(),
                    currentAgent.getLevel() != null ? currentAgent.getLevel() : 0,
                    agentRate, effectiveEarnRate, commission));

            if (currentAgent.getParentid() == null || currentAgent.getParentid() <= 0) {
                break;
            }
            currentAgent = idToAgent.get(currentAgent.getParentid());
        }
        return distributions;
    }

    public static void loadAgents(Map<String, UserAgentModel> codeToAgent,
                                  Map<Integer, UserAgentModel> idToAgent,
                                  String agentCode) throws Exception {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
            String sql;
            PreparedStatement stm;
            if (agentCode != null && !agentCode.trim().isEmpty()) {
                sql = "SELECT * FROM useragent WHERE code = ?";
                stm = conn.prepareStatement(sql);
                stm.setString(1, agentCode);
                try (ResultSet rs = stm.executeQuery()) {
                    while (rs.next()) {
                        UserAgentModel agent = mapAgent(rs);
                        String bk = agentBusinessKey(agent);
                        if (!bk.isEmpty()) {
                            codeToAgent.put(bk, agent);
                        }
                        idToAgent.put(agent.getId(), agent);
                    }
                }
                stm.close();

                loadDescendants(conn, codeToAgent, idToAgent);
            } else {
                sql = "SELECT * FROM useragent WHERE active = 1";
                stm = conn.prepareStatement(sql);
                try (ResultSet rs = stm.executeQuery()) {
                    while (rs.next()) {
                        UserAgentModel agent = mapAgent(rs);
                        String bk = agentBusinessKey(agent);
                        if (!bk.isEmpty()) {
                            codeToAgent.put(bk, agent);
                        }
                        idToAgent.put(agent.getId(), agent);
                    }
                }
                stm.close();
            }
        }
    }

    /** Load every active agent into maps (for parent chain resolution in commission). */
    public static void loadAllAgentsForChain(Map<String, UserAgentModel> codeToAgent,
                                             Map<Integer, UserAgentModel> idToAgent) throws Exception {
        loadAgents(codeToAgent, idToAgent, null);
        // Debug dump for commission chain building
        if (codeToAgent == null) {
            logger.info("loadAllAgentsForChain: codeToAgent=null");
        } else {
            for (Map.Entry<String, UserAgentModel> e : codeToAgent.entrySet()) {
                UserAgentModel a = e.getValue();
            }
        }
        if (idToAgent == null) {
            logger.info("loadAllAgentsForChain: idToAgent=null");
        } else {
            for (Map.Entry<Integer, UserAgentModel> e : idToAgent.entrySet()) {
                UserAgentModel a = e.getValue();
            }
        }
    }

    /**
     * Danh sách tất cả đại lý active (để hiển thị bảng realtime — luôn có đủ dòng, kể cả không có mã code).
     */
    public static List<UserAgentModel> loadAllActiveAgentsList() throws Exception {
        List<UserAgentModel> list = new ArrayList<>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
            String sql = "SELECT * FROM useragent WHERE active = 1 ORDER BY id ASC";
            PreparedStatement stm = conn.prepareStatement(sql);
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAgent(rs));
                }
            }
            stm.close();
        }
        return list;
    }

    /** Tìm đại lý theo tham số ac (mã, nickname hoặc business key). */
    public static UserAgentModel resolveAgent(Map<String, UserAgentModel> codeToAgent, String ac) {
        if (ac == null || ac.trim().isEmpty()) {
            return null;
        }
        String t = ac.trim();
        UserAgentModel u = codeToAgent.get(t);
        if (u != null) {
            return u;
        }
        for (UserAgentModel x : codeToAgent.values()) {
            if (t.equalsIgnoreCase(agentBusinessKey(x))) {
                return x;
            }
            if (x.getCode() != null && t.equalsIgnoreCase(x.getCode().trim())) {
                return x;
            }
            if (x.getNickname() != null && t.equalsIgnoreCase(x.getNickname().trim())) {
                return x;
            }
        }
        return null;
    }

    /**
     * {@code users.nick_name} có phải tài khoản đại lý (bản ghi useragent) không.
     * Khớp nickname đại lý, username CMS, hoặc mã code — khác với chỉ so với đại lý "sở hữu" theo referral.
     */
    public static UserAgentModel resolveAgentByPlayerNick(Map<String, UserAgentModel> codeToAgent, String usersNickName) {
        if (usersNickName == null || usersNickName.trim().isEmpty() || codeToAgent == null) {
            return null;
        }
        String t = usersNickName.trim();
        for (UserAgentModel x : codeToAgent.values()) {
            if (x == null) {
                continue;
            }
            if (x.getNickname() != null && t.equalsIgnoreCase(x.getNickname().trim())) {
                return x;
            }
            if (x.getUsername() != null && t.equalsIgnoreCase(x.getUsername().trim())) {
                return x;
            }
            if (x.getCode() != null && t.equalsIgnoreCase(x.getCode().trim())) {
                return x;
            }
        }
        return null;
    }

    private static void loadDescendants(Connection conn,
                                        Map<String, UserAgentModel> codeToAgent,
                                        Map<Integer, UserAgentModel> idToAgent) throws Exception {
        List<Integer> parentIds = new ArrayList<>(idToAgent.keySet());
        while (!parentIds.isEmpty()) {
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < parentIds.size(); i++) {
                if (i > 0) placeholders.append(",");
                placeholders.append("?");
            }
            String sql = "SELECT * FROM useragent WHERE parentid IN (" + placeholders + ") AND active = 1";
            PreparedStatement stm = conn.prepareStatement(sql);
            for (int i = 0; i < parentIds.size(); i++) {
                stm.setInt(i + 1, parentIds.get(i));
            }

            List<Integer> newParentIds = new ArrayList<>();
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    UserAgentModel agent = mapAgent(rs);
                    if (agent.getId() != null && !idToAgent.containsKey(agent.getId())) {
                        String bk = agentBusinessKey(agent);
                        if (!bk.isEmpty()) {
                            codeToAgent.put(bk, agent);
                        }
                        idToAgent.put(agent.getId(), agent);
                        newParentIds.add(agent.getId());
                    }
                }
            }
            stm.close();
            parentIds = newParentIds;
        }
    }

    public static List<Map<String, Object>> loadUsersForAgent(String agentCode) throws Exception {
        List<Map<String, Object>> users = new ArrayList<>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            String sql = "SELECT nick_name, commission_rate FROM users WHERE referral_code = ? AND is_bot = 0";
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setString(1, agentCode);
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> user = new HashMap<>();
                    user.put("nick_name", rs.getString("nick_name"));
                    Double rate = rs.getObject("commission_rate") != null ? rs.getDouble("commission_rate") : null;
                    user.put("commission_rate", rate);
                    users.add(user);
                }
            }
            stm.close();
        }
        return users;
    }

    /**
     * Lấy tài khoản user trùng nickname đại lý (đại lý tự chơi), nếu có.
     */
    public static Map<String, Object> loadSelfUserByNickname(String nickname) throws Exception {
        if (nickname == null || nickname.trim().isEmpty()) {
            return null;
        }
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            String sql = "SELECT nick_name, commission_rate FROM users WHERE nick_name = ? AND is_bot = 0 LIMIT 1";
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setString(1, nickname.trim());
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> user = new HashMap<>();
                    user.put("nick_name", rs.getString("nick_name"));
                    Double rate = rs.getObject("commission_rate") != null ? rs.getDouble("commission_rate") : null;
                    user.put("commission_rate", rate);
                    return user;
                }
            }
            stm.close();
        }
        return null;
    }

    /**
     * Trả về danh sách [root + tất cả đại lý con] theo parentid.
     * parentid > -1 được xem là đại lý con.
     */
    public static List<UserAgentModel> collectAgentSubtree(UserAgentModel root, Map<Integer, UserAgentModel> idToAgent) {
        List<UserAgentModel> out = new ArrayList<>();
        if (root == null || root.getId() == null) {
            return out;
        }

        Map<Integer, List<UserAgentModel>> parentToChildren = new HashMap<>();
        for (UserAgentModel a : idToAgent.values()) {
            if (a == null || a.getId() == null || a.getParentid() == null) {
                continue;
            }
            if (a.getParentid() > -1) {
                parentToChildren.computeIfAbsent(a.getParentid(), k -> new ArrayList<>()).add(a);
            }
        }

        List<UserAgentModel> queue = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        queue.add(root);

        for (int i = 0; i < queue.size(); i++) {
            UserAgentModel current = queue.get(i);
            if (current == null || current.getId() == null || !seen.add(current.getId())) {
                continue;
            }
            out.add(current);
            List<UserAgentModel> children = parentToChildren.get(current.getId());
            if (children != null && !children.isEmpty()) {
                queue.addAll(children);
            }
        }
        return out;
    }

    /**
     * Bản ghi giả (cột game = 0) cho user không có dòng trong {@code log_report_user} trong khoảng ngày.
     */
    public static Map<String, Object> buildZeroLogReportRecord(String nickName, String timeReport) {
        Map<String, Object> row = new HashMap<>();
        row.put("nick_name", nickName);
        row.put("time_report", timeReport != null ? timeReport : "");
        for (String col : GAME_COLUMN_MAP.values()) {
            row.put(col, 0L);
        }
        return row;
    }

    public static List<Map<String, Object>> loadLogReportUsers(List<String> nicknames, String fromDate, String toDate) throws Exception {
        List<Map<String, Object>> records = new ArrayList<>();
        if (nicknames.isEmpty()) return records;

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < nicknames.size(); i++) {
                if (i > 0) placeholders.append(",");
                placeholders.append("?");
            }
            String sql = "SELECT * FROM log_report_user WHERE nick_name IN (" + placeholders + ") AND time_report BETWEEN ? AND ?";
            PreparedStatement stm = conn.prepareStatement(sql);
            int idx = 1;
            for (String nn : nicknames) {
                stm.setString(idx++, nn);
            }
            stm.setString(idx++, fromDate);
            stm.setString(idx, toDate);

            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("nick_name", rs.getString("nick_name"));
                    row.put("time_report", rs.getString("time_report"));
                    for (String col : GAME_COLUMN_MAP.values()) {
                        try {
                            row.put(col, rs.getLong(col));
                        } catch (Exception e) {
                            row.put(col, 0L);
                        }
                    }
                    records.add(row);
                }
            }
            stm.close();
        }
        return records;
    }

    public static class UserCashflow {
        public long totalNap;
        public long totalRut;
    }

    public static class AgentUsersBatch {
        public final Map<String, List<Map<String, Object>>> usersByAgentCode = new HashMap<>();
        public final Map<String, UserAgentModel> ownerByNick = new HashMap<>();
        public final Map<String, Double> userDefaultRateByNick = new HashMap<>();
        public final Set<String> allNicknames = new HashSet<>();
    }

    /**
     * Load một lần tất cả user cho danh sách đại lý (gồm user referral + user tự chơi trùng nickname đại lý).
     */
    public static AgentUsersBatch loadUsersForAgentsBatch(List<UserAgentModel> ownerAgents) throws Exception {
        AgentUsersBatch out = new AgentUsersBatch();
        if (ownerAgents == null || ownerAgents.isEmpty()) {
            return out;
        }

        List<String> ownerCodes = new ArrayList<>();
        List<String> ownerNicknames = new ArrayList<>();
        Map<String, UserAgentModel> ownerByCode = new HashMap<>();
        Map<String, UserAgentModel> ownerByNickname = new HashMap<>();
        for (UserAgentModel a : ownerAgents) {
            if (a == null) continue;
            String code = agentBusinessKey(a);
            if (!code.isEmpty()) {
                ownerCodes.add(code);
                ownerByCode.put(code, a);
                out.usersByAgentCode.putIfAbsent(code, new ArrayList<>());
            }
            if (a.getNickname() != null && !a.getNickname().trim().isEmpty()) {
                String nn = a.getNickname().trim();
                ownerNicknames.add(nn);
                ownerByNickname.put(nn, a);
            }
        }
        if (ownerCodes.isEmpty()) {
            return out;
        }

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            StringBuilder codePh = new StringBuilder();
            for (int i = 0; i < ownerCodes.size(); i++) {
                if (i > 0) codePh.append(",");
                codePh.append("?");
            }
            StringBuilder nickPh = new StringBuilder();
            for (int i = 0; i < ownerNicknames.size(); i++) {
                if (i > 0) nickPh.append(",");
                nickPh.append("?");
            }

            String sql = "SELECT nick_name, commission_rate, referral_code FROM users WHERE is_bot = 0 AND (" +
                    "referral_code IN (" + codePh + ")";
            if (!ownerNicknames.isEmpty()) {
                sql += " OR nick_name IN (" + nickPh + ")";
            }
            sql += ")";

            PreparedStatement stm = conn.prepareStatement(sql);
            int idx = 1;
            for (String c : ownerCodes) {
                stm.setString(idx++, c);
            }
            for (String n : ownerNicknames) {
                stm.setString(idx++, n);
            }

            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    String nick = rs.getString("nick_name");
                    String referralCode = rs.getString("referral_code");
                    Double rate = rs.getObject("commission_rate") != null ? rs.getDouble("commission_rate") : null;
                    if (nick == null || nick.trim().isEmpty()) {
                        continue;
                    }
                    String nn = nick.trim();
                    out.allNicknames.add(nn);
                    out.userDefaultRateByNick.put(nn, rate != null ? rate : 0.0);

                    Map<String, Object> user = new HashMap<>();
                    user.put("nick_name", nn);
                    user.put("commission_rate", rate);

                    if (referralCode != null && ownerByCode.containsKey(referralCode)) {
                        out.usersByAgentCode.computeIfAbsent(referralCode, k -> new ArrayList<>()).add(user);
                        out.ownerByNick.putIfAbsent(nn, ownerByCode.get(referralCode));
                    }
                    if (ownerByNickname.containsKey(nn)) {
                        String ownerCode = agentBusinessKey(ownerByNickname.get(nn));
                        if (!ownerCode.isEmpty()) {
                            out.usersByAgentCode.computeIfAbsent(ownerCode, k -> new ArrayList<>()).add(user);
                            out.ownerByNick.putIfAbsent(nn, ownerByNickname.get(nn));
                        }
                    }
                }
            }
            stm.close();
        }

        return out;
    }

    /**
     * Batch tổng nạp/rút theo nick_name trong khoảng thời gian với action_name chứa "Admin".
     */
    public static Map<String, UserCashflow> loadAdminNapRutByNicknames(List<String> nicknames, String fromTime, String toTime) {
        Map<String, UserCashflow> out = new HashMap<>();
        if (nicknames == null || nicknames.isEmpty()) {
            return out;
        }
        String from = fromTime == null ? "" : fromTime.trim();
        String to = toTime == null ? "" : toTime.trim();
        // API thường truyền yyyy-MM-dd, cần mở rộng full-day để match trans_time dạng yyyy-MM-dd HH:mm:ss
        if (!from.isEmpty() && from.length() == 10) {
            from = from + " 00:00:00";
        }
        if (!to.isEmpty() && to.length() == 10) {
            to = to + " 23:59:59";
        }

        MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
        List<String> names = new ArrayList<>(new HashSet<>(nicknames));
        BasicDBObject timeCond = new BasicDBObject();
        timeCond.put("$gte", from);
        timeCond.put("$lte", to);
        Document match = new Document("nick_name", new BasicDBObject("$in", names))
                .append("trans_time", timeCond)
                .append("action_name", new BasicDBObject("$regex", ".*Admin.*").append("$options", "i"));
        Document group = new Document("$group", new Document("_id", "$nick_name").append("total", new Document("$sum", "$money_exchange")));

        try {
            MongoCollection<Document> napCol = db.getCollection(MongoCollections.LOG_MONEY_USER_NAP_VIN);
            AggregateIterable<Document> napAgg = napCol.aggregate(new ArrayList<Document>() {{
                add(new Document("$match", match));
                add(group);
            }});
            napAgg.forEach((Block<Document>) d -> {
                String nick = d.getString("_id");
                long total = d.get("total") instanceof Number ? ((Number) d.get("total")).longValue() : 0L;
                UserCashflow c = out.computeIfAbsent(nick, k -> new UserCashflow());
                c.totalNap = total;
            });

            MongoCollection<Document> rutCol = db.getCollection("log_money_user_tieu_vin");
            AggregateIterable<Document> rutAgg = rutCol.aggregate(new ArrayList<Document>() {{
                add(new Document("$match", match));
                add(group);
            }});
            rutAgg.forEach((Block<Document>) d -> {
                String nick = d.getString("_id");
                long total = d.get("total") instanceof Number ? ((Number) d.get("total")).longValue() : 0L;
                UserCashflow c = out.computeIfAbsent(nick, k -> new UserCashflow());
                c.totalRut = total;
            });
        } catch (Exception ignored) {
        }
        return out;
    }

    /**
     * Lấy số dư vin theo nick: ưu tiên Hazelcast "users", thiếu thì fallback DB users trong 1 query.
     */
    public static Map<String, Long> loadVinBalanceByNicknames(List<String> nicknames) throws Exception {
        Map<String, Long> out = new HashMap<>();
        if (nicknames == null || nicknames.isEmpty()) {
            return out;
        }

        List<String> names = new ArrayList<>(new HashSet<>(nicknames));
        List<String> missing = new ArrayList<>();

        try {
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            IMap<String, UserModel> userMap = client.getMap("users");
            for (String nn : names) {
                if (nn == null || nn.trim().isEmpty()) continue;
                UserModel u = userMap.get(nn);
                if (u != null) {
                    out.put(nn, u.getVin());
                } else {
                    missing.add(nn);
                }
            }
        } catch (Exception e) {
            missing.addAll(names);
        }

        if (missing.isEmpty()) {
            return out;
        }

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            StringBuilder ph = new StringBuilder();
            for (int i = 0; i < missing.size(); i++) {
                if (i > 0) ph.append(",");
                ph.append("?");
            }
            String sql = "SELECT nick_name, vin FROM users WHERE nick_name IN (" + ph + ")";
            PreparedStatement stm = conn.prepareStatement(sql);
            for (int i = 0; i < missing.size(); i++) {
                stm.setString(i + 1, missing.get(i));
            }
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    out.put(rs.getString("nick_name"), rs.getLong("vin"));
                }
            }
            stm.close();
        }
        return out;
    }

    public static long getLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).longValue();
        return 0L;
    }

    public static UserAgentModel mapAgent(ResultSet rs) throws Exception {
        UserAgentModel agent = new UserAgentModel();
        agent.setId(rs.getInt("id"));
        agent.setNickname(rs.getString("nickname"));
        agent.setCode(rs.getString("code"));
        agent.setLevel(rs.getInt("level"));
        agent.setParentid(rs.getInt("parentid"));
        Double commissionRate = rs.getObject("commission_rate") != null ? rs.getDouble("commission_rate") : null;
        agent.setCommission_rate(commissionRate);
        agent.setStatus(rs.getString("status"));
        agent.setActive(rs.getInt("active"));
        return agent;
    }

    /** Per-row breakdown from one log_report_user row (one user + one day). */
    public static class LogRecordCommissionBreakdown {
        public long totalBet;
        public long totalBetCasino;
        public long totalBetSport;
        public long totalBetGame;
        public long totalUserCommission;
        public final Map<String, Long> agentTotalCommission = new HashMap<>();
        public Map<String, AgentCommissionDaily.GameDetail> gameDetailsMap;
    }

    /**
     * Same game loop as CalculateAgentCommissionProcessor for one record.
     * @param includeGameDetails if true, fills {@link LogRecordCommissionBreakdown#gameDetailsMap} for Mongo upsert.
     */
    public static LogRecordCommissionBreakdown computeLogRecordBreakdown(
            Map<String, Object> record,
            UserAgentModel directAgent,
            double defaultUserRate,
            Map<Integer, Double> perGameRates,
            Map<String, UserAgentModel> codeToAgent,
            Map<Integer, UserAgentModel> idToAgent,
            boolean includeGameDetails) {

        LogRecordCommissionBreakdown out = new LogRecordCommissionBreakdown();
        if (includeGameDetails) {
            out.gameDetailsMap = new LinkedHashMap<>();
        }

        for (Map.Entry<Integer, String> entry : GAME_COLUMN_MAP.entrySet()) {
            int gameId = entry.getKey();
            String column = entry.getValue();
            long betAmount = Math.abs(getLong(record, column));
            if (betAmount == 0) continue;

            double userRate = perGameRates.getOrDefault(gameId, defaultUserRate);
            long userComm = (long) (userRate / 100.0 * betAmount);

            List<AgentCommissionDaily.AgentDistribution> gameDists =
                    buildDistributionsForGame(directAgent, userRate, betAmount, codeToAgent, idToAgent);

            if (includeGameDetails && out.gameDetailsMap != null) {
                Games gameEnum = Games.findGameById(gameId);
                String gameName = gameEnum != null ? gameEnum.getDescription() : column;
                out.gameDetailsMap.put(column, new AgentCommissionDaily.GameDetail(
                        gameId, gameName, betAmount, userRate, userComm, gameDists));
            }

            out.totalBet += betAmount;
            out.totalUserCommission += userComm;

            String category = getGameCategory(gameId);
            if (CATEGORY_CASINO.equals(category)) out.totalBetCasino += betAmount;
            else if (CATEGORY_SPORT.equals(category)) out.totalBetSport += betAmount;
            else out.totalBetGame += betAmount;

            for (AgentCommissionDaily.AgentDistribution d : gameDists) {
                out.agentTotalCommission.merge(d.getAgentCode(), d.getCommission(), Long::sum);
            }
        }
        return out;
    }
}
