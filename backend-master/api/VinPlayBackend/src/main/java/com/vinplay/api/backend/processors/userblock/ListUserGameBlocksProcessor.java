package com.vinplay.api.backend.processors.userblock;

import com.hazelcast.core.IMap;
import com.vinplay.dal.service.UserGameBlock;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * c=9984 — List active blocks for a user.
 *
 * <p>Params: {@code aat}, plus EITHER {@code user_id} (long) OR
 * {@code nick_name} (string).
 */
public class ListUserGameBlocksProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();

            String aat = request.getParameter("aat");
            if (aat == null || aat.isEmpty()) return err(response, "1001", "aat required");
            IMap<String, String> tokenMap = HazelcastClientFactory.getInstance().getMap("cacheToken");
            if (tokenMap.get(aat) == null) return err(response, "1001", "Unauthorized");

            long userId = parseLongOr(request.getParameter("user_id"), 0);
            String nickName = request.getParameter("nick_name");
            if (userId <= 0 && (nickName == null || nickName.isEmpty())) {
                return err(response, "4001", "user_id or nick_name required");
            }

            List<UserGameBlock.BlockRow> rows = UserGameBlock.listForUser(userId, nickName);
            JSONArray arr = new JSONArray();
            for (UserGameBlock.BlockRow r : rows) {
                JSONObject o = new JSONObject();
                o.put("id", r.id);
                o.put("user_id", r.userId);
                o.put("nick_name", r.nickName);
                o.put("provider", r.provider);
                o.put("vendor_platform", r.vendorPlatform);
                o.put("game_code", r.gameCode);
                o.put("table_tag", r.tableTag);
                o.put("category_id", r.categoryId);
                o.put("reason", r.reason);
                o.put("blocked_by", r.blockedBy);
                o.put("blocked_at", r.blockedAt == null ? null : r.blockedAt.toString());
                o.put("expires_at", r.expiresAt == null ? null : r.expiresAt.toString());
                o.put("active", r.active);
                arr.put(o);
            }

            JSONObject data = new JSONObject();
            data.put("user_id", userId);
            data.put("nick_name", nickName);
            data.put("count", arr.length());
            data.put("items", arr);
            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", data);
        } catch (Exception e) {
            logger.error("ListUserGameBlocksProcessor error", e);
            return err(response, "9999", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    private static long parseLongOr(String s, long fallback) {
        if (s == null || s.isEmpty()) return fallback;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return fallback; }
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false);
        r.put("errorCode", code);
        r.put("message", msg);
        return r.toString();
    }
}
