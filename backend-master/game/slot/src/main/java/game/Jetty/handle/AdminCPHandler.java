package game.Jetty.handle;

import com.google.gson.Gson;
import com.vinplay.usercore.dao.GameConfigDao;
import com.vinplay.usercore.dao.impl.GameConfigDaoImpl;
import com.vinplay.usercore.utils.GameThirdPartyInit;
import com.vinplay.utils.TelegramUtil;
import com.vinplay.vbee.common.statics.Consts;

import game.Jetty.FunctionUtils;
import game.Jetty.JettyErrorCode;
import game.Jetty.JettyResponse;
import game.Jetty.JettyUtils;
import game.Jetty.action.AdminAction;
import game.Jetty.model.FundData;
import game.Jetty.model.JackpotData;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;


public class AdminCPHandler extends AbstractHandler {
	
    private static final Gson gson = new Gson();
    
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

    private String actionStr(byte action) {
    	switch (action){
        case AdminAction.GET_JACKPOT_BITCOIN:{
            return "GET_JACKPOT_BITCOIN";
        }
        case AdminAction.SET_JACKPOT_BITCOIN:{
        	 return "SET_JACKPOT_BITCOIN";
        }
        case AdminAction.GET_FUND_BITCOIN:{
        	 return "GET_FUND_BITCOIN";
        }
        case AdminAction.SET_FUND_BITCOIN:{
        	 return "SET_FUND_BITCOIN";
        }
        case AdminAction.GET_JACKPOT_TAYDU:{
        	 return "GET_JACKPOT_TAYDU";
        }
        case AdminAction.SET_JACKPOT_TAYDU:{
        	 return "SET_JACKPOT_DUAXE";
        }
        case AdminAction.GET_FUND_TAYDU:{
        	 return "GET_FUND_TAYDU";
        }
        case AdminAction.SET_FUND_TAYDU:{
        	 return "SET_FUND_DUAXE";
        }
        case AdminAction.GET_JACKPOT_CHIMDIEN:{
        	 return "GET_JACKPOT_CHIMDIEN";
        }
        case AdminAction.SET_JACKPOT_CHIMDIEN:{
        	 return "SET_JACKPOT_CHIMDIEN";
        }
        case AdminAction.GET_FUND_CHIMDIEN:{
        	 return "GET_FUND_CHIMDIEN";
        }
        case AdminAction.SET_FUND_CHIMDIEN:{
        	 return "SET_FUND_CHIMDIEN";
        }
        case AdminAction.GET_JACKPOT_THANTAI:{
        	 return "GET_JACKPOT_THANTAI";
        }
        case AdminAction.SET_JACKPOT_THANTAI:{
        	 return "SET_JACKPOT_THANTAI";
        }
        case AdminAction.GET_FUND_THANTAI:{
        	 return "GET_FUND_THANTAI";
        }
        case AdminAction.SET_FUND_THANTAI:{
        	 return "SET_FUND_THANTAI";
        }
        case AdminAction.GET_JACKPOT_THETHAO:{
        	 return "GET_JACKPOT_THETHAO";
        }
        case AdminAction.SET_JACKPOT_THETHAO:{
        	 return "SET_JACKPOT_THETHAO";
        }
        case AdminAction.GET_FUND_THETHAO:{
        	 return "GET_FUND_THETHAO";
        }
        case AdminAction.SET_FUND_THETHAO:{
        	 return "SET_FUND_THETHAO";
        }
        case AdminAction.GET_JACKPOT_CHIEMTINH:{
        	 return "GET_JACKPOT_CHIEMTINH";
        }
        case AdminAction.SET_JACKPOT_CHIEMTINH:{
        	 return "SET_JACKPOT_CHIEMTINH";
        }
        case AdminAction.GET_FUND_CHIEMTINH:{
        	 return "GET_FUND_CHIEMTINH";
        }
        case AdminAction.SET_FUND_CHIEMTINH:{
        	 return "SET_FUND_CHIEMTINH";
        }
        case AdminAction.GET_JACKPOT_BIKINI:{
            return "GET_JACKPOT_BIKINI";
        }
        case AdminAction.SET_JACKPOT_BIKINI:{
            return "SET_JACKPOT_BIKINI";
        }
        case AdminAction.GET_FUND_BIKINI:{
            return "GET_FUND_BIKINI";
        }
        case AdminAction.SET_FUND_BIKINI:{
            return "SET_FUND_BIKINI";
        }
    }
		return action + "";
	}

    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) throws IOException,
            ServletException {
		// get ipAddress
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
                case AdminAction.GET_JACKPOT_BITCOIN:{
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS,
                            gson.toJson(AdminCPUtils.getJackpotDataSlotBitcoin())));
                    return;
                }
                case AdminAction.SET_JACKPOT_BITCOIN:{
                    String data = params.get("data");
                    if(data.length() <5){
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.UNKOWN_ERROR, "Không đúng format jackpot bitcoin"));
                        return;
                    }
                    JackpotData jackpotData = gson.fromJson(data, JackpotData.class);
                    AdminCPUtils.setJackpotDataSlotBitcoin(jackpotData);
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS, "ok"));
                    return;
                }
                case AdminAction.GET_FUND_BITCOIN:{
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS,
                            gson.toJson(AdminCPUtils.getFundDataSlotBitcoin())));
                    return;
                }
                case AdminAction.SET_FUND_BITCOIN:{
                    String data = params.get("data");
                    if(data.length() <5){
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.UNKOWN_ERROR, "Không đúng format fund bitcoin"));
                        return;
                    }
                    FundData fundData = gson.fromJson(data,FundData.class);
                    AdminCPUtils.setFundDataSlotBitcoin(fundData);
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS, "ok"));
                    return;
                }
                case AdminAction.GET_JACKPOT_TAYDU:{
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS,
                            gson.toJson(AdminCPUtils.getJackpotDataSlotTayDu())));
                    return;
                }
                case AdminAction.SET_JACKPOT_TAYDU:{
                    String data = params.get("data");
                    if(data.length() <5){
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.UNKOWN_ERROR, "Không đúng format jackpot tay du"));
                        return;
                    }
                    JackpotData jackpotData = gson.fromJson(data, JackpotData.class);
                    AdminCPUtils.setJackpotDataSlotTayDu(jackpotData);
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS, "ok"));
                    return;
                }
                case AdminAction.GET_FUND_TAYDU:{
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS,
                            gson.toJson(AdminCPUtils.getFundDataSlotTayDu())));
                    return;
                }
                case AdminAction.SET_FUND_TAYDU:{
                    String data = params.get("data");
                    if(data.length() <5){
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.UNKOWN_ERROR, "Không đúng format fund tay du"));
                        return;
                    }
                    FundData fundData = gson.fromJson(data,FundData.class);
                    AdminCPUtils.setFundDataSlotTayDu(fundData);
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS, "ok"));
                    return;
                }
                case AdminAction.GET_JACKPOT_CHIMDIEN:{
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS,
                            gson.toJson(AdminCPUtils.getJackpotDataSlotChimDien())));
                    return;
                }
                case AdminAction.SET_JACKPOT_CHIMDIEN:{
                    String data = params.get("data");
                    if(data.length() <5){
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.UNKOWN_ERROR, "Không đúng format jackpot chim dien"));
                        return;
                    }
                    JackpotData jackpotData = gson.fromJson(data, JackpotData.class);
                    AdminCPUtils.setJackpotDataSlotChimDien(jackpotData);
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS, "ok"));
                    return;
                }
                case AdminAction.GET_FUND_CHIMDIEN:{
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS,
                            gson.toJson(AdminCPUtils.getFundDataSlotChimDien())));
                    return;
                }
                case AdminAction.SET_FUND_CHIMDIEN:{
                    String data = params.get("data");
                    if(data.length() <5){
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.UNKOWN_ERROR, "Không đúng format fund chim dien"));
                        return;
                    }
                    FundData fundData = gson.fromJson(data,FundData.class);
                    AdminCPUtils.setFundDataSlotChimDien(fundData);
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS, "ok"));
                    return;
                }
                case AdminAction.GET_JACKPOT_THANTAI:{
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS,
                            gson.toJson(AdminCPUtils.getJackpotDataSlotThanTai())));
                    return;
                }
                case AdminAction.SET_JACKPOT_THANTAI:{
                    String data = params.get("data");
                    if(data.length() <5){
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.UNKOWN_ERROR, "Không đúng format jackpot than tai"));
                        return;
                    }
                    JackpotData jackpotData = gson.fromJson(data, JackpotData.class);
                    AdminCPUtils.setJackpotDataSlotThanTai(jackpotData);
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS, "ok"));
                    return;
                }
                case AdminAction.GET_FUND_THANTAI:{
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS,
                            gson.toJson(AdminCPUtils.getFundDataSlotThanTai())));
                    return;
                }
                case AdminAction.SET_FUND_THANTAI:{
                    String data = params.get("data");
                    if(data.length() <5){
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.UNKOWN_ERROR, "Không đúng format fund than tai"));
                        return;
                    }
                    FundData fundData = gson.fromJson(data,FundData.class);
                    AdminCPUtils.setFundDataSlotThanTai(fundData);
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS, "ok"));
                    return;
                }
                case AdminAction.GET_JACKPOT_THETHAO:{
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS,
                            gson.toJson(AdminCPUtils.getJackpotDataSlotTheThao())));
                    return;
                }
                case AdminAction.SET_JACKPOT_THETHAO:{
                    String data = params.get("data");
                    if(data.length() <5){
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.UNKOWN_ERROR, "Không đúng format jackpot than tai"));
                        return;
                    }
                    JackpotData jackpotData = gson.fromJson(data, JackpotData.class);
                    AdminCPUtils.setJackpotDataSlotTheThao(jackpotData);
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS, "ok"));
                    return;
                }
                case AdminAction.GET_FUND_THETHAO:{
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS,
                            gson.toJson(AdminCPUtils.getFundDataSlotTheThao())));
                    return;
                }
                case AdminAction.SET_FUND_THETHAO:{
                    String data = params.get("data");
                    if(data.length() <5){
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.UNKOWN_ERROR, "Không đúng format fund than tai"));
                        return;
                    }
                    FundData fundData = gson.fromJson(data,FundData.class);
                    AdminCPUtils.setFundDataSlotTheThao(fundData);
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS, "ok"));
                    return;
                }
                
                
                case AdminAction.GET_JACKPOT_CHIEMTINH:{
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS,
                            gson.toJson(AdminCPUtils.getJackpotDataSlotChiemTinh())));
                    return;
                }
                case AdminAction.SET_JACKPOT_CHIEMTINH:{
                    String data = params.get("data");
                    if(data.length() <5){
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.UNKOWN_ERROR, "Không đúng format jackpot CHIEMTINH"));
                        return;
                    }
                    JackpotData jackpotData = gson.fromJson(data, JackpotData.class);
                    AdminCPUtils.setJackpotDataSlotChiemTinh(jackpotData);
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS, "ok"));
                    return;
                }
                case AdminAction.GET_FUND_CHIEMTINH:{
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS,
                            gson.toJson(AdminCPUtils.getFundDataSlotChiemTinh())));
                    return;
                }
                case AdminAction.SET_FUND_CHIEMTINH:{
                    String data = params.get("data");
                    if(data.length() <5){
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.UNKOWN_ERROR, "Không đúng format fund bitcoin"));
                        return;
                    }
                    FundData fundData = gson.fromJson(data,FundData.class);
                    AdminCPUtils.setFundDataSlotChiemtinh(fundData);
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS, "ok"));
                    return;
                }

                case AdminAction.GET_JACKPOT_THANBAI:{
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS,
                            gson.toJson(AdminCPUtils.getJackpotDataSlotThanBai())));
                    return;
                }
                case AdminAction.SET_JACKPOT_THANBAI:{
                    String data = params.get("data");

                    if(data.length() <5){
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.UNKOWN_ERROR, "Không đúng format jackpot CHIEMTINH"));
                        return;
                    }

                    JackpotData jackpotData = gson.fromJson(data, JackpotData.class);
                    AdminCPUtils.setJackpotDataSlotThanBai(jackpotData);
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS, "ok"));
                    return;
                }
                case AdminAction.GET_FUND_THANBAI:{
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS,
                            gson.toJson(AdminCPUtils.getFundDataSlotThanBai())));
                    return;
                }
                case AdminAction.SET_FUND_THANBAI:{
                    String data = params.get("data");

                    if(data.length() <5){
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.UNKOWN_ERROR, "Không đúng format fund bitcoin"));
                        return;
                    }

                    FundData fundData = gson.fromJson(data,FundData.class);
                    AdminCPUtils.setFundDataSlotThanBai(fundData);
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS, "ok"));
                    return;
                }

                case AdminAction.GET_JACKPOT_BIKINI:{
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS,
                            gson.toJson(AdminCPUtils.getJackpotDataSlotBikini())));
                    return;
                }
                case AdminAction.SET_JACKPOT_BIKINI:{
                    String data = params.get("data");

                    if(data.length() <5){
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.UNKOWN_ERROR, "Không đúng format jackpot CHIEMTINH"));
                        return;
                    }

                    JackpotData jackpotData = gson.fromJson(data, JackpotData.class);
                    AdminCPUtils.setJackpotDataSlotBikini(jackpotData);
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS, "ok"));
                    return;
                }
                case AdminAction.GET_FUND_BIKINI:{
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS,
                            gson.toJson(AdminCPUtils.getFundDataSlotBikini())));
                    return;
                }
                case AdminAction.SET_FUND_BIKINI:{
                    String data = params.get("data");

                    if(data.length() <5){
                        JettyUtils.send(baseRequest, response, new JettyResponse(
                                JettyErrorCode.UNKOWN_ERROR, "Không đúng format fund bitcoin"));
                        return;
                    }

                    FundData fundData = gson.fromJson(data,FundData.class);
                    AdminCPUtils.setFundDataSlotBikini(fundData);
                    JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.SUCCESS, "ok"));
                    return;
                }
            }
        }catch (Exception e){

        }
        JettyUtils.send(baseRequest, response, new JettyResponse(JettyErrorCode.UNKOWN_ERROR, "Đã có lỗi xảy ra."));
    }
}
