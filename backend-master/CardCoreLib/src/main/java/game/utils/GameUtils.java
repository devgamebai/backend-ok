/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.config.ConfigHandle
 *  bitzero.server.core.BZEvent
 *  bitzero.server.core.IBZEvent
 *  bitzero.server.core.IBZEventType
 *  bitzero.server.entities.User
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.util.ExtensionUtility
 *  bitzero.util.common.business.CommonHandle
 *  bitzero.util.socialcontroller.bean.UserInfo
 *  com.google.gson.Gson
 *  com.vinplay.dal.service.BroadcastMessageService
 *  com.vinplay.dal.service.impl.BroadcastMessageServiceImpl
 *  com.vinplay.usercore.service.LogGameService
 *  com.vinplay.usercore.service.impl.LogGameServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.messages.LogGameMessage
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.response.UserResponse
 *  org.json.JSONException
 *  org.json.JSONObject
 */
package game.utils;

import bitzero.server.config.ConfigHandle;
import bitzero.server.core.BZEvent;
import bitzero.server.core.IBZEvent;
import bitzero.server.core.IBZEventType;
import bitzero.server.entities.User;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.util.ExtensionUtility;
import bitzero.util.common.business.CommonHandle;
import bitzero.util.socialcontroller.bean.UserInfo;
import com.google.gson.Gson;
import com.vinplay.dal.service.BroadcastMessageService;
import com.vinplay.dal.service.impl.BroadcastMessageServiceImpl;
import com.vinplay.usercore.service.LogGameService;
import com.vinplay.usercore.service.impl.LogGameServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.messages.LogGameMessage;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.UserResponse;
import game.entities.PlayerInfo;
import game.entities.UserScore;
import game.eventHandlers.GameEventParam;
import game.eventHandlers.GameEventType;
import game.modules.gameRoom.config.HuVangConfig;
import game.modules.gameRoom.entities.GameMoneyInfo;
import game.modules.gameRoom.entities.GameRoom;
import game.modules.gameRoom.entities.GameRoomSetting;
import game.modules.gameRoom.entities.ThongTinHuVang;
import game.modules.gameRoom.entities.ThongTinThangLon;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONException;
import org.json.JSONObject;

public class GameUtils {
    public static boolean isMainTain = false;
    public static boolean isHuVang = safeLong("isHuVang") == 1;
    public static boolean isBot = safeLong("isBot") == 1;
    public static boolean dev_mod = safeLong("dev_mod") == 1;
    public static boolean isCheat = safeLong("isCheat") == 1;
    public static boolean isLog = safeLong("isLog") == 1;
    public static final String gameName = ConfigHandle.instance().get("games");
    public static boolean enable_payment = safeLong("enable_payment") == 1;
    public static final String gameServerClassPath = ConfigHandle.instance().get("gameserver.main.path");

    /** Safe wrapper for ConfigHandle.getLong — returns 0 if key is missing or unparseable.
     *  ConfigHandle.getLong can throw if DebugConsole.log is not yet initialized during <clinit>. */
    private static long safeLong(String key) {
        try {
            Long val = ConfigHandle.instance().getLong(key);
            return val != null ? val : 0L;
        } catch (Throwable t) {
            System.err.println("[GameUtils] WARNING: Failed to read config key '" + key + "': " + t.getMessage());
            return 0L;
        }
    }
    public static final Random rd = new Random();
    public static ExecutorService commonThread = Executors.newFixedThreadPool(1);
    public static LogGameService dao = new LogGameServiceImpl();
    public static BroadcastMessageService broadcastMsgService = new BroadcastMessageServiceImpl();
    public static final Gson gson = new Gson();

    public static String toJsonString(Object obj) {
        try {
            return gson.toJson(obj);
        }
        catch (Exception e) {
            return "{}";
        }
    }

    public static JSONObject toJSONObject(Object obj) {
        String s = GameUtils.toJsonString(obj);
        try {
            return new JSONObject(s);
        }
        catch (JSONException e) {
            return null;
        }
    }

    public static boolean infoCheck(User user) {
        PlayerInfo pInfo = PlayerInfo.getInfo(user);
        if (pInfo == null) {
            return false;
        }
        return !PlayerInfo.getIsHold(pInfo.nickName);
    }

    public static void setUserScore(int userId, UserScore score, boolean updateDatabase) {
    }

    public static UserInfo getUserInfoDev(String username, String sessionKey) {
        UserInfo info = new UserInfo();
        int userId = 0;
        try {
            Integer v = new Integer(sessionKey);
            userId = v;
        }
        catch (Exception e) {
            Random rd = new Random();
            userId = Math.abs(rd.nextInt() % 100000);
        }
        info.setUsername("vp_" + userId);
        info.setUserId("" + userId);
        return info;
    }

    public static UserInfo getAdminInfo() {
        UserInfo info = new UserInfo();
        info.setUsername("vingod");
        info.setUserId("-1");
        return info;
    }

