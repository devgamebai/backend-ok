/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonObject
 */
package game.third.usecase.game568win.service.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import game.third.usecase.config.Game568winConfig;
import game.third.usecase.config.ThirdPartyLoad;
import game.third.usecase.game568win.request.GetGameList;
import game.third.usecase.game568win.request.Login;
import game.third.usecase.game568win.request.RegisterAgent;
import game.third.usecase.game568win.request.RegisterPlayer;
import game.third.usecase.game568win.request.UpdateAgent;
import game.third.usecase.game568win.response.GetGameListResult;
import game.third.usecase.game568win.response.LoginResult;
import game.third.usecase.game568win.response.RegisterPlayerResult;
import game.third.usecase.game568win.response.UpdateAgentResult;
import game.third.usecase.game568win.service.APIGame568winService;
import game.third.utils.HttpUtils;
import java.net.HttpURLConnection;
import java.net.URL;

public class APIGame568winServiceImpl
implements APIGame568winService {
    public void checkAndCreateAgent() {
        Game568winConfig winConfig = ThirdPartyLoad.getGame568winConfig();
        try {
            if (winConfig == null) {
                return;
            }
            if (!winConfig.getAgent().isEmpty()) {
                RegisterAgent request = new RegisterAgent();
                request.setUsername(winConfig.getAgent());
                request.setPassword(winConfig.getAgentPassword());
                request.setCurrency(winConfig.getCurrency());
                request.setMin(winConfig.getMin());
                request.setMax(winConfig.getMax());
                request.setMaxPerMatch(winConfig.getMaxPerMatch());
                request.setCasinoTableLimit(winConfig.getCasinoTableLimit());
                request.setServerId(winConfig.getServerId());
                UpdateAgentResult updateAgentResult = this.RegisterAgent(request);
                if (updateAgentResult == null) {
                    System.out.println("Register agent fail");
                    return;
                }
                if (updateAgentResult.getError().getId() == 0) {
                    System.out.println("Register agent success");
                } else if (updateAgentResult.getError().getId() == 4103) {
                    UpdateAgent requestUpdate = new UpdateAgent();
                    requestUpdate.setUsername(winConfig.getAgent());
                    requestUpdate.setMin(winConfig.getMin());
                    requestUpdate.setMax(winConfig.getMax());
                    requestUpdate.setMaxPerMatch(winConfig.getMaxPerMatch());
                    requestUpdate.setCasinoTableLimit(winConfig.getCasinoTableLimit());
                    requestUpdate.setServerId(winConfig.getServerId());
                    updateAgentResult = this.UpdateAgent(requestUpdate);
                    if (updateAgentResult.getError().getId() == 0) {
                        System.out.println("Update agent success");
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public UpdateAgentResult RegisterAgent(RegisterAgent request) {
        Game568winConfig winConfig = ThirdPartyLoad.getGame568winConfig();
        try {
            String urlString = winConfig.getServer() + "/web-root/restricted/agent/register-agent.aspx";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setDoOutput(true);
            JsonObject jsonInput = new JsonObject();
            jsonInput.addProperty("Username", request.getUsername());
            jsonInput.addProperty("Password", request.getPassword());
            jsonInput.addProperty("Currency", request.getCurrency());
            jsonInput.addProperty("Min", (Number)request.getMin());
            jsonInput.addProperty("Max", (Number)request.getMax());
            jsonInput.addProperty("MaxPerMatch", (Number)request.getMaxPerMatch());
            jsonInput.addProperty("CasinoTableLimit", (Number)request.getCasinoTableLimit());
            jsonInput.addProperty("ServerId", request.getServerId());
            jsonInput.addProperty("CompanyKey", winConfig.getCompanyKey());
            String jsonInputString = jsonInput.toString();
            System.out.println("RegisterAgent request body: " + jsonInputString);
            String data = HttpUtils.postData(urlString, jsonInputString);
            System.out.println("RegisterAgent response: " + data);
            Gson gson = new Gson();
            return (UpdateAgentResult)gson.fromJson(data, UpdateAgentResult.class);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public UpdateAgentResult UpdateAgent(UpdateAgent request) {
        Game568winConfig winConfig = ThirdPartyLoad.getGame568winConfig();
        try {
            String urlString = winConfig.getServer() + "/web-root/restricted/agent/update-agent-preset-bet-settings.aspx";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setDoOutput(true);
            JsonObject jsonInput = new JsonObject();
            jsonInput.addProperty("Username", request.getUsername());
            jsonInput.addProperty("Min", (Number)request.getMin());
            jsonInput.addProperty("Max", (Number)request.getMax());
            jsonInput.addProperty("MaxPerMatch", (Number)request.getMaxPerMatch());
            jsonInput.addProperty("CasinoTableLimit", (Number)request.getCasinoTableLimit());
            jsonInput.addProperty("ServerId", request.getServerId());
            jsonInput.addProperty("CompanyKey", winConfig.getCompanyKey());
            String jsonInputString = jsonInput.toString();
            System.out.println("UpdateAgent request body: " + jsonInputString);
            String data = HttpUtils.postData(urlString, jsonInputString);
            System.out.println("UpdateAgent response: " + data);
            Gson gson = new Gson();
            return (UpdateAgentResult)gson.fromJson(data, UpdateAgentResult.class);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public RegisterPlayerResult RegisterPlayer(RegisterPlayer request) {
        Game568winConfig winConfig = ThirdPartyLoad.getGame568winConfig();
        try {
            String urlString = winConfig.getServer() + "/web-root/restricted/player/register-player.aspx";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setDoOutput(true);
            JsonObject jsonInput = new JsonObject();
            jsonInput.addProperty("Username", request.getUsername());
            jsonInput.addProperty("DisplayName", request.getDisplayName());
            jsonInput.addProperty("UserGroup", request.getUserGroup());
            jsonInput.addProperty("Agent", winConfig.getAgent());
            jsonInput.addProperty("ServerId", winConfig.getServerId());
            jsonInput.addProperty("CompanyKey", winConfig.getCompanyKey());
            String jsonInputString = jsonInput.toString();
            System.out.println("RegisterPlayer request body: " + jsonInputString);
            String data = HttpUtils.postData(urlString, jsonInputString);
            System.out.println("RegisterPlayer response: " + data);
            Gson gson = new Gson();
            return (RegisterPlayerResult)gson.fromJson(data, RegisterPlayerResult.class);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public LoginResult Login(Login request) {
        Game568winConfig winConfig = ThirdPartyLoad.getGame568winConfig();
        try {
            String urlString = winConfig.getServer() + "/web-root/restricted/player/login.aspx";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setDoOutput(true);
            JsonObject jsonInput = new JsonObject();
            jsonInput.addProperty("Username", request.getUsername());
            jsonInput.addProperty("Portfolio", request.getPortfolio());
            jsonInput.addProperty("IsWapSports", "false");
            jsonInput.addProperty("ServerId", request.getServerId());
            jsonInput.addProperty("CompanyKey", winConfig.getCompanyKey());
            String jsonInputString = jsonInput.toString();
            System.out.println("Login request body: " + jsonInputString);
            String data = HttpUtils.postData(urlString, jsonInputString);
            System.out.println("Login response: " + data);
            Gson gson = new Gson();
            return (LoginResult)gson.fromJson(data, LoginResult.class);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public GetGameListResult GetGameList(GetGameList request) {
        Game568winConfig winConfig = ThirdPartyLoad.getGame568winConfig();
        try {
            String urlString = winConfig.getServer() + "/web-root/restricted/information/get-game-list.aspx";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setDoOutput(true);
            JsonObject jsonInput = new JsonObject();
            jsonInput.addProperty("GpId", (Number)request.getGpId());
            jsonInput.addProperty("IsGetAll", request.getIsGetAll());
            jsonInput.addProperty("CompanyKey", winConfig.getCompanyKey());
            String jsonInputString = jsonInput.toString();
            System.out.println("GetGameListResult request body: " + jsonInputString);
            String data = HttpUtils.postData(urlString, jsonInputString);
            System.out.println("GetGameListResult response: " + data);
            Gson gson = new Gson();
            return (GetGameListResult)gson.fromJson(data, GetGameListResult.class);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

