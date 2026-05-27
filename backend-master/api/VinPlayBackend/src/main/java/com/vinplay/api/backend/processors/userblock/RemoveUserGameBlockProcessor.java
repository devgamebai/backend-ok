package com.vinplay.api.backend.processors.userblock;

import com.hazelcast.core.IMap;
import com.vinplay.dal.service.UserGameBlock;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * c=9986 — Remove a per-user block by id (soft-delete is_active=0).
 *
 * <p>Param: {@code id} = the {@code user_game_block.id} returned from
 * c=9984 list or c=9985 add.
 */
public class RemoveUserGameBlockProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();

            String aat = request.getParameter("aat");
            if (aat == null || aat.isEmpty()) return err(response, "1001", "aat required");
            IMap<String, String> tokenMap = HazelcastClientFactory.getInstance().getMap("cacheToken");
            String adminNick = tokenMap.get(aat);
            if (adminNick == null || adminNick.isEmpty()) return err(response, "1001", "Unauthorized");

            String idStr = request.getParameter("id");
            if (idStr == null || idStr.isEmpty()) return err(response, "4001", "id required");
            long id;
            try { id = Long.parseLong(idStr); }
            catch (NumberFormatException e) { return err(response, "4002", "id must be numeric"); }

            boolean ok = UserGameBlock.deactivate(id, adminNick);
            if (!ok) return err(response, "1002", "block id not found");

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO log_admin (action, username, reason, status, account_name) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, "usergameblock.remove");
                ps.setString(2, adminNick);
                ps.setString(3, "id=" + id);
                ps.setString(4, "deactivated");
                ps.setString(5, String.valueOf(id));
                ps.executeUpdate();
            } catch (Exception auditErr) {
                logger.warn("RemoveUserGameBlock audit insert failed: " + auditErr.getMessage());
            }

            JSONObject data = new JSONObject();
            data.put("id", id);
            data.put("active", 0);
            data.put("note", "Block deactivation propagates within 60s (UserGameBlock cache TTL).");
            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", data);
        } catch (Exception e) {
            logger.error("RemoveUserGameBlockProcessor error", e);
            return err(response, "9999", "Internal: " + e.getMessage());
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
