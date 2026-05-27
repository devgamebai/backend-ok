package com.vinplay.api.backend.processors.slot;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

public class GetSlotGameSymbolsProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String accessToken = request.getParameter("at");
            if (accessToken == null || accessToken.isEmpty()) accessToken = request.getParameter("aat");

            // Validate admin token
            if (accessToken == null || accessToken.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                response.put("message", "Missing access token");
                return response.toString();
            }

            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = instance.getMap("cacheToken");
            if (!tokenMap.containsKey(accessToken)) {
                response.put("success", false);
                response.put("errorCode", "1001");
                response.put("message", "Invalid access token");
                return response.toString();
            }

            // Build games list — ONLY games that implement SlotForceResultHelper.checkAndConsume
            // Verified in slot server rooms: KhoBauRoom, ThanTaiRoom, ThanDenRoom, GalaxyRoom, DragonBallRoom
            JSONArray gamesArray = new JSONArray();
            gamesArray.put(buildKhoBauGame());      // 3x5
            gamesArray.put(buildThanTaiGame());     // 3x5
            gamesArray.put(buildThanDenGame());     // 3x5
            gamesArray.put(buildChiemTinhGame());   // 3x5 (Pirate King / Chiêm Tinh)
            gamesArray.put(buildGalaxyGame());      // 3x3 (Kim Cương)
            gamesArray.put(buildDragonBallGame());  // 3x3

            JSONObject data = new JSONObject();
            data.put("games", gamesArray);

            response.put("success", true);
            response.put("data", data);
        } catch (Exception e) {
            logger.error("GetSlotGameSymbolsProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
            response.put("message", "Internal server error");
        }
        return response.toString();
    }

    private JSONObject buildKhoBauGame() {
        JSONObject game = new JSONObject();
        game.put("name", "KHOBAU");
        game.put("displayName", "Kho B\u00e1u");
        game.put("rows", 3);
        game.put("cols", 5);

        JSONArray symbols = new JSONArray();
        symbols.put(buildSymbol(0, "POUCH", "T\u00fai v\u00e0ng"));
        symbols.put(buildSymbol(1, "BAG", "Bao v\u00e0ng"));
        symbols.put(buildSymbol(2, "BOOK", "S\u00e1ch"));
        symbols.put(buildSymbol(3, "BULLSEYE", "Bia ng\u1eafm"));
        symbols.put(buildSymbol(4, "MAP", "B\u1ea3n \u0111\u1ed3"));
        symbols.put(buildSymbol(5, "BOTTLE", "B\u00ecnh r\u01b0\u1ee3u"));
        symbols.put(buildSymbol(6, "ANVIL", "\u0110e"));

        game.put("symbols", symbols);
        return game;
    }

    // ThanTai = module "Spartan" in slot server (handler 7000). 3x5 grid, SpartanItem symbols.
    private JSONObject buildThanTaiGame() {
        JSONObject game = new JSONObject();
        game.put("name", "THANTAI");
        game.put("displayName", "Th\u1ea7n T\u00e0i");
        game.put("rows", 3);
        game.put("cols", 5);
        JSONArray symbols = new JSONArray();
        symbols.put(buildSymbol(0, "SCATTER", "Scatter"));
        symbols.put(buildSymbol(1, "BONUS", "Bonus"));
        symbols.put(buildSymbol(2, "WILD", "Wild"));
        symbols.put(buildSymbol(3, "JACKPOT", "Jackpot"));
        symbols.put(buildSymbol(4, "NAM_TAY", "N\u1eafm tay"));
        symbols.put(buildSymbol(5, "BUA", "B\u00faa"));
        symbols.put(buildSymbol(6, "KHIEN", "Khi\u00ean"));
        symbols.put(buildSymbol(7, "KIM_CUONG", "Kim c\u01b0\u01a1ng"));
        symbols.put(buildSymbol(8, "DAI_BANG", "\u0110\u1ea1i b\u00e0ng"));
        symbols.put(buildSymbol(9, "NGUOI_NHEN", "Ng\u01b0\u1eddi nh\u1ec7n"));
        symbols.put(buildSymbol(10, "RADAR", "Radar"));
        game.put("symbols", symbols);
        return game;
    }

    // ThanDen (Thần Đèn) = handler 19000. 3x5 grid, ThanDenItem symbols A..H + WILD/BONUS/SCATTER.
    private JSONObject buildThanDenGame() {
        JSONObject game = new JSONObject();
        game.put("name", "THANDEN");
        game.put("displayName", "Th\u1ea7n \u0110\u00e8n");
        game.put("rows", 3);
        game.put("cols", 5);
        JSONArray symbols = new JSONArray();
        symbols.put(buildSymbol(0, "A", "Symbol A"));
        symbols.put(buildSymbol(1, "B", "Symbol B"));
        symbols.put(buildSymbol(2, "C", "Symbol C"));
        symbols.put(buildSymbol(3, "D", "Symbol D"));
        symbols.put(buildSymbol(4, "E", "Symbol E"));
        symbols.put(buildSymbol(5, "F", "Symbol F"));
        symbols.put(buildSymbol(6, "G", "Symbol G"));
        symbols.put(buildSymbol(7, "H", "Symbol H"));
        symbols.put(buildSymbol(8, "WILD", "Wild"));
        symbols.put(buildSymbol(9, "BONUS", "Bonus"));
        symbols.put(buildSymbol(10, "SCATTER", "Scatter"));
        game.put("symbols", symbols);
        return game;
    }

    // Galaxy (Kim Cương) = handler 8000. 3x3 grid, Slot3x3 symbols.
    private JSONObject buildGalaxyGame() {
        JSONObject game = new JSONObject();
        game.put("name", "GALAXY");
        game.put("displayName", "Kim C\u01b0\u01a1ng");
        game.put("rows", 3);
        game.put("cols", 3);
        JSONArray symbols = new JSONArray();
        symbols.put(buildSymbol(0, "JACKPOT", "Jackpot"));
        symbols.put(buildSymbol(1, "SCATTER", "Scatter"));
        symbols.put(buildSymbol(2, "WILD", "Wild"));
        symbols.put(buildSymbol(3, "DIAMOND_A", "Kim c\u01b0\u01a1ng A"));
        symbols.put(buildSymbol(4, "DIAMOND_B", "Kim c\u01b0\u01a1ng B"));
        symbols.put(buildSymbol(5, "DIAMOND_C", "Kim c\u01b0\u01a1ng C"));
        symbols.put(buildSymbol(6, "DIAMOND_D", "Kim c\u01b0\u01a1ng D"));
        symbols.put(buildSymbol(7, "DIAMOND_E", "Kim c\u01b0\u01a1ng E"));
        game.put("symbols", symbols);
        return game;
    }

    // DragonBall = handler 9000. 3x3 grid, Slot3x3 symbols.
    private JSONObject buildDragonBallGame() {
        JSONObject game = new JSONObject();
        game.put("name", "DRAGONBALL");
        game.put("displayName", "Dragon Ball");
        game.put("rows", 3);
        game.put("cols", 3);
        JSONArray symbols = new JSONArray();
        symbols.put(buildSymbol(0, "JACKPOT", "Jackpot"));
        symbols.put(buildSymbol(1, "SCATTER", "Scatter"));
        symbols.put(buildSymbol(2, "WILD", "Wild"));
        symbols.put(buildSymbol(3, "BALL_1", "Rồng 1 sao"));
        symbols.put(buildSymbol(4, "BALL_2", "Rồng 2 sao"));
        symbols.put(buildSymbol(5, "BALL_3", "Rồng 3 sao"));
        symbols.put(buildSymbol(6, "BALL_4", "Rồng 4 sao"));
        symbols.put(buildSymbol(7, "BALL_5", "Rồng 5 sao"));
        game.put("symbols", symbols);
        return game;
    }

    // Chiêm Tinh (Pirate King bundle) = handler 6000. 3x5 grid, Slot11Icon engine.
    private JSONObject buildChiemTinhGame() {
        JSONObject game = new JSONObject();
        game.put("name", "CHIEMTINH");
        game.put("displayName", "Chi\u00eam Tinh (Pirate King)");
        game.put("rows", 3);
        game.put("cols", 5);
        JSONArray symbols = new JSONArray();
        symbols.put(buildSymbol(0, "A", "Symbol A"));
        symbols.put(buildSymbol(1, "B", "Symbol B"));
        symbols.put(buildSymbol(2, "C", "Symbol C"));
        symbols.put(buildSymbol(3, "D", "Symbol D"));
        symbols.put(buildSymbol(4, "E", "Symbol E"));
        symbols.put(buildSymbol(5, "F", "Symbol F"));
        symbols.put(buildSymbol(6, "G", "Symbol G"));
        symbols.put(buildSymbol(7, "H", "Symbol H"));
        symbols.put(buildSymbol(8, "WILD", "Wild"));
        symbols.put(buildSymbol(9, "BONUS", "Bonus"));
        symbols.put(buildSymbol(10, "SCATTER", "Scatter"));
        game.put("symbols", symbols);
        return game;
    }

    private JSONObject buildSymbol(int id, String name, String displayName) {
        JSONObject symbol = new JSONObject();
        symbol.put("id", id);
        symbol.put("name", name);
        symbol.put("displayName", displayName);
        return symbol;
    }
}
