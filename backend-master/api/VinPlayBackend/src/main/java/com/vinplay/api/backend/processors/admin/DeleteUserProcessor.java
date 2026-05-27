package com.vinplay.api.backend.processors.admin;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.mongodb.client.MongoDatabase;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * c=9818 — Admin: completely delete a user and ALL related data.
 * (was c=9860 — collided with CreateRtpExperimentProcessor; renumbered per GitLab #33.)
 *
 * MySQL: DELETE FROM users WHERE id=? → FK CASCADE handles 65 tables.
 * MongoDB: explicit deleteMany on all game log collections (win123club + vinplay).
 * Hazelcast: remove from users + cacheToken maps.
 *
 * If user is an agent, also deletes useragent row (FK CASCADE handles
 * agency_wallet, credit_wallet, rebate_config, rebate_logs, etc.)
 *
 * Params: aat (admin token), user_id OR nickname
 */
public class DeleteUserProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    private static final String[] MONGO_VINPLAY_COLLECTIONS = {
            "log_awc_bets", "log_gsc_bets", "log_taixiu", "log_sicbo"
    };

    private static final String[] MONGO_WIN123_COLLECTIONS = {
            "log_KhoBau", "log_VuongQuocVin", "log_SieuAnhHung", "log_NuDiepVien",
            "log_ChiemTinh", "log_mini_poker", "log_cao_thap", "log_cao_thap_win",
            "bau_cua_transaction", "bau_cua_results", "bau_cua_toi_chon_ca",
            "log_no_hu_slot", "log_no_hu_game_bai", "log_hu_game_bai",
            "log_game_detail", "log_money_user_xu", "log_money_user_tieu_vin",
            "log_taixiu", "log_sicbo", "jackpot_tx", "report_total_money"
    };

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();

            String userIdStr = request.getParameter("user_id");
            String nickname = request.getParameter("nickname");

            if ((userIdStr == null || userIdStr.isEmpty()) && (nickname == null || nickname.isEmpty())) {
                return err(response, "4001", "user_id or nickname required");
            }

            long userId = -1;
            String userName = null;
            String nickName = null;
            int agentId = 0;

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Resolve user
                String sql = userIdStr != null && !userIdStr.isEmpty()
                        ? "SELECT id, user_name, nick_name FROM users WHERE id = ?"
                        : "SELECT id, user_name, nick_name FROM users WHERE nick_name = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    if (userIdStr != null && !userIdStr.isEmpty()) {
                        ps.setLong(1, Long.parseLong(userIdStr));
                    } else {
                        ps.setString(1, nickname);
                    }
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            return err(response, "1002", "User not found");
                        }
                        userId = rs.getLong("id");
                        userName = rs.getString("user_name");
                        nickName = rs.getString("nick_name");
                    }
                }

                // Check if user is an agent
                try (Connection adminConn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
                     PreparedStatement ps = adminConn.prepareStatement(
                             "SELECT id FROM useragent WHERE nickname = ? LIMIT 1")) {
                    ps.setString(1, nickName);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) agentId = rs.getInt("id");
                    }
                }

                // Phase 1: MySQL — DELETE user (FK CASCADE handles 65 tables)
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
                    ps.setLong(1, userId);
                    ps.executeUpdate();
                }

                // Phase 2: MySQL — DELETE agent if exists (FK CASCADE handles 10 tables)
                if (agentId > 0) {
                    try (Connection adminConn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
                         PreparedStatement ps = adminConn.prepareStatement("DELETE FROM useragent WHERE id = ?")) {
                        ps.setInt(1, agentId);
                        ps.executeUpdate();
                    }
                }
            }

            // Phase 3: MongoDB — clean game logs (no FK, must delete explicitly)
            int mongoDeleted = 0;
            try {
                MongoDatabase vinplayDb = MongoDBConnectionFactory.getDB();
                for (String col : MONGO_VINPLAY_COLLECTIONS) {
                    try {
                        long d = vinplayDb.getCollection(col).deleteMany(
                                new Document("user_name", nickName)).getDeletedCount();
                        d += vinplayDb.getCollection(col).deleteMany(
                                new Document("nick_name", nickName)).getDeletedCount();
                        mongoDeleted += d;
                    } catch (Exception ignored) {}
                }

                MongoDatabase win123Db = MongoDBConnectionFactory.getDB("win123club");
                for (String col : MONGO_WIN123_COLLECTIONS) {
                    try {
                        long d = win123Db.getCollection(col).deleteMany(
                                new Document("user_name", nickName)).getDeletedCount();
                        d += win123Db.getCollection(col).deleteMany(
                                new Document("nick_name", nickName)).getDeletedCount();
                        mongoDeleted += d;
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                logger.warn("DeleteUser MongoDB cleanup partial failure for " + nickName, e);
            }

            // Phase 4: Hazelcast — remove from cache
            final String cacheKey = nickName;
            try {
                HazelcastInstance hz = HazelcastClientFactory.getInstance();
                IMap<String, Object> users = hz.getMap("users");
                users.remove(cacheKey);
                IMap<String, String> tokenMap = hz.getMap("cacheToken");
                tokenMap.entrySet().removeIf(e -> cacheKey.equals(e.getValue()));
            } catch (Exception e) {
                logger.warn("DeleteUser Hazelcast cleanup failed for " + nickName, e);
            }

            JSONObject data = new JSONObject();
            data.put("user_id", userId);
            data.put("user_name", userName);
            data.put("nick_name", nickName);
            data.put("agent_id", agentId);
            data.put("mongo_deleted", mongoDeleted);
            data.put("mysql_cascade", "65 FK tables");
            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", data);

            logger.info("DeleteUser: wiped user=" + nickName + " id=" + userId + " agent=" + agentId + " mongo=" + mongoDeleted);

        } catch (Exception e) {
            logger.error("DeleteUserProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false);
        r.put("errorCode", code);
        r.put("message", msg);
        return r.toString();
    }
}
