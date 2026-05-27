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
import java.io.BufferedReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class SetForceSlotResultProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");
    // Games that actually implement SlotForceResultHelper.checkAndConsume in their room code.
    // KHOBAU, THANTAI, THANDEN, CHIEMTINH → 3x5 (15 cells)
    // GALAXY, DRAGONBALL → 3x3 (9 cells)
    private static final Set<String> VALID_GAMES = new HashSet<>(Arrays.asList(
            "KHOBAU", "THANTAI", "THANDEN", "CHIEMTINH", "GALAXY", "DRAGONBALL"));
    private static final long TTL_MINUTES = 60;

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

            String adminNickname = (String) tokenMap.get(accessToken);

            // Get parameters - support both query param and JSON POST body
            String game = request.getParameter("game");
            String username = request.getParameter("username");
            String matrixStr = request.getParameter("matrix");

            // If params not in query string, try JSON body
            if (game == null || username == null || matrixStr == null) {
                try {
                    StringBuilder sb = new StringBuilder();
                    BufferedReader reader = request.getReader();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    String body = sb.toString().trim();
                    if (!body.isEmpty()) {
                        JSONObject bodyJson = new JSONObject(body);
                        if (game == null && bodyJson.has("game")) {
                            game = bodyJson.getString("game");
                        }
                        if (username == null && bodyJson.has("username")) {
                            username = bodyJson.getString("username");
                        }
                        if (matrixStr == null && bodyJson.has("matrix")) {
                            // matrix can be a JSON array in the body
                            Object matrixObj = bodyJson.get("matrix");
                            if (matrixObj instanceof JSONArray) {
                                matrixStr = matrixObj.toString();
                            } else {
                                matrixStr = matrixObj.toString();
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("SetForceSlotResultProcessor: failed to parse JSON body", e);
                }
            }

            // Validate game
            if (game == null || game.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "Missing game parameter");
                return response.toString();
            }
            game = game.toUpperCase();
            if (!VALID_GAMES.contains(game)) {
                response.put("success", false);
                response.put("errorCode", "4002");
                response.put("message", "Invalid game name. Valid: KHOBAU, THANTAI, THANDEN, CHIEMTINH, GALAXY, DRAGONBALL");
                return response.toString();
            }

            // Per-game grid dimensions
            int rows = getRows(game);
            int cols = getCols(game);
            int expectedSize = rows * cols;

            // Validate username
            if (username == null || username.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4003");
                response.put("message", "Missing username parameter");
                return response.toString();
            }

            // Validate matrix
            if (matrixStr == null || matrixStr.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4004");
                response.put("message", "Missing matrix parameter");
                return response.toString();
            }

            JSONArray matrixArray;
            try {
                matrixArray = new JSONArray(matrixStr);
            } catch (Exception e) {
                response.put("success", false);
                response.put("errorCode", "4005");
                response.put("message", "Invalid matrix format. Must be a JSON array of " + expectedSize + " integers");
                return response.toString();
            }

            if (matrixArray.length() != expectedSize) {
                response.put("success", false);
                response.put("errorCode", "4006");
                response.put("message", "Matrix for " + game + " must contain exactly " + expectedSize
                        + " integers (" + rows + "x" + cols + " grid)");
                return response.toString();
            }

            int maxSymbol = getMaxSymbol(game);
            int[] matrix = new int[expectedSize];
            for (int i = 0; i < expectedSize; i++) {
                try {
                    matrix[i] = matrixArray.getInt(i);
                } catch (Exception e) {
                    response.put("success", false);
                    response.put("errorCode", "4007");
                    response.put("message", "Matrix element at index " + i + " is not a valid integer");
                    return response.toString();
                }
                if (matrix[i] < 0 || matrix[i] > maxSymbol) {
                    response.put("success", false);
                    response.put("errorCode", "4008");
                    response.put("message", "Matrix element at index " + i + " is out of range. Valid range for " + game + ": 0-" + maxSymbol);
                    return response.toString();
                }
            }

            // Build the value JSON
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            JSONObject valueObj = new JSONObject();
            valueObj.put("game", game);
            valueObj.put("username", username);
            valueObj.put("matrix", matrixArray);
            valueObj.put("createdBy", adminNickname != null ? adminNickname : "admin");
            valueObj.put("createdAt", sdf.format(new Date()));

            // Store via DistCache with TTL (Hazelcast or Redis per routing flag)
            String cacheKey = game + ":" + username;
            com.vinplay.vbee.common.cache.DistCache<String, String> forceResultMap =
                    com.vinplay.vbee.common.cache.CacheFactory.get("cacheForceResult", String.class);
            forceResultMap.put(cacheKey, valueObj.toString(), TTL_MINUTES, TimeUnit.MINUTES);

            logger.info("SetForceSlotResult: admin=" + adminNickname + " set force result for " + cacheKey);

            response.put("success", true);
        } catch (Exception e) {
            logger.error("SetForceSlotResultProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
            response.put("message", "Internal server error");
        }
        return response.toString();
    }

    private int getRows(String game) {
        // All supported slot games use 3 rows
        return 3;
    }

    private int getCols(String game) {
        switch (game) {
            case "GALAXY":
            case "DRAGONBALL":
                return 3; // 3x3 Slot3x3Util grid
            case "KHOBAU":
            case "THANTAI":
            case "THANDEN":
            case "CHIEMTINH":
            default:
                return 5; // 3x5 standard reel
        }
    }

    private int getMaxSymbol(String game) {
        switch (game) {
            case "KHOBAU":
                return 6;  // POUCH(0) .. ANVIL(6) — 7 symbols
            case "THANTAI":
                return 10; // SCATTER(0) .. RADAR(10) — 11 symbols (SpartanItem)
            case "THANDEN":
                return 10; // A(0) .. SCATTER(10) — 11 symbols (ThanDenItem)
            case "CHIEMTINH":
                return 10; // Slot11Icon — 11 symbols 0-10
            case "GALAXY":
            case "DRAGONBALL":
                return 7;  // Slot3x3 symbols 0-7 — 8 icons (rateIcon array)
            default:
                return 6;
        }
    }
}
