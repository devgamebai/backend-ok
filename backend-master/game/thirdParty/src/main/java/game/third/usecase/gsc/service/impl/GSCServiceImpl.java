package game.third.usecase.gsc.service.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import game.third.usecase.config.GSCConfig;
import game.third.usecase.config.ThirdPartyLoad;
import game.third.usecase.core.common.HashUtil;
import game.third.usecase.core.constans.GSC;
import game.third.usecase.gsc.model.LaunchSuperLobbyResponse;
import game.third.usecase.gsc.service.GSCService;
import game.third.usecase.gsc.model.GSCGameLaunchResponse;
import game.third.usecase.gsc.model.GSCGameProviderResponse;
import game.third.usecase.gsc.model.GSCProductInfo;
import game.third.utils.HttpUtils;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GSCServiceImpl implements GSCService {

    private static final Logger logger = Logger.getLogger("service");

    protected GSCConfig gscConfig;

    public GSCServiceImpl() {
        this.gscConfig = ThirdPartyLoad.getGscConfig();
    }

    /**
     * Retrieves the list of game providers.
     * <p>
     * Endpoint: GET {{operator_url}}/api/operators/provider-games
     * Parameters:
     * - product_code (int, optional): The unique identifier of the product.
     * - operator_code (String, required): The operator's unique identifier, used as the backend login username.
     * - game_type (String, optional): Refer to the Game Type.
     * - sign (String, required): md5(request_time + secret_key + "gamelist" + operator_code).
     * Example: md5(1694617425XXXXgamelistCMUT_V2)
     * - request_time (int64, required): Timestamp of request time.
     * - offset (int, optional): Start record number of this retrieval.
     * - size (int, optional, default: 25): Number of records retrieved this time.
     *
     * @return a GSCGameProviderResponse object containing the list of game providers.
     */
    @Override
    public GSCGameProviderResponse gameList(int productCode, String gameType) throws Exception {
        try {
            long requestTime = System.currentTimeMillis() / 1000L;
            String operatorCode = this.gscConfig.getOperatorCode(); // Replace with actual operator code
            String sign = HashUtil.md5(requestTime + gscConfig.getSecretKey() + "gamelist" + operatorCode);

            String urlString = gscConfig.getOperatorUrl() +
                    "/api/operators/provider-games?" +
                    "operator_code=" + operatorCode +
                    "&product_code=" + productCode +
                    "&game_type=" + gameType +
                    "&sign=" + sign +
                    "&request_time=" + requestTime +
                    "&size=1000";

            String data = HttpUtils.getData(urlString);
            Gson gson = new Gson();
            return gson.fromJson(data, GSCGameProviderResponse.class);
        } catch (Exception e) {
            logger.error(e);
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Retrieves the game history for a specific wager code.
     * <p>
     * Endpoint: GET {{operator_url}}/api/operators/{wager_code}/game-history
     * <p>
     * Parameters:
     * - operator_code (String, required): The operator's unique identifier, used as the backend login username.
     * - sign (String, optional): md5(request_time + secret_key + "gamehistory" + operator_code).
     * Example: md5(1694617425XXXXgamehistoryCMUT_V2)
     * - request_time (int64, required): Timestamp of request time.
     *
     * @param wagerCode The unique identifier for the wager.
     * @return a String containing the game history in HTML format.
     */
    @Override
    public String gameHistory(String wagerCode) {
        try {
            long requestTime = System.currentTimeMillis() / 1000L;
            String operatorCode = gscConfig.getOperatorCode();
            String sign = HashUtil.md5(requestTime + gscConfig.getSecretKey() + "gamehistory" + operatorCode);

            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(gscConfig.getOperatorUrl())
                    .append("/api/operators/").append(wagerCode).append("/game-history?")
                    .append("operator_code=").append(operatorCode)
                    .append("&sign=").append(sign)
                    .append("&request_time=").append(requestTime);

            String urlString = urlBuilder.toString();
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                return response.toString();
            } else {
                // Handle non-OK response
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Retrieves the list of available products.
     * <p>
     * Endpoint: GET {{operator_url}}/api/operators/available-products
     * <p>
     * Parameters:
     * - operator_code (String, required): The operator's unique identifier, used as the backend login username.
     * - sign (String, required): md5(request_time + secret_key + "productlist" + operator_code).
     * Example: md5(1694617425XXXXproductlistCMUT_V2)
     * - request_time (int64, required): Timestamp of request time.
     * - offset (int, optional): Start record number of this retrieval.
     * - size (int, optional, default: 500): Number of records retrieved this time.
     *
     * @return a List of GSCProductInfo objects containing the available products.
     */
    @Override
    public List<GSCProductInfo> productList(Integer offset, Integer size) {
        try {
            long requestTime = System.currentTimeMillis() / 1000L;
            String operatorCode = gscConfig.getOperatorCode();
            String sign = HashUtil.md5(requestTime + gscConfig.getSecretKey() + "productlist" + operatorCode);

            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(gscConfig.getOperatorUrl())
                    .append("/api/operators/available-products?")
                    .append("operator_code=").append(operatorCode)
                    .append("&sign=").append(sign)
                    .append("&request_time=").append(requestTime);

            if (offset != null) {
                urlBuilder.append("&offset=").append(offset);
            }
            if (size != null) {
                urlBuilder.append("&size=").append(size);
            }

            String urlString = urlBuilder.toString();
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                Gson gson = new Gson();
                return gson.fromJson(response.toString(), new TypeToken<List<GSCProductInfo>>() {
                }.getType());
            } else {
                // Handle non-OK response
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Launches a game for the user.
     * <p>
     * Endpoint: POST {{operator_url}}/api/operators/launch-game
     * <p>
     * Parameters:
     * - operator_code (String, required): The operator's unique identifier, used as the backend login username.
     * - member_account (String, required): The unique identifier of members within the operator.
     * - password (String, required): The member's password in the operator's system, used for identity verification.
     * - nickname (String, optional): The member's nickname displayed in the game.
     * - currency (String, required): The currency used by the member in the game. Ensure the currency is supported by the provider.
     * - game_code (String, optional): The unique identifier in the game list API provided by the provider.
     * - product_code (int, required): The unique identifier of the product.
     * - game_type (String, required): Refer to the Game Type.
     * - language_code (String, optional, default: 0): The language code of the member.
     * - ip (String, required): The IP address of the member.
     * - platform (String, required): Platform type (web, desktop, mobile).
     * - sign (String, required): md5(request_time + secret_key + "launchgame" + operator_code).
     * Example: md5(1694617425XXXXlaunchgameCMUT_V2)
     * - request_time (int, required): Timestamp of request time (in seconds).
     * - operator_lobby_url (String, required): URL of the client site.
     *
     * @param nickname The member's nickname displayed in the game.
     * @return a GSCGameLaunchResponse object containing the launch game response.
     */
    @Override
    public GSCGameLaunchResponse launchGame(String nickname, String memberPassword, String productCode, String gameCode, String gameType, String currency, String platform, String ipClient) {
        try {
            long requestTime = System.currentTimeMillis() / 1000L;
            String operatorCode = gscConfig.getOperatorCode();
            // GSC spec requires the member's password — sending empty returns
            // 400 invalid parameters. Caller passes users.password (MD5).
            String password = memberPassword != null ? memberPassword : "";
            String sign = HashUtil.md5(requestTime + gscConfig.getSecretKey() + "launchgame" + operatorCode);

            String urlString = gscConfig.getOperatorUrl() + "/api/operators/launch-game";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            JsonObject jsonInput = new JsonObject();
            jsonInput.addProperty("operator_code", operatorCode);
            jsonInput.addProperty("member_account", nickname);
            jsonInput.addProperty("password", password);
            jsonInput.addProperty("nickname", nickname);
            jsonInput.addProperty("currency", currency);
            jsonInput.addProperty("game_code", gameCode);
            jsonInput.addProperty("product_code", 1002);
            jsonInput.addProperty("game_type", gameType);
//            jsonInput.addProperty("language_code", GSC.LanguageCode.VIETNAMESE.getCode()); // Assuming default value is 0
            jsonInput.addProperty("ip", ipClient);
            jsonInput.addProperty("platform", platform);
            jsonInput.addProperty("sign", sign);
            jsonInput.addProperty("request_time", requestTime);
            // Use the configured operator lobby URL (game_config.gsc_game.operatorLobbyUrl).
            // The hardcoded "https://play.sunkr.bet" was the cause of the
            // table-keeps-refreshing bug: Evo's iframe POSTs back to
            // <operator_lobby_url>/gsc/v1/api/seamless/balance, but only
            // play.sunkr.club is wired in nginx. .bet returned 404 → balance
            // poll failed → iframe reload-looped.
            jsonInput.addProperty("operator_lobby_url", gscConfig.getOperatorLobbyUrl());

            String jsonInputString = jsonInput.toString();

            System.out.println("launchGame request body: " + jsonInputString);
            String data = HttpUtils.postData(urlString, jsonInputString);
            System.out.println("launchGame response: " + data);

            Gson gson = new Gson();
            return gson.fromJson(data, GSCGameLaunchResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Launches the super lobby for a specific user.
     *
     * @param nickname        The member's nickname displayed in the game.
     * @param currency        The currency used by the member in the game.
     * @param platform        The platform type (e.g., web, desktop, mobile).
     * @return A string containing the URL for launching the super lobby.
     * @throws Exception If an error occurs during the request.
     */
    /**
     * Launches the super lobby for a specific user.
     * <p>
     * Endpoint: POST {{operator_url}}/superlobby/launch
     * <p>
     * Parameters:
     * - operator_code (String, required): The operator's unique identifier, used as the backend login username.
     * - member_account (String, required): The unique identifier of members within the operator.
     * - nickname (String, required): The member's nickname displayed in the game.
     * - currency (String, required): The currency used by the member in the game. Ensure the currency is supported by the provider.
     * - language_code (String, optional, default: 0): The language code of the member.
     * - platform (String, required): Platform type (web, desktop, mobile).
     * - sign (String, required): md5(request_time + secret_key + "launchsuperlobby" + operator_code).
     * Example: md5(1694617425XXXXlaunchsuperlobbyCMUT_V2)
     * - request_time (int, required): Timestamp of request time (in seconds).
     * - operator_lobby_url (String, required): URL of the client site.
     * <p>
     * @param nickname        The member's nickname displayed in the game.
     * @param platform        The platform type (e.g., web, desktop, mobile).
     * @return A LaunchSuperLobbyResponse object containing the URL for launching the super lobby.
     * @throws Exception If an error occurs during the request.
     */
    public LaunchSuperLobbyResponse launchSuperLobby(String nickname, String platform) throws Exception {
        try {
            long requestTime = System.currentTimeMillis() / 1000L;
            String operatorCode = gscConfig.getOperatorCode();
            String currency = gscConfig.getCurrency();
            String sign = HashUtil.md5(requestTime + gscConfig.getSecretKey() + "launchsuperlobby" + operatorCode);

            String urlString = gscConfig.getOperatorUrl() + "/superlobby/launch";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            JsonObject jsonInput = new JsonObject();
            jsonInput.addProperty("operator_code", operatorCode);
            jsonInput.addProperty("member_account", nickname);
            jsonInput.addProperty("nickname", nickname);
            jsonInput.addProperty("currency", currency);
            jsonInput.addProperty("language_code",  GSC.LanguageCode.VIETNAMESE.getCode()); // Assuming default value is 0
            jsonInput.addProperty("platform", platform);
            jsonInput.addProperty("sign", sign);
            jsonInput.addProperty("request_time", requestTime);
            jsonInput.addProperty("operator_lobby_url", gscConfig.getOperatorLobbyUrl());

            String jsonInputString = jsonInput.toString();
            String data = HttpUtils.postData(urlString, jsonInputString);
            Gson gson = new Gson();
            return gson.fromJson(data, LaunchSuperLobbyResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
            throw e;
        }
    }
}
