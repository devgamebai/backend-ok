package com.vinplay.api.backend.processors.taixiu;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Force next TaiXiu round result (c=8798).
 * Params: result (0=Xỉu, 1=Tài), aat (admin token)
 *
 * Writes pre-generated dice into Hazelcast map "ketquataixiu" which MGRoomTaiXiu
 * reads via suaKetQuaTaiXiu() at result time. One-shot: consumed and deleted after use.
 */
public class ForceResultTaiXiu implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            // Validate admin token
            String aat = request.getParameter("aat");
            if (aat == null || aat.isEmpty()) aat = request.getParameter("at");
            if (aat == null || aat.isEmpty()) {
                return err(response, "1001", "Missing admin token");
            }
            HazelcastInstance hz = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = hz.getMap("cacheToken");
            String adminNick = tokenMap.get(aat);
            if (adminNick == null) {
                return err(response, "1001", "Invalid admin token");
            }

            // Parse result param: 0=Xỉu, 1=Tài
            String rs = request.getParameter("result");
            if (rs == null || rs.isEmpty()) {
                return err(response, "4001", "Missing result param (0=Xỉu, 1=Tài)");
            }
            int resultSide;
            try {
                resultSide = Integer.parseInt(rs);
            } catch (NumberFormatException e) {
                return err(response, "4002", "result must be 0 or 1");
            }
            if (resultSide != 0 && resultSide != 1) {
                return err(response, "4002", "result must be 0 (Xỉu) or 1 (Tài)");
            }

            // Generate dice matching the forced side
            short[] dices = generateDicesForSide(resultSide);

            // Writes to BOTH Hazelcast maps so the force reaches whichever
            // TaiXiu module is actually running on this deployment.
            //
            // - "ketquataixiu"    → legacy TaiXiuModule / MGRoomTaiXiu.
            //                        Consumed by TaiXiuServiceImpl.suaKetQuaTaiXiu().
            // - "ketquataixiumd5" → MD5-provable TaiXiuMD5Module (LIVE on staging
            //                        as of 2026-04-17 — reference_id in the
            //                        288k range, writes to result_tai_xiu_md5).
            //                        Consumed by TaiXiuMD5ServiceImpl.suaKetQuaTaiXiu().
            //
            // Writing to both is cheap (one-shot put) and robust to future
            // module switches without needing an env flag.
            IMap<String, short[]> mapLegacy = hz.getMap("ketquataixiu");
            mapLegacy.put("ketquataixiu", dices);
            IMap<String, short[]> mapMd5 = hz.getMap("ketquataixiumd5");
            mapMd5.put("ketquataixiumd5", dices);

            int total = dices[0] + dices[1] + dices[2];
            logger.info("ForceResultTaiXiu: admin=" + adminNick + " side=" + (resultSide == 1 ? "TAI" : "XIU")
                    + " dice=[" + dices[0] + "," + dices[1] + "," + dices[2] + "] total=" + total);

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("side", resultSide == 1 ? "TAI" : "XIU");
            response.put("dice", new int[]{dices[0], dices[1], dices[2]});
            response.put("total", total);

        } catch (Exception e) {
            logger.error("ForceResultTaiXiu error", e);
            return err(response, "9999", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    /**
     * Generate 3 dice that sum to the requested side.
     * side=0 → Xỉu (total 3-10), side=1 → Tài (total 11-18)
     */
    private short[] generateDicesForSide(int side) {
        ThreadLocalRandom rd = ThreadLocalRandom.current();
        short[] dices;
        int total;
        do {
            dices = new short[]{
                    (short) (rd.nextInt(6) + 1),
                    (short) (rd.nextInt(6) + 1),
                    (short) (rd.nextInt(6) + 1)
            };
            total = dices[0] + dices[1] + dices[2];
        } while ((total > 10 ? 1 : 0) != side);
        return dices;
    }

    private String err(JSONObject r, String code, String msg) {
        r.put("success", false);
        r.put("errorCode", code);
        r.put("message", msg);
        return r.toString();
    }
}