    public static UserInfo getUserInfo(String username, String sessionKey) {
        if (dev_mod) {
            UserInfo info = new UserInfo();
            int userId = 0;
            try {
                Integer v = new Integer(username);
                userId = v;
            }
            catch (Exception e) {
                Random rd = new Random();
                userId = Math.abs(rd.nextInt() % 100000);
            }
            info.setUsername("vp_" + userId);
            info.setUserId("" + userId);
            return info;
        }
        UserServiceImpl service = new UserServiceImpl();
        long now = System.currentTimeMillis();
        UserResponse res = service.checkSessionKey(username, sessionKey, Games.findGameByName((String)gameName));
        if (res.getErrorCode() == "0") {
            res.getUser().getId();
            UserInfo info = new UserInfo();
            info.setUserId(String.valueOf(res.getUser().getId()));
            info.setUsername(res.getUser().getNickname());
            int avatarId = Math.abs(rd.nextInt() % 12);
            info.setHeadurl(String.valueOf(avatarId));
            if (res.getUser().isBot()) {
                info.setStatus("1");
            }
            return info;
        }
        if (res.getErrorCode() == "1111") {
            UserInfo info = new UserInfo();
            info.setUsername("");
            return info;
        }
        return null;
    }

    public static boolean dispatchEventThangLon(User user, GameRoom room, int gameId, GameMoneyInfo moneyInfo, long moneyBet, boolean noHu, byte[] cards) {
        if (room.setting.moneyType == 0 || user == null || room == null) {
            return false;
        }
        if (noHu && !ThongTinHuVang.instance().dangChayHu() || !isHuVang) {
            return false;
        }
        ThongTinThangLon info = new ThongTinThangLon();
        info.gameName = gameName;
        info.cards = cards;
        info.noHu = noHu;
        info.roomId = room.getId();
        info.gameId = gameId;
        info.nickName = user.getName();
        info.moneySessionId = moneyInfo.sessionId;
        info.rate = HuVangConfig.instance().getRate(gameName, moneyBet);
        info.moneyBet = (int)room.setting.moneyBet;
        StringBuilder sb = new StringBuilder(gameName);
        if (room.setting.maxUserPerRoom == 2) {
            sb.append(" Solo");
        }
        info.desc = sb.toString();
        HashMap<GameEventParam, Object> evtParams = new HashMap<GameEventParam, Object>();
        evtParams.put(GameEventParam.USER, (Object)user);
        evtParams.put(GameEventParam.GAMEROOM, room);
        evtParams.put(GameEventParam.THONG_TIN_THANG_LON, info);
        ExtensionUtility.dispatchImmediateEvent((IBZEvent)new BZEvent((IBZEventType)GameEventType.THANG_LON, evtParams));
        broadcastMsgService.putMessage(Games.findGameByName((String)gameName).getId(), info.nickName, info.MoneyAdd);
        return true;
    }

    public static void dispatchAddEventScore(User user, UserScore score, GameRoom room) {
        if (user == null) {
            return;
        }
        score.moneyType = room.setting.moneyType;
        UserScore newScore = score.clone();
        HashMap<GameEventParam, Object> evtParams = new HashMap<GameEventParam, Object>();
        evtParams.put(GameEventParam.USER, (Object)user);
        evtParams.put(GameEventParam.USER_SCORE, newScore);
        ExtensionUtility.dispatchEvent((IBZEvent)new BZEvent((IBZEventType)GameEventType.EVENT_ADD_SCORE, evtParams));
    }

    public static void logStartGame(int gameId, String nickName, long logTime, int moneyType) {
        if (!isLog) {
            return;
        }
        LogGameMessage msg = new LogGameMessage();
        msg.gameName = gameName;
        msg.sessionId = String.valueOf(gameId);
        msg.timeLog = String.valueOf(logTime);
        msg.nickName = nickName;
        msg.moneyType = String.valueOf(moneyType);
        try {
            dao.saveLogGameByNickName(msg);
        }
        catch (Exception e) {
            CommonHandle.writeDebugLog((Throwable)e);
            CommonHandle.writeErrLogDebug((Object[])new Object[]{msg.gameName, msg.sessionId, msg.timeLog, msg.nickName, msg.moneyType});
        }
    }

    public static void logEndGame(int gameId, String gameLog, long logTime) {
        if (!isLog) {
            return;
        }
        LogGameMessage msg = new LogGameMessage();
        msg.gameName = gameName;
        msg.sessionId = String.valueOf(gameId);
        msg.logDetail = gameLog;
        msg.timeLog = String.valueOf(logTime);
        try {
            dao.saveLogGameDetail(msg);
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
            CommonHandle.writeErrLogDebug((Object[])new Object[]{msg.gameName, msg.sessionId, msg.logDetail, msg.timeLog});
        }
    }

    public static void send(BaseMsg msg, User user) {
        ExtensionUtility.instance().send(msg, user);
    }

    public static void sendAlert(String msg) {
    }

    public static void sendAlertAndCall(String msg) {
    }

    public static void sendSMSToUser(String username, String message) {
    }

    public static boolean disablePlayMiniGame(bitzero.server.entities.User user) {
        String daiLy = (String) user.getProperty("dai_ly");
        if (daiLy != null && !daiLy.isEmpty()) {
            int val = Integer.parseInt(daiLy);
            if (val > 0) return true;
        }
        return false;
    }
}

