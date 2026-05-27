package com.vinplay.api.processors.gsc;

import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * c=3093 — manually flush the in-process GSC game-list cache on this
 * portal-api JVM.
 *
 * <p>Primary path for cache invalidation is the Hazelcast topic publish
 * from c=9982 (admin toggle), which auto-flushes every portal-api JVM in
 * the cluster within ~50 ms. This endpoint exists as an ops backup: if the
 * topic publish ever fails silently, an operator can call this to force
 * a flush.
 *
 * <p>Params:
 * <ul>
 *   <li>{@code aat} — admin access token, required</li>
 *   <li>{@code product_code} — optional. If present, flush only entries
 *       for that product; otherwise flush the entire cache.</li>
 * </ul>
 *
 * <p>Returns: {@code {success:true, errorCode:"0", data:{cleared:N}}}.
 */
public class ClearGSCGameListCacheProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = LoggerFactory.getLogger("portal");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();

            String aat = request.getParameter("aat");
            if (aat == null || aat.isEmpty()) {
                return err(response, "1001", "aat required");
            }
            IMap<String, String> tokenMap = HazelcastClientFactory.getInstance().getMap("cacheToken");
            String adminNick = tokenMap.get(aat);
            if (adminNick == null || adminNick.isEmpty()) {
                return err(response, "1001", "Unauthorized");
            }

            int productCode = 0;
            String pcStr = request.getParameter("product_code");
            if (pcStr != null && !pcStr.isEmpty()) {
                try { productCode = Integer.parseInt(pcStr); } catch (NumberFormatException ignored) {}
            }

            int cleared = GSCGameListProcessor.clearCache(productCode);
            logger.info("c=3093 cache flush by admin={} product_code={} cleared_entries={}",
                    adminNick, productCode, cleared);

            JSONObject data = new JSONObject();
            data.put("cleared", cleared);
            data.put("scope", productCode > 0 ? ("product=" + productCode) : "all");
            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", data);
        } catch (Exception e) {
            logger.error("ClearGSCGameListCacheProcessor error", e);
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
