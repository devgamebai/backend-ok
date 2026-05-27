/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.server.BitZeroServer
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

import bitzero.server.BitZeroServer;
import com.google.gson.Gson;
import com.vinplay.usercore.utils.GameThirdPartyInit;
import com.vinplay.utils.TelegramUtil;
import com.vinplay.vbee.common.statics.Consts;
import game.Jetty.FunctionUtils;
import game.Jetty.JettyResponse;
import game.Jetty.JettyUtils;
import game.Jetty.handle.AdminCPUtils;
import game.Jetty.model.FundData;
import game.Jetty.model.FundDataSlot;
import game.Jetty.model.JackpotData;
import game.modules.minigame.BauCuaModule;
import game.modules.minigame.TaiXiuModule;
import game.modules.minigame.SicboModule;
import game.modules.minigame.room.MGRoomBauCua;
import game.modules.minigame.room.MGRoomTaiXiu;
import game.modules.minigame.room.MGRoomSicbo;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class AdminCPHandler
extends AbstractHandler {
    private static final Gson gson = new Gson();

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
                return "GET_JACKPOT_MINIPOKER";
            }
            case 1: {
                return "SET_JACKPOT_MINIPOKER";
            }
            case 6: {
                return "GET_FUND_MINIPOKER";
            }
            case 7: {
                return "SET_FUND_MINIPOKER";
            }
            case 2: {
                return "GET_JACKPOT_SLOT3X3";
            }
            case 3: {
                return "SET_JACKPOT_SLOT3X3";
            }
            case 8: {
                return "GET_FUND_SLOT3X3";
            }
            case 9: {
                return "SET_FUND_SLOT3X3";
            }
            case 4: {
                return "GET_JACKPOT_CAOTHAP";
            }
            case 5: {
                return "SET_JACKPOT_CAOTHAP";
            }
            case 14: {
                return "GET_FUND_CAOTHAP";
            }
            case 15: {
                return "SET_FUND_CAOTHAP";
            }
            case 10: {
                return "GET_FUND_BAUCUA";
            }
            case 11: {
                return "SET_FUND_BAUCUA";
            }
            case 12: {
                return "GET_FUND_TAIXIU";
            }
            case 13: {
                return "SET_FUND_TAIXIU";
            }
            case 16: {
                return "GET_CCU";
            }
            case 18: {
                return "GET_LIST_CCU";
            }
            case 19: {
                return "GET_JACKPOT_GALAXY";
            }
            case 20: {
                return "SET_JACKPOT_GALAXY";
            }
            case 21: {
                return "GET_FUND_GALAXY";
            }
            case 22: {
                return "SET_FUND_GALAXY";
            }
            case 30: {
                return "GET_FUND_SICBO";
            }
            case 31: {
                return "SET_FUND_SICBO";
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
        if ("pro".equals(GameThirdPartyInit.enviroment) && action % 2 != 0) {
            TelegramUtil.warningCheat((String)("Setting config , action = " + this.actionStr(action) + " , param = " + params.toString()));
        }
        try {
            switch (action) {
                case 0: {
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, gson.toJson(AdminCPUtils.getJackpotDataSlotMinipoker())));
                    return;
                }
                case 1: {
                    String data = params.get("data");
                    if (data.length() < 5) {
                        JettyUtils.send(baseRequest, response, new JettyResponse(98, "Kh\u00f4ng \u0111\u00fang format jackpot minipoker"));
                        return;
                    }
                    JackpotData jackpotData = (JackpotData)gson.fromJson(data, JackpotData.class);
                    AdminCPUtils.setJackpotDataSlotMinipoker(jackpotData);
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, "ok"));
                    return;
                }
                case 6: {
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, gson.toJson(AdminCPUtils.getFundDataSlotMinipoker())));
                    return;
                }
                case 7: {
                    String data = params.get("data");
                    if (data.length() < 5) {
                        JettyUtils.send(baseRequest, response, new JettyResponse(98, "Kh\u00f4ng \u0111\u00fang format fund minipoker"));
                        return;
                    }
                    FundDataSlot fundDataSlot = (FundDataSlot)gson.fromJson(data, FundDataSlot.class);
                    AdminCPUtils.setFundDataSlotMinipoker(fundDataSlot);
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, "ok"));
                    return;
                }
                case 2: {
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, gson.toJson(AdminCPUtils.getJackpotDataSlot3x3())));
                    return;
                }
                case 3: {
                    String data = params.get("data");
                    if (data.length() < 5) {
                        JettyUtils.send(baseRequest, response, new JettyResponse(98, "Kh\u00f4ng \u0111\u00fang format jackpot slot 3x3"));
                        return;
                    }
                    JackpotData jackpotData = (JackpotData)gson.fromJson(data, JackpotData.class);
                    AdminCPUtils.setJackpotDataSlot3x3(jackpotData);
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, "ok"));
                    return;
                }
                case 8: {
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, gson.toJson(AdminCPUtils.getFundDataSlot3x3())));
                    return;
                }
                case 9: {
                    String data = params.get("data");
                    if (data.length() < 5) {
                        JettyUtils.send(baseRequest, response, new JettyResponse(98, "Kh\u00f4ng \u0111\u00fang format fund slot 3x3"));
                        return;
                    }
                    FundDataSlot fundDataSlot = (FundDataSlot)gson.fromJson(data, FundDataSlot.class);
                    AdminCPUtils.setFundDataSlot3x3(fundDataSlot);
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, "ok"));
                    return;
                }
                case 4: {
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, gson.toJson(AdminCPUtils.getJackpotDataCaoThap())));
                    return;
                }
                case 5: {
                    String data = params.get("data");
                    if (data.length() < 5) {
                        JettyUtils.send(baseRequest, response, new JettyResponse(98, "Kh\u00f4ng \u0111\u00fang format jackpot cao thap"));
                        return;
                    }
                    JackpotData jackpotData = (JackpotData)gson.fromJson(data, JackpotData.class);
                    AdminCPUtils.setJackpotDataCaoThap(jackpotData);
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, "ok"));
                    return;
                }
                case 14: {
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, gson.toJson(AdminCPUtils.getFundDataCaoThap())));
                    return;
                }
                case 15: {
                    String data = params.get("data");
                    if (data.length() < 5) {
                        JettyUtils.send(baseRequest, response, new JettyResponse(98, "Kh\u00f4ng \u0111\u00fang format fund cao thap"));
                        return;
                    }
                    FundData fundData = (FundData)gson.fromJson(data, FundData.class);
                    AdminCPUtils.setFundDataCaoTHap(fundData);
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, "ok"));
                    return;
                }
                case 10: {
                    MGRoomBauCua mgRoomBauCua = (MGRoomBauCua)BauCuaModule.getInstance().rooms.get("BauCua_vin_1000");
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, gson.toJson(new FundData(mgRoomBauCua.fund))));
                    return;
                }
                case 11: {
                    String data = params.get("data");
                    if (data.length() < 5) {
                        JettyUtils.send(baseRequest, response, new JettyResponse(98, "Kh\u00f4ng \u0111\u00fang format fund bau cua"));
                        return;
                    }
                    FundData fundData = (FundData)gson.fromJson(data, FundData.class);
                    MGRoomBauCua mgRoomBauCua = (MGRoomBauCua)BauCuaModule.getInstance().rooms.get("BauCua_vin_1000");
                    mgRoomBauCua.fund = fundData.listFund[0];
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, "ok"));
                    return;
                }
                case 12: {
                    MGRoomTaiXiu roomTXVin = TaiXiuModule.getInstance().getRoomTX((short)1);
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, gson.toJson(new FundData(roomTXVin.getFundTaiXiu()))));
                    return;
                }
                case 13: {
                    MGRoomTaiXiu roomTXVin = TaiXiuModule.getInstance().getRoomTX((short)1);
                    String data = params.get("data");
                    if (data.length() < 5) {
                        JettyUtils.send(baseRequest, response, new JettyResponse(98, "Kh\u00f4ng \u0111\u00fang format fund tai xiu"));
                        return;
                    }
                    FundData fundData = (FundData)gson.fromJson(data, FundData.class);
                    roomTXVin.setFundTaiXiu(fundData.listFund[0]);
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, "ok"));
                    return;
                }
                case 30: {
                    MGRoomSicbo roomTXVin = SicboModule.getInstance().getRoomTX((short)1);
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, gson.toJson(new FundData(roomTXVin.getFundTaiXiu()))));
                    return;
                }
                case 31: {
                    MGRoomSicbo roomTXVin = SicboModule.getInstance().getRoomTX((short)1);
                    String data = params.get("data");
                    if (data.length() < 5) {
                        JettyUtils.send(baseRequest, response, new JettyResponse(98, "Kh\u00f4ng \u0111\u00fang format fund tai xiu"));
                        return;
                    }
                    FundData fundData = (FundData)gson.fromJson(data, FundData.class);
                    roomTXVin.setFundTaiXiu(fundData.listFund[0]);
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, "ok"));
                    return;
                }
                case 16: {
                    int ccu = BitZeroServer.getInstance().getUserManager().getUserCount();
                    response.getWriter().println("user online: " + ccu);
                    return;
                }
                case 18: {
                    List User2 = BitZeroServer.getInstance().getUserManager().getAllUsers();
                    Gson gson = new Gson();
                    response.getWriter().println(gson.toJson(User2));
                    return;
                }
                case 19: {
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, gson.toJson(AdminCPUtils.getJackpotDataGalaxy())));
                    return;
                }
                case 20: {
                    String data = params.get("data");
                    if (data.length() < 5) {
                        JettyUtils.send(baseRequest, response, new JettyResponse(98, "Kh\u00f4ng \u0111\u00fang format jackpot slot 3x3"));
                        return;
                    }
                    JackpotData jackpotData = (JackpotData)gson.fromJson(data, JackpotData.class);
                    AdminCPUtils.setJackpotDataGalaxy(jackpotData);
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, "ok"));
                    return;
                }
                case 21: {
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, gson.toJson(AdminCPUtils.getFundDataGalaxy())));
                    return;
                }
                case 22: {
                    String data = params.get("data");
                    if (data.length() < 5) {
                        JettyUtils.send(baseRequest, response, new JettyResponse(98, "Kh\u00f4ng \u0111\u00fang format fund slot 3x3"));
                        return;
                    }
                    FundDataSlot fundDataSlot = (FundDataSlot)gson.fromJson(data, FundDataSlot.class);
                    AdminCPUtils.setFundDataGalaxy(fundDataSlot);
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, "ok"));
                    return;
                }
                case 23: {
                    MGRoomTaiXiu roomTXVin = TaiXiuModule.getInstance().getRoomTX((short)1);
                    long[] funJp = new long[]{Long.getLong(roomTXVin.getJpValue()), Long.getLong(roomTXVin.getJpValue())};
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, gson.toJson(new FundData(funJp))));
                    return;
                }
                case 24: {
                    MGRoomTaiXiu roomTXVin = TaiXiuModule.getInstance().getRoomTX((short)1);
                    String data = params.get("data");
                    if (data.length() < 5) {
                        JettyUtils.send(baseRequest, response, new JettyResponse(98, "Kh\u00f4ng \u0111\u00fang format fund tai xiu"));
                        return;
                    }
                    FundData fundData = (FundData)gson.fromJson(data, FundData.class);
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, "ok"));
                    return;
                }
                case 26: {
                    MGRoomTaiXiu roomTXVin = TaiXiuModule.getInstance().getRoomTX((short)1);
                    String data = params.get("data");
                    if (data.length() < 5) {
                        JettyUtils.send(baseRequest, response, new JettyResponse(98, "Kh\u00f4ng \u0111\u00fang format fund tai xiu"));
                        return;
                    }
                    FundData fundData = (FundData)gson.fromJson(data, FundData.class);
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, "ok"));
                    return;
                }
                case 28: {
                    MGRoomTaiXiu roomTXVin = TaiXiuModule.getInstance().getRoomTX((short)1);
                    String data = params.get("data");
                    if (data.length() < 5) {
                        JettyUtils.send(baseRequest, response, new JettyResponse(98, "Kh\u00f4ng \u0111\u00fang format fund tai xiu"));
                        return;
                    }
                    FundData fundData = (FundData)gson.fromJson(data, FundData.class);
                    if (fundData.listFund[0] == 0L) {
                        roomTXVin.setFlagJpTai(true);
                        roomTXVin.setFlagJpXiu(false);
                    } else {
                        roomTXVin.setFlagJpTai(false);
                        roomTXVin.setFlagJpXiu(true);
                    }
                    JettyUtils.send(baseRequest, response, new JettyResponse(0, "ok"));
                    return;
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        JettyUtils.send(baseRequest, response, new JettyResponse(98, "Da co loi xay ra."));
    }
}

