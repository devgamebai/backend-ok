/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.Debug
 *  com.google.gson.Gson
 *  com.vinplay.usercore.utils.GameThirdPartyInit
 *  com.vinplay.utils.TelegramUtil
 *  com.vinplay.vbee.common.statics.Consts
 *  javax.servlet.ServletException
 *  javax.servlet.http.HttpServletRequest
 *  javax.servlet.http.HttpServletResponse
 *  org.eclipse.jetty.server.Request
 *  org.eclipse.jetty.server.handler.AbstractHandler
 */
package game.Jetty.handle;

import bitzero.util.common.business.Debug;
import com.google.gson.Gson;
import com.vinplay.usercore.utils.GameThirdPartyInit;
import com.vinplay.utils.TelegramUtil;
import com.vinplay.vbee.common.statics.Consts;
import game.GameConfig.ConfigGame.MinigameConfig.MinipokerGameConfig;
import game.GameConfig.ConfigGame.MinigameConfig.Slot3x3GameConfig;
import game.GameConfig.ConfigGame.SlotMultiJackpotConfig;
import game.GameConfig.GameConfig;
import game.Jetty.FunctionUtils;
import game.Jetty.JettyResponse;
import game.Jetty.JettyUtils;
import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class ChangeConfigCPHandler
extends AbstractHandler {
    private Gson gson = new Gson();

    private String getIpAddress(HttpServletRequest request) {
        String[] arrayIp;
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        String clientIp = null;
        if (ipAddress != null && !"".equals(ipAddress) && (arrayIp = ipAddress.split(",")).length > 0) {
            clientIp = arrayIp[0].trim();
        }
        return clientIp;
    }

    private String actionStr(byte action) {
        switch (action) {
            case 0: {
                return "GET_MINIPOKER_GAME_CONFIG";
            }
            case 1: {
                return "SET_MINIPOKER_GAME_CONFIG";
            }
            case 2: {
                return "GET_SLOT3X3_GAME_CONFIG";
            }
            case 3: {
                return "SET_SLOT3X3_GAME_CONFIG";
            }
            case 4: {
                return "GET_MULTI_JACKPOT_GAME_CONFIG";
            }
            case 5: {
                return "SET_MULTI_JACKPOT_GAME_CONFIG";
            }
        }
        return action + "";
    }

    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String ip = this.getIpAddress(request);
        if (!Consts.IP_SERVER.contains(ip)) {
            return;
        }
        String input = request.getQueryString();
        Map<String, String> params = FunctionUtils.splitQuery(input);
        byte action = JettyUtils.getByte(params.get("action"));
        if ("pro".equals(GameThirdPartyInit.enviroment)) {
            TelegramUtil.warningCheat((String)("Setting config , action = " + this.actionStr(action) + " , param = " + params.toString()));
        }
        try {
            switch (action) {
                case 0: {
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, this.gson.toJson(GameConfig.getInstance().minipokerGameConfig)));
                    return;
                }
                case 1: {
                    String data = params.get("data");
                    if (data.length() < 5) {
                        JettyUtils.send(baseRequest, response, new JettyResponse(98, "Kh\u00c3\u00b4ng \u00c4\u2018\u00c3\u00bang format Json minipoker config"));
                        return;
                    }
                    try {
                        GameConfig.getInstance().minipokerGameConfig = (MinipokerGameConfig)this.gson.fromJson(data, MinipokerGameConfig.class);
                        GameConfig.getInstance().setFileConfig("MinipokerGameConfig.json", GameConfig.getInstance().minipokerGameConfig);
                        JettyUtils.send(baseRequest, response, new JettyResponse(0, "ok"));
                    }
                    catch (Exception e) {
                        Debug.trace((Object[])new Object[]{e});
                        JettyUtils.send(baseRequest, response, new JettyResponse(98, "Kh\u00c3\u00b4ng \u00c4\u2018\u00c3\u00bang format Json minipoker config"));
                    }
                    return;
                }
                case 2: {
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, this.gson.toJson(GameConfig.getInstance().slot3x3GameConfig)));
                    return;
                }
                case 3: {
                    String data = params.get("data");
                    if (data.length() < 5) {
                        JettyUtils.send(baseRequest, response, new JettyResponse(98, "Kh\u00c3\u00b4ng \u00c4\u2018\u00c3\u00bang format Json slot3x3 config"));
                        return;
                    }
                    try {
                        GameConfig.getInstance().slot3x3GameConfig = (Slot3x3GameConfig)this.gson.fromJson(data, Slot3x3GameConfig.class);
                        GameConfig.getInstance().setFileConfig("Slot3x3GameConfig.json", GameConfig.getInstance().slot3x3GameConfig);
                        JettyUtils.send(baseRequest, response, new JettyResponse(0, "ok"));
                    }
                    catch (Exception e) {
                        Debug.trace((Object[])new Object[]{e});
                        JettyUtils.send(baseRequest, response, new JettyResponse(98, "Kh\u00c3\u00b4ng \u00c4\u2018\u00c3\u00bang format Json slot3x3 config"));
                    }
                    return;
                }
                case 4: {
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, this.gson.toJson(GameConfig.getInstance().slotMultiJackpotConfig)));
                    return;
                }
                case 5: {
                    String data = params.get("data");
                    if (data.length() < 5) {
                        JettyUtils.send(baseRequest, response, new JettyResponse(98, "Kh\u00c3\u00b4ng \u00c4\u2018\u00c3\u00bang format Json multijackpot slot config"));
                        return;
                    }
                    try {
                        GameConfig.getInstance().slotMultiJackpotConfig = (SlotMultiJackpotConfig)this.gson.fromJson(data, SlotMultiJackpotConfig.class);
                        GameConfig.getInstance().setFileConfig("SlotMultiJackpotConfig.json", GameConfig.getInstance().minipokerGameConfig);
                        JettyUtils.send(baseRequest, response, new JettyResponse(0, "ok"));
                    }
                    catch (Exception e) {
                        Debug.trace((Object[])new Object[]{e});
                        JettyUtils.send(baseRequest, response, new JettyResponse(98, "Kh\u00c3\u00b4ng \u00c4\u2018\u00c3\u00bang format Json  multijackpot slot config"));
                    }
                    return;
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        JettyUtils.send(baseRequest, response, new JettyResponse(98, "\u00c4\ufffd\u00c3\u00a3 c\u00c3\u00b3 l\u00e1\u00bb\u2014i x\u00e1\u00ba\u00a3y ra."));
    }
}

