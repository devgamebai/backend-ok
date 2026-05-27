/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.Block
 *  com.mongodb.client.AggregateIterable
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.response.GetUserInfoResponse
 *  com.vinplay.vbee.common.response.UserInfoResponse
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.usercore.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.usercore.dao.UserInfoDao;
import com.vinplay.usercore.model.DuplicateIpGroupRow;
import com.vinplay.usercore.model.DuplicateIpUserRow;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.response.GetUserInfoResponse;
import com.vinplay.vbee.common.response.UserInfoResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.bson.Document;
import org.bson.conversions.Bson;

public class UserInfoDaoImpl
implements UserInfoDao {

    /** Page size used by findDuplicateIpGroups pagination. */
    private static final int PAGE_SIZE_DUP = 50;

    /**
     * Hard cap on rows returned by {@link #findUsersForFirstIps} to keep
     * the JVM bounded when an admin drills into a shared-VPN IP. With
     * PAGE_SIZE_DUP=50 first-IPs and this 500-row cap, a single response
     * stays under ~50 KiB even for very promiscuous IPs.
     */
    private static final int MAX_DUP_USERS = 500;

    /**
     * Escape a user-supplied substring for MongoDB regex matching.
     * <p>
     * Java's {@code Pattern.quote()} wraps the literal in {@code \Q...\E},
     * which MongoDB 7.0's PCRE-based engine technically accepts but has
     * documented edge cases under {@code $options: "i"} on older builds.
     * Escape each metacharacter individually so the resulting regex is
     * portable across all Mongo regex flavours we ship against.
     */
    private static String escapeMongoRegex(String literal) {
        if (literal == null) return "";
        StringBuilder sb = new StringBuilder(literal.length() + 8);
        for (int i = 0, n = literal.length(); i < n; i++) {
            char c = literal.charAt(i);
            if (c == '.' || c == '*' || c == '+' || c == '?' || c == '^' || c == '$'
                    || c == '|' || c == '(' || c == ')' || c == '[' || c == ']'
                    || c == '{' || c == '}' || c == '\\') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Cache of compiled IP-chain regexes. user_login_info.ip stores the
     * full X-Forwarded-For chain ("client_ip, cdn_edge_ip"), so admin
     * search has to match the IP at any chain position. Pattern.compile
     * isn't free; admin pages hit both call sites with the same IP many
     * times during paged scrolls, so a small process-wide cache pays off
     * without bounding memory because operator IP search is low-cardinality.
     */
    private static final Map<String, Pattern> IP_CHAIN_PATTERN_CACHE =
            new ConcurrentHashMap<String, Pattern>();

    /**
     * Build (or reuse from cache) the regex that matches {@code ip} at any
     * position in a comma-separated XFF chain. Returns {@code null} when
     * {@code ip} is null/blank — caller should skip the filter in that
     * case rather than match-all.
     */
    private static Pattern xffChainPattern(String ip) {
        if (ip == null) return null;
        final String trimmed = ip.trim();
        if (trimmed.isEmpty()) return null;
        Pattern cached = IP_CHAIN_PATTERN_CACHE.get(trimmed);
        if (cached != null) return cached;
        Pattern p = Pattern.compile("(^|,\\s*)" + Pattern.quote(trimmed) + "(\\s*,|$)");
        IP_CHAIN_PATTERN_CACHE.put(trimmed, p);
        return p;
    }

    @Override
    public List<UserInfoResponse> searchUserInfo(String nickName, String ip, String startDate, String endDate, String type, int page) {
        final ArrayList<UserInfoResponse> result = new ArrayList<UserInfoResponse>();
        int num_start = (page - 1) * 50;
        int num_end = 50;
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        objsort.put("_id", -1);
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        if (nickName != null && !nickName.equals("")) {
            conditions.put("nick_name", nickName);
        }
        Pattern ipPattern = xffChainPattern(ip);
        if (ipPattern != null) {
            conditions.put("ip", ipPattern);
        }
        if (type != null && !type.trim().isEmpty() && !type.trim().equals("null")) {
            try {
                conditions.put("type", Integer.parseInt(type.trim()));
            } catch (NumberFormatException ignored) {}
        }
        if (startDate != null && !startDate.equals("") && endDate != null && !endDate.equals("")) {
            obj.put("$gte", startDate);
            obj.put("$lte", endDate);
            conditions.put("time_log", obj);
        }
        final UserServiceImpl service = new UserServiceImpl();
        FindIterable iterable = db.getCollection("user_login_info").find((Bson)new Document(conditions)).skip(num_start).limit(50).sort((Bson)objsort);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                String nn = document.getString("nick_name");
                UserInfoResponse userinfo = new UserInfoResponse();
                Integer uid = document.getInteger("user_id");
                userinfo.user_id = uid != null ? uid : 0;
                userinfo.user_name = document.getString("user_name");
                userinfo.nick_name = document.getString("nick_name");
                userinfo.ip = document.getString("ip");
                userinfo.agent = document.getString("agent");
                Integer ttype = document.getInteger("type");
                userinfo.type = ttype != null ? ttype : 0;
                userinfo.time_log = document.getString("time_log");
                try {
                    UserModel model = service.getUserByNickName(nn);
                    if (model != null) {
                        userinfo.security = model.isHasMobileSecurity();
                    } else {
                        userinfo.security = false;
                    }
                }
                catch (SQLException e) {
                    e.printStackTrace();
                }
                result.add(userinfo);
            }
        });
        return result;
    }

    @Override
    public int countsearchUserInfo(String nickName, String ip, String startDate, String endDate, String type) {
        final ArrayList result = new ArrayList();
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        objsort.put("_id", -1);
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        if (nickName != null && !nickName.equals("")) {
            conditions.put("nick_name", nickName);
        }
        Pattern ipPattern = xffChainPattern(ip);
        if (ipPattern != null) {
            conditions.put("ip", ipPattern);
        }
        if (type != null && !type.trim().isEmpty() && !type.trim().equals("null")) {
            try {
                conditions.put("type", Integer.parseInt(type.trim()));
            } catch (NumberFormatException ignored) {}
        }
        if (startDate != null && !startDate.equals("") && endDate != null && !endDate.equals("")) {
            obj.put("$gte", startDate);
            obj.put("$lte", endDate);
            conditions.put("time_log", obj);
        }
        FindIterable iterable = db.getCollection("user_login_info").find((Bson)new Document(conditions)).sort((Bson)objsort);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                UserInfoResponse userinfo = new UserInfoResponse();
                Integer uid = document.getInteger("user_id");
                userinfo.user_id = uid != null ? uid : 0;
                userinfo.user_name = document.getString("user_name");
                userinfo.nick_name = document.getString("nick_name");
                userinfo.ip = document.getString("ip");
                userinfo.agent = document.getString("agent");
                Integer ttype = document.getInteger("type");
                userinfo.type = ttype != null ? ttype : 0;
                userinfo.time_log = document.getString("time_log");
                result.add(userinfo);
            }
        });
        return result.size();
    }

    @Override
    public GetUserInfoResponse listGetNickName(String nickName) throws SQLException {
        GetUserInfoResponse userinfo = new GetUserInfoResponse();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT * FROM users WHERE nick_name=?";
            PreparedStatement stm = conn.prepareStatement("SELECT * FROM users WHERE nick_name=?");
            stm.setString(1, nickName);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                userinfo.nick_name = rs.getString("nick_name");
                userinfo.user_name = rs.getString("user_name");
                userinfo.ip = "";
                userinfo.time_log = rs.getString("create_time");
                userinfo.phone = rs.getString("mobile");
            } else {
                userinfo.nick_name = nickName;
            }
            rs.close();
            stm.close();
        }
        return userinfo;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Implementation notes:
     * <ul>
     *   <li>{@code type} and {@code nnSubstring} are applied in the initial $match
     *       (before grouping) so Mongo can use collection indexes on those fields
     *       and avoid materialising unwanted documents into the $group stage.</li>
     *   <li>{@code firstIp} is applied as a post-addFields $match (correctness
     *       gate) because it targets the computed field first_ip which does not
     *       exist as a stored field. An additional anchored prefix regex in the
     *       initial $match could be added as a performance optimisation but is
     *       omitted here to keep the pipeline readable.</li>
     *   <li>Both the page pipeline and the count pipeline share the identical
     *       stages up through the "userCount >= minUsers" $match; only the
     *       tail differs ($skip/$limit/$project vs $count).</li>
     * </ul>
     */
    @Override
    public List<DuplicateIpGroupRow> findDuplicateIpGroups(
            String tsFrom, String tsTo, Integer type,
            String nnSubstring, String firstIp,
            int minUsers, int page) {

        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection collection = db.getCollection("user_login_info");

        // ── Stage 1: initial $match ───────────────────────────────────────────
        // time_log range + ip field exists and is non-empty.
        // type and nnSubstring are also included here (pre-group) for efficiency.
        BasicDBObject initialMatchDoc = new BasicDBObject();
        if (tsFrom != null && tsTo != null) {
            BasicDBObject timeRange = new BasicDBObject();
            timeRange.put("$gte", tsFrom);
            timeRange.put("$lte", tsTo);
            initialMatchDoc.put("time_log", timeRange);
        }
        initialMatchDoc.put("ip", new BasicDBObject("$exists", true).append("$nin", Arrays.asList(null, "")));
        if (type != null) {
            initialMatchDoc.put("type", type);
        }
        if (nnSubstring != null && !nnSubstring.isEmpty()) {
            // Use a manual Mongo-safe escape; Pattern.quote()'s \Q…\E is a
            // Java-PCRE convention that is fragile under MongoDB regex.
            initialMatchDoc.put("nick_name",
                    new BasicDBObject("$regex", escapeMongoRegex(nnSubstring))
                            .append("$options", "i"));
        }
        Document stageMatch1 = new Document("$match", initialMatchDoc);

        // ── Stage 2: $addFields — compute first_ip ───────────────────────────
        // $indexOfBytes returns -1 when "," is not present (single-hop IP).
        // $let binds the result once so we avoid calling $indexOfBytes twice.
        Document firstIpExpr = new Document("$let",
                new Document("vars",
                        new Document("commaIdx",
                                new Document("$indexOfBytes",
                                        Arrays.asList("$ip", ","))))
                .append("in",
                        new Document("$cond",
                                Arrays.asList(
                                        new Document("$eq", Arrays.asList("$$commaIdx", -1)),
                                        "$ip",
                                        new Document("$substrBytes",
                                                Arrays.asList("$ip", 0, "$$commaIdx"))))));
        Document stageAddFields = new Document("$addFields",
                new Document("first_ip", firstIpExpr));

        // ── Stage 3: post-addFields $match ────────────────────────────────────
        // firstIp exact-match on the computed field is the correctness gate.
        Document stageMatch2 = null;
        if (firstIp != null && !firstIp.isEmpty()) {
            stageMatch2 = new Document("$match", new Document("first_ip", firstIp));
        }

        // ── Stage 4: $group ───────────────────────────────────────────────────
        Document stageGroup = new Document("$group",
                new Document("_id", "$first_ip")
                .append("userIds",    new Document("$addToSet", "$user_id"))
                .append("firstSeen",  new Document("$min", "$time_log"))
                .append("lastSeen",   new Document("$max", "$time_log")));

        // ── Stage 5: $addFields — derive userCount from the set size ─────────
        Document stageAddUserCount = new Document("$addFields",
                new Document("userCount", new Document("$size", "$userIds")));

        // ── Stage 6: $match — keep only groups with enough distinct users ─────
        Document stageMatch3 = new Document("$match",
                new Document("userCount", new Document("$gte", minUsers)));

        // ── Build the shared prefix list (stages used by both pipelines) ─────
        List<Document> sharedStages = new ArrayList<Document>();
        sharedStages.add(stageMatch1);
        sharedStages.add(stageAddFields);
        if (stageMatch2 != null) {
            sharedStages.add(stageMatch2);
        }
        sharedStages.add(stageGroup);
        sharedStages.add(stageAddUserCount);
        sharedStages.add(stageMatch3);

        // ── Count pipeline ────────────────────────────────────────────────────
        List<Document> countPipeline = new ArrayList<Document>(sharedStages);
        countPipeline.add(new Document("$count", "total"));

        long totalRecord = 0L;
        AggregateIterable countResult = collection.aggregate(countPipeline)
                .allowDiskUse(Boolean.TRUE);
        for (Object _doc : countResult) {
            Document d = (Document) _doc;
            Object t = d.get("total");
            if (t instanceof Number) {
                totalRecord = ((Number) t).longValue();
            }
            break;
        }

        // ── Page pipeline ─────────────────────────────────────────────────────
        // Stage 7: $sort  — most users first, most recent activity tiebreak
        Document stageSort = new Document("$sort",
                new Document("userCount", -1).append("lastSeen", -1));

        // Stage 8: $skip + $limit
        int skip = (page - 1) * PAGE_SIZE_DUP;
        if (skip < 0) skip = 0;
        Document stageSkip  = new Document("$skip",  skip);
        Document stageLimit = new Document("$limit", PAGE_SIZE_DUP);

        // Stage 9: $project — rename _id → firstIp, drop userIds set
        Document stageProject = new Document("$project",
                new Document("firstIp",   "$_id")
                .append("userCount",  1)
                .append("firstSeen",  1)
                .append("lastSeen",   1)
                .append("_id",        0));

        List<Document> pagePipeline = new ArrayList<Document>(sharedStages);
        pagePipeline.add(stageSort);
        pagePipeline.add(stageSkip);
        pagePipeline.add(stageLimit);
        pagePipeline.add(stageProject);

        List<DuplicateIpGroupRow> result = new ArrayList<DuplicateIpGroupRow>();
        AggregateIterable pageResult = collection.aggregate(pagePipeline)
                .allowDiskUse(Boolean.TRUE);
        for (Object _doc : pageResult) {
            Document d = (Document) _doc;
            DuplicateIpGroupRow row = new DuplicateIpGroupRow();
            row.firstIp     = d.getString("firstIp");
            row.firstSeen   = d.getString("firstSeen");
            row.lastSeen    = d.getString("lastSeen");
            Integer uc = d.getInteger("userCount");
            row.userCount   = uc != null ? uc : 0;
            row.totalRecord = totalRecord;
            result.add(row);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Short-circuits to an empty list when {@code firstIps} is null or empty
     * to avoid issuing a $match with an empty $in clause (which would perform a
     * full collection scan and return no results anyway).
     */
    @Override
    public List<DuplicateIpUserRow> findUsersForFirstIps(
            String tsFrom, String tsTo, List<String> firstIps,
            Integer type, String nnSubstring) {

        if (firstIps == null || firstIps.isEmpty()) {
            return Collections.emptyList();
        }

        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection collection = db.getCollection("user_login_info");

        // ── Stage 1: $match — time window + ip exists/non-empty ──────────────
        // type and nnSubstring are mirrored from findDuplicateIpGroups so
        // userCount reported by the group query matches users[].size() in
        // the drill-down response. Without this, filtered-out logins would
        // reappear in the user list and confuse the operator.
        BasicDBObject initialMatchDoc = new BasicDBObject();
        if (tsFrom != null && tsTo != null) {
            BasicDBObject timeRange = new BasicDBObject();
            timeRange.put("$gte", tsFrom);
            timeRange.put("$lte", tsTo);
            initialMatchDoc.put("time_log", timeRange);
        }
        initialMatchDoc.put("ip", new BasicDBObject("$exists", true).append("$nin", Arrays.asList(null, "")));
        if (type != null) {
            initialMatchDoc.put("type", type);
        }
        if (nnSubstring != null && !nnSubstring.isEmpty()) {
            initialMatchDoc.put("nick_name",
                    new BasicDBObject("$regex", escapeMongoRegex(nnSubstring))
                            .append("$options", "i"));
        }
        Document stageMatch1 = new Document("$match", initialMatchDoc);

        // ── Stage 2: $addFields — compute first_ip (same expression as above) ─
        Document firstIpExpr = new Document("$let",
                new Document("vars",
                        new Document("commaIdx",
                                new Document("$indexOfBytes",
                                        Arrays.asList("$ip", ","))))
                .append("in",
                        new Document("$cond",
                                Arrays.asList(
                                        new Document("$eq", Arrays.asList("$$commaIdx", -1)),
                                        "$ip",
                                        new Document("$substrBytes",
                                                Arrays.asList("$ip", 0, "$$commaIdx"))))));
        Document stageAddFields = new Document("$addFields",
                new Document("first_ip", firstIpExpr));

        // ── Stage 3: $match — restrict to the requested first IPs ────────────
        Document stageMatch2 = new Document("$match",
                new Document("first_ip", new Document("$in", firstIps)));

        // ── Stage 4: $group — unique (firstIp, userId) pairs ─────────────────
        Document groupId = new Document("firstIp", "$first_ip")
                .append("userId", "$user_id");
        Document stageGroup = new Document("$group",
                new Document("_id", groupId)
                .append("userName",   new Document("$first", "$user_name"))
                .append("nickName",   new Document("$first", "$nick_name"))
                .append("firstLogin", new Document("$min",   "$time_log"))
                .append("lastLogin",  new Document("$max",   "$time_log"))
                .append("loginCount", new Document("$sum",   1)));

        // ── Stage 5: $sort — by IP asc, then most-recent login first ─────────
        Document stageSort = new Document("$sort",
                new Document("_id.firstIp", 1).append("lastLogin", -1));

        // ── Stage 6: $project — flatten the compound _id to top-level fields ──
        Document stageProject = new Document("$project",
                new Document("firstIp",    "$_id.firstIp")
                .append("userId",     "$_id.userId")
                .append("userName",   1)
                .append("nickName",   1)
                .append("loginCount", 1)
                .append("firstLogin", 1)
                .append("lastLogin",  1)
                .append("_id",        0));

        // Hard cap so a promiscuous IP cannot blow up the response.
        Document stageLimit = new Document("$limit", MAX_DUP_USERS);

        List<Document> pipeline = Arrays.asList(
                stageMatch1, stageAddFields, stageMatch2,
                stageGroup, stageSort, stageLimit, stageProject);

        List<DuplicateIpUserRow> result = new ArrayList<DuplicateIpUserRow>();
        AggregateIterable aggResult = collection.aggregate(pipeline)
                .allowDiskUse(Boolean.TRUE);
        for (Object _doc : aggResult) {
            Document d = (Document) _doc;
            DuplicateIpUserRow row = new DuplicateIpUserRow();
            row.firstIp    = d.getString("firstIp");
            row.userName   = d.getString("userName");
            row.nickName   = d.getString("nickName");
            row.firstLogin = d.getString("firstLogin");
            row.lastLogin  = d.getString("lastLogin");
            Integer lc = d.getInteger("loginCount");
            row.loginCount = lc != null ? lc : 0;
            // user_id is stored as int in Mongo; safe-cast via Number
            Object uid = d.get("userId");
            if (uid instanceof Number) {
                row.userId = ((Number) uid).longValue();
            }
            result.add(row);
        }
        return result;
    }

}

