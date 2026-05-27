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
 * Force next Sicbo round result (c=8799).
 * Params:
 *   - result: 0=Xỉu, 1=Tài (generates random dice matching side)
 *   - d1,d2,d3: exact dice values (1-6 each) — overrides result param
 *   - aat: admin token
 *
 * Writes pre-generated dice into Hazelcast map "ketquataixiusicbo" which MGRoomTaiXiuSicbo
 * reads via suaKetQuaTaiXiu() at result time. One-shot: consumed and deleted after use.
 */
public class ForceResultSicbo implements BaseProcessor<HttpServletRequest, String> {

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

            // Check if exact dice provided (d1, d2, d3)
            String d1s = request.getParameter("d1");
            String d2s = request.getParameter("d2");
            String d3s = request.getParameter("d3");

            short[] dices;

            if (d1s != null && d2s != null && d3s != null) {
                // Exact dice mode
                try {
                    short d1 = Short.parseShort(d1s);
                    short d2 = Short.parseShort(d2s);
                    short d3 = Short.parseShort(d3s);
                    if (d1 < 1 || d1 > 6 || d2 < 1 || d2 > 6 || d3 < 1 || d3 > 6) {
                        return err(response, "4002", "Dice values must be 1-6");
                    }
                    dices = new short[]{d1, d2, d3};
                } catch (NumberFormatException e) {
                    return err(response, "4002", "d1,d2,d3 must be integers 1-6");
                }
            } else {
                // Side mode: 0=Xỉu, 1=Tài
                String rs = request.getParameter("result");
                if (rs == null || rs.isEmpty()) {
                    return err(response, "4001", "Missing result (0=Xỉu, 1=Tài) or d1,d2,d3 params");
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
                dices = generateDicesForSide(resultSide);
            }

            // Write to Hazelcast map "ketquataixiusicbo"
            IMap<String, short[]> map = hz.getMap("ketquataixiusicbo");
            map.put("ketquataixiusicbo", dices);

            int total = dices[0] + dices[1] + dices[2];
            String side = total > 10 ? "TAI" : "XIU";
            logger.info("ForceResultSicbo: admin=" + adminNick + " side=" + side
                    + " dice=[" + dices[0] + "," + dices[1] + "," + dices[2] + "] total=" + total);

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("side", side);
            response.put("dice", new int[]{dices[0], dices[1], dices[2]});
            response.put("total", total);

        } catch (Exception e) {
            logger.error("ForceResultSicbo error", e);
            return err(response, "9999", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

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
