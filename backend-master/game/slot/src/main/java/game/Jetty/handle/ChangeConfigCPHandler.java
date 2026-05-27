package game.Jetty.handle;

import bitzero.util.common.business.Debug;
import com.google.gson.Gson;
import com.vinplay.usercore.utils.GameThirdPartyInit;
import com.vinplay.utils.TelegramUtil;
import com.vinplay.vbee.common.statics.Consts;

import game.GameConfig.GameConfig;
import game.GameConfig.BotJackpotConfig.*;
import game.GameConfig.SlotConfig.*;
import game.Jetty.FunctionUtils;
import game.Jetty.JettyErrorCode;
import game.Jetty.JettyResponse;
import game.Jetty.JettyUtils;
import game.Jetty.action.ChangeConfigAction;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;


public class ChangeConfigCPHandler extends AbstractHandler {
    private Gson gson = new Gson();
    private String getIpAddress(HttpServletRequest request) {
		String ipAddress = request.getHeader("X-FORWARDED-FOR");
		if (ipAddress == null) {
			ipAddress = request.getRemoteAddr();
		}
		String clientIp = null;
		if (ipAddress != null && !"".equals(ipAddress)) {
			String[] arrayIp = ipAddress.split(",");
			if (arrayIp.length > 0) {
				clientIp = arrayIp[0].trim();
			}
		}
		return clientIp;
	}
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) throws IOException,
            ServletException {
		// get ipaddress
		String ip = getIpAddress(request);
		if (!Consts.IP_SERVER.contains(ip)) {
			return;
		}
        // Validate date from request
        String input = request.getQueryString();
        Map<String, String> params = FunctionUtils.splitQuery(input);
        byte action = JettyUtils.getByte(params.get("action"));
     // notification
		if ("pro".equals(GameThirdPartyInit.enviroment) && action % 2 != 0) {
			TelegramUtil.warningCheat("Setting config , action = " + actionStr(action) + " , param = " + params.toString());
		}
        try {
            switch (action){
                case ChangeConfigAction.GET_SLOT_7ICON_CONFIG:{
                    JettyUtils.send(baseRequest, response, new JettyResponse(
                            JettyErrorCode.SUCCESS, gson.toJson(GameConfig.getInstance().slot7IconConfig)));
                    return;
                }
                case ChangeConfigAction.SET_SLOT_7ICON_CONFIG:{
                    String data = params.get("data");
                    if(data.length() <5){
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.UNKOWN_ERROR, "Không đúng format Json slot 7 icon"));
                        return;
                    }
                    try {
                        GameConfig.getInstance().slot7IconConfig = gson.fromJson(data, Slot7IconConfig.class);
                        GameConfig.getInstance().setFileConfig("Slot7IconConfig.json", GameConfig.getInstance().slot7IconConfig);
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.SUCCESS, "ok"));
                    }catch (Exception e){
                        Debug.trace(e);
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.UNKOWN_ERROR, "Không đúng format Json slot 7 icon"));
                    }
                    return;
                }
                case ChangeConfigAction.GET_SLOT_7ICON_WILD_CONFIG:{
                    JettyUtils.send(baseRequest, response, new JettyResponse(
                            JettyErrorCode.SUCCESS, gson.toJson(GameConfig.getInstance().slot7IconWildConfig)));
                    return;
                }
                case ChangeConfigAction.SET_SLOT_7ICON_WILD_CONFIG:{
                    String data = params.get("data");
                    if(data.length() <5){
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.UNKOWN_ERROR, "Không đúng format Json slot 7 icon wild"));
                        return;
                    }
                    try {
                        GameConfig.getInstance().slot7IconWildConfig = gson.fromJson(data, Slot7IconWildConfig.class);

                        GameConfig.getInstance().setFileConfig("Slot7IconWildConfig.json",
                                GameConfig.getInstance().slot7IconWildConfig);
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.SUCCESS, "ok"));
                    }catch (Exception e){
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.UNKOWN_ERROR, "Không đúng format Json slot 7 icon wild"));
                        Debug.trace(e);
                    }
                    return;
                }
                case ChangeConfigAction.GET_SLOT_9ICON_CONFIG:{
                    JettyUtils.send(baseRequest, response, new JettyResponse(
                            JettyErrorCode.SUCCESS, gson.toJson(GameConfig.getInstance().slot9IconConfig)));
                    return;
                }
                case ChangeConfigAction.SET_SLOT_9ICON_CONFIG:{
                    String data = params.get("data");
                    if(data.length() <5){
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.UNKOWN_ERROR, "Không đúng format Json slot 9 icon"));
                        return;
                    }
                    try {
                        GameConfig.getInstance().slot9IconConfig = gson.fromJson(data, Slot9IconConfig.class);
                        GameConfig.getInstance().setFileConfig("Slot9IconConfig.json",
                                GameConfig.getInstance().slot9IconConfig);
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.SUCCESS, "ok"));
                    }catch (Exception e){
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.UNKOWN_ERROR, "Không đúng format Json slot 9 icon"));
                        Debug.trace(e);
                    }
                    return;
                }
                case ChangeConfigAction.GET_SLOT_11ICON_WILD_LIENTUC_CONFIG:{
                    JettyUtils.send(baseRequest, response, new JettyResponse(
                            JettyErrorCode.SUCCESS, gson.toJson(GameConfig.getInstance().slot11IconWildLienTucConfig)));
                    return;
                }
                case ChangeConfigAction.SET_SLOT_11ICON_WILD_LIENTUC_CONFIG:{
                    String data = params.get("data");
                    if(data.length() <5){
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.UNKOWN_ERROR, "Không đúng format Json slot 11 icon"));
                        return;
                    }
                    try {
                        GameConfig.getInstance().slot11IconWildLienTucConfig = gson.fromJson(data, Slot11IconWildLienTucConfig.class);
                        GameConfig.getInstance().setFileConfig("Slot11IconWildLienTucConfig.json",
                                GameConfig.getInstance().slot11IconWildLienTucConfig);
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.SUCCESS, "ok"));
                    }catch (Exception e){
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.UNKOWN_ERROR, "Không đúng format Json slot 11 icon"));
                        Debug.trace(e);
                    }
                    return;
                }
                case ChangeConfigAction.GET_MULTI_JACKPOT_CONFIG:{
                    JettyUtils.send(baseRequest, response, new JettyResponse(
                            JettyErrorCode.SUCCESS, gson.toJson(GameConfig.getInstance().slotMultiJackpotConfig)));
                    return;
                }
                case ChangeConfigAction.SET_MULTI_JACKPOT_CONFIG:{
                    String data = params.get("data");
                    if(data.length() <5){
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.UNKOWN_ERROR, "Không đúng format Json SlotMultiJackpotConfig"));
                        return;
                    }
                    try {
                        GameConfig.getInstance().slotMultiJackpotConfig = gson.fromJson(data, SlotMultiJackpotConfig.class);
                        GameConfig.getInstance().setFileConfig("SlotMultiJackpotConfig.json",
                                GameConfig.getInstance().slotMultiJackpotConfig);
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.SUCCESS, "ok"));
                    }catch (Exception e){
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.UNKOWN_ERROR, "Không đúng format Json SlotMultiJackpotConfig"));
                        Debug.trace(e);
                    }
                    return;
                }
            }
        }catch (Exception e){

        }

        JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.UNKOWN_ERROR, "Đã có lỗi xảy ra."));
    }
    
	private String actionStr(byte action) {
		switch (action) {
		case ChangeConfigAction.GET_SLOT_7ICON_CONFIG: {
			return "GET_SLOT_7ICON_CONFIG";
		}
		case ChangeConfigAction.SET_SLOT_7ICON_CONFIG: {
			return "SET_SLOT_7ICON_CONFIG";
		}
		case ChangeConfigAction.GET_SLOT_7ICON_WILD_CONFIG: {
			return "GET_SLOT_7ICON_WILD_CONFIG";
		}
		case ChangeConfigAction.SET_SLOT_7ICON_WILD_CONFIG: {
			return "SET_SLOT_7ICON_WILD_CONFIG";
		}
		case ChangeConfigAction.GET_SLOT_9ICON_CONFIG: {
			return "GET_SLOT_9ICON_CONFIG";
		}
		case ChangeConfigAction.SET_SLOT_9ICON_CONFIG: {
			return "SET_SLOT_9ICON_CONFIG";
		}
		case ChangeConfigAction.GET_SLOT_11ICON_WILD_LIENTUC_CONFIG: {
			return "GET_SLOT_11ICON_WILD_LIENTUC_CONFIG";
		}
		case ChangeConfigAction.SET_SLOT_11ICON_WILD_LIENTUC_CONFIG: {
			return "SET_SLOT_11ICON_WILD_LIENTUC_CONFIG";
		}
		case ChangeConfigAction.GET_MULTI_JACKPOT_CONFIG: {
			return "GET_MULTI_JACKPOT_CONFIG";
		}
		case ChangeConfigAction.SET_MULTI_JACKPOT_CONFIG: {
			return "SET_MULTI_JACKPOT_CONFIG";
		}
		}
		return action + "";
	}
}
