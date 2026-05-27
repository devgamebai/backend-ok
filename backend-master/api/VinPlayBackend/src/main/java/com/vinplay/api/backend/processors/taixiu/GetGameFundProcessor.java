package com.vinplay.api.backend.processors.taixiu;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Get game fund/quỹ values (c=8800).
 * Params: game (taixiu|sicbo|all), aat (admin token)
 *
 * Reads from vinplay_minigame.minigame_funds table.
 * Fund names: TaiXiu_Fund_vin, TaiXiu_Fund_xu, TaiXiu_Fund_JPTai, TaiXiu_Fund_JPXiu, SICBO_FUND_VIN
 */
public class GetGameFundProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            String game = request.getParameter("game");
            if (game == null || game.isEmpty()) game = "all";
            game = game.toLowerCase();

            String filter;
            switch (game) {
                case "taixiu":
                    filter = "TaiXiu_Fund%";
                    break;
                case "sicbo":
                    filter = "SICBO_FUND%";
                    break;
                default:
                    filter = "%";
                    break;
            }

            JSONArray funds = new JSONArray();
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame")) {
                PreparedStatement stm = conn.prepareStatement(
                        "SELECT id, fund_name, value FROM minigame_funds WHERE fund_name LIKE ?");
                stm.setString(1, filter);
                ResultSet rs = stm.executeQuery();
                while (rs.next()) {
                    JSONObject fund = new JSONObject();
                    fund.put("id", rs.getInt("id"));
                    fund.put("fund_name", rs.getString("fund_name"));
                    fund.put("value", rs.getLong("value"));
                    funds.put(fund);
                }
                rs.close();
                stm.close();
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", funds);

        } catch (Exception e) {
            logger.error("GetGameFundProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }
}
