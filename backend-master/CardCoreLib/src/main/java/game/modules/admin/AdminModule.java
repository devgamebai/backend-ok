/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.engine.sessions.ISession
 *  bitzero.server.entities.User
 *  bitzero.server.entities.managers.IUserManager
 *  bitzero.server.extensions.BaseClientRequestHandler
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.server.extensions.data.DataCmd
 *  bitzero.util.ExtensionUtility
 *  bitzero.util.common.business.CommonHandle
 *  org.json.JSONArray
 *  org.json.JSONException
 *  org.json.JSONObject
 */
package game.modules.admin;

import bitzero.engine.sessions.ISession;
import bitzero.server.entities.User;
import bitzero.server.entities.managers.IUserManager;
import bitzero.server.extensions.BaseClientRequestHandler;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.extensions.data.DataCmd;
import bitzero.util.ExtensionUtility;
import bitzero.util.common.business.CommonHandle;
import game.entities.PlayerInfo;
import game.modules.admin.cmd.receive.CommandAdminCmd;
import game.modules.admin.cmd.send.ResponseAdminMsg;
import game.modules.bot.Bot;
import game.modules.bot.BotManager;
import game.modules.gameRoom.config.HuVangConfig;
import game.modules.gameRoom.entities.BlockingData;
import game.modules.gameRoom.entities.GameMoneyInfo;
import game.modules.gameRoom.entities.GameRoom;
import game.modules.gameRoom.entities.GameRoomGroup;
import game.modules.gameRoom.entities.GameRoomManager;
import game.modules.gameRoom.entities.GameRoomSetting;
import game.modules.gameRoom.entities.GameServer;
import game.modules.gameRoom.entities.ThongTinHuVang;
import game.modules.player.entities.NormalMoneyInfo;
import game.modules.player.entities.VipMoneyInfo;
import game.utils.GameUtils;
import java.io.IOException;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AdminModule
extends BaseClientRequestHandler {
    public void handleClientRequest(User user, DataCmd dataCmd) {
        if (!user.getName().equalsIgnoreCase("vingod")) {
            return;
        }
        switch (dataCmd.getId()) {
            case 4000: {
                this.onAdminCommand(user, dataCmd);
            }
        }
    }

    private void onAdminCommand(User user, DataCmd dataCmd) {
        JSONObject command = null;
        try {
            CommandAdminCmd cmd = new CommandAdminCmd(dataCmd);
            command = new JSONObject(cmd.command);
            int commandId = command.getInt("command");
            switch (commandId) {
                case 1: {
                    this.serverCommand(user, command);
                    break;
                }
                case 2: {
                    this.groupsCommand(user, command);
                    break;
                }
                case 3: {
                    this.subGroupCommand(user, command);
                    break;
                }
                case 4: {
                    this.roomCommand(user, command);
                    break;
                }
                case 5: {
                    this.gameCommand(user, command);
                    break;
                }
                case 6: {
                    this.userCommand(user, command);
                    break;
                }
                case 7: {
                    this.rankingTableCommand(user, command);
                    break;
                }
                case 8: {
                    this.kickAllRoom(user, command);
                }
            }
        }
        catch (Exception e1) {
            CommonHandle.writeErrLog((Throwable)e1);
            try {
                JSONObject json = new JSONObject();
                if (command != null) {
                    json.put("command", (Object)command);
                }
                json.put("exception", (Object)e1.toString());
                this.response(user, json, 1);
            }
            catch (Exception e2) {
                CommonHandle.writeErrLog((Throwable)e2);
            }
        }
    }

    private void kickAllRoom(User user, JSONObject command) {
    }

    private void serverCommand(User user, JSONObject json) throws Exception {
        int sub = json.getInt("sub");
        switch (sub) {
            case 1: {
                this.getServerInfo(user, json);
                break;
            }
            case 2: {
                this.maintainGame(user, json);
                break;
            }
            case 3: {
                this.thayDoiHuVang(user, json);
                break;
            }
            case 4: {
                this.getBotMoneyInfo(user, json);
                break;
            }
            case 5: {
                this.stopBotIngame(user, json);
                break;
            }
            case 7: {
                this.configBotIngame(user, json);
                break;
            }
            case 6: {
                this.loopRoom(user, json);
            }
        }
    }

    private void getBotMoneyInfo(User user, JSONObject json) throws Exception {
        JSONObject res = new JSONObject();
        res.put("BotMoneyInfo", (Object)BotManager.instance().getBotMoneyInfo());
        res.put("Balance Mode 1 UP 2 DOWN", BotManager.instance().balanceMode);
        this.response(user, res, 0);
    }

    private void loopRoom(User user, JSONObject json) throws JSONException {
        JSONObject res = new JSONObject();
        int roomId = json.getInt("roomId");
        GameRoom room = GameRoomManager.instance().getGameRoomById(roomId);
        room.getGameServer().init();
        res.put("roomId", roomId);
        this.response(user, res, 0);
    }

    private void stopBotIngame(User user, JSONObject json) throws JSONException {
        JSONObject res = new JSONObject();
        GameUtils.isBot = json.getBoolean("isBot");
        res.put("game", (Object)GameUtils.gameName);
        res.put("isBot", GameUtils.isBot);
        this.response(user, res, 0);
    }

    private void configBotIngame(User user, JSONObject json) throws JSONException {
        JSONObject res = new JSONObject();
        res.put("game", (Object)GameUtils.gameName);
        res.put("isBot", GameUtils.isBot);
        this.response(user, res, 0);
    }

    private void thayDoiHuVang(User user, JSONObject json) throws Exception {
        boolean isHuVang;
        JSONObject res = new JSONObject();
        GameUtils.isHuVang = isHuVang = json.getBoolean("isHuVang");
        res.put("game", (Object)GameUtils.gameName);
        res.put("isHuVang", GameUtils.isHuVang);
        if (GameUtils.isHuVang) {
            HuVangConfig.instance().initconfig();
            ThongTinHuVang.instance().firstTime = true;
        }
        this.response(user, res, 0);
    }

    private void maintainGame(User user, JSONObject json) throws Exception {
        boolean isMaintain;
        JSONObject res = new JSONObject();
        GameUtils.isMainTain = isMaintain = json.getBoolean("maintain");
        res.put("game", (Object)GameUtils.gameName);
        res.put("isMaintain", GameUtils.isMainTain);
        this.response(user, res, 0);
    }

    private void getServerInfo(User user, JSONObject json) throws Exception {
        JSONObject res = new JSONObject();
        int ccu = ExtensionUtility.globalUserManager.getUserCount();
        res.put("game", (Object)GameUtils.gameName);
        res.put("maintain", GameUtils.isMainTain);
        res.put("ccu", ccu);
        if (GameUtils.isBot) {
            res.put("bot", BotManager.instance().getBotCount());
        }
        res.put("room", GameRoomManager.instance().getRoomCount());
        this.response(user, res, 0);
    }

    private void groupsCommand(User user, JSONObject json) throws Exception {
        JSONObject res = new JSONObject();
        res.put("groups", (Object)GameRoomManager.instance().toJSONObject());
        this.response(user, res, 0);
    }

    private void subGroupCommand(User user, JSONObject json) throws Exception {
        int moneyType = json.getInt("moneyType");
        int moneyBet = json.getInt("moneyBet");
        int maxUserPerRoom = json.getInt("maxUserPerRoom");
        int rule = json.getInt("rule");
        GameRoomSetting setting = new GameRoomSetting(moneyType, moneyBet, maxUserPerRoom, rule);
        GameRoomGroup group = GameRoomManager.instance().getGroup(setting);
        JSONObject res = new JSONObject();
        res.put("group", (Object)group.toJSONObject());
        this.response(user, res, 0);
    }

    private void roomCommand(User user, JSONObject json) throws Exception {
        int roomId = json.getInt("roomId");
        JSONObject res = new JSONObject();
        GameRoom room = GameRoomManager.instance().getGameRoomById(roomId);
        if (room != null) {
            res.put("room", (Object)room.toJSONObject());
            this.response(user, res, 0);
        } else {
            res.put("msg", (Object)"Room not exists");
            res.put("roomId", roomId);
            this.response(user, res, 1);
        }
    }

    private void userCommand(User user, JSONObject json) throws Exception {
        int sub = json.getInt("sub");
        switch (sub) {
            case 6: {
                this.putUserToRoom(user, json);
                break;
            }
            case 1: {
                this.getUserInfo(user, json);
                break;
            }
            case 2: {
                this.choUserNoHu(user, json);
                break;
            }
            case 3: {
                this.kickUser(user, json);
                break;
            }
            case 4: {
                this.blockSpamInvite(user, json);
            }
        }
    }

    private void blockSpamInvite(User user, JSONObject json) throws JSONException {
        String nickName = json.getString("nickName");
        JSONObject res = new JSONObject();
        res.put(nickName, (Object)nickName);
        res.put("block", true);
        BlockingData.instance().addSpamInvite(nickName);
        this.response(user, res, 0);
    }

    private void kickUser(User user, JSONObject json) throws JSONException {
        String nickName = json.getString("nickName");
        User u = ExtensionUtility.globalUserManager.getUserByName(nickName);
        JSONObject res = new JSONObject();
        res.put(nickName, (Object)nickName);
        if (u != null) {
            res.put("disconnect", true);
            try {
                u.getSession().close();
            }
            catch (IOException e) {
                res.put("Error", (Object)"Cannot close session");
            }
        } else {
            res.put("disconnect", false);
        }
        this.response(user, res, 0);
    }

    private void choUserNoHu(User user, JSONObject json) throws JSONException {
        String nickName = json.getString("nickName");
        Bot bot = null;
        if (GameUtils.isBot) {
            bot = BotManager.instance().getBotByName(nickName);
        }
        JSONObject res = new JSONObject();
        User u = null;
        if (bot != null) {
            u = bot.user;
            res.put("isBot", true);
        }
        if (u == null) {
            u = ExtensionUtility.globalUserManager.getUserByName(nickName);
            res.put("isBot", false);
        }
        if (u != null) {
            res.put("nickName", (Object)u.getName());
            GameRoom room = (GameRoom)u.getProperty((Object)"GAME_ROOM");
            if (room != null) {
                room.getGameServer().choNoHu(nickName);
                res.put("roomId", room.getId());
            }
        } else {
            res.put("nickname", (Object)"invalid");
        }
        this.response(user, res, 0);
    }

    private void gameCommand(User user, JSONObject json) {
    }

    private void rankingTableCommand(User user, JSONObject json) {
    }

    private void response(User user, JSONObject json, int error) {
        try {
            if (error == 0 && json != null) {
                ResponseAdminMsg msg = new ResponseAdminMsg();
                json.put("error", 0);
                msg.info = json.toString();
                this.send((BaseMsg)msg, user);
            } else {
                ResponseAdminMsg msg = new ResponseAdminMsg();
                if (json == null) {
                    json = new JSONObject();
                    json.put("error", error);
                }
                this.send((BaseMsg)msg, user);
            }
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
        }
    }

    private void putUserToRoom(User user, JSONObject json) throws Exception {
        JSONObject res = new JSONObject();
        String nickName = json.getString("nickName");
        User u = null;
        Bot bot = BotManager.instance().getBotByName(nickName);
        if (bot != null) {
            u = bot.user;
        }
        if (u == null) {
            u = ExtensionUtility.globalUserManager.getUserByName(nickName);
        }
        if (u != null) {
            int roomId = json.getInt("roomId");
            GameRoom room = GameRoomManager.instance().getGameRoomById(roomId);
            if (room != null && this.checkJoinRoom(u, room)) {
                GameRoomManager.instance().joinRoom(u, room, false);
                res.put("nickName", (Object)nickName);
                res.put("room", (Object)room.toJSONObject());
                this.response(user, res, 0);
            } else {
                res.put("msg", (Object)"Join room fail");
                this.response(user, json, 1);
            }
        }
    }

    private void getUserInfo(User user, JSONObject json) throws Exception {
        PlayerInfo pInfo;
        VipMoneyInfo vmInfo;
        NormalMoneyInfo nmInfo;
        JSONArray arr = json.getJSONArray("listInfo");
        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < arr.length(); ++i) {
            list.add(arr.getInt(i));
        }
        String nickName = json.getString("nickName");
        JSONObject res = new JSONObject();
        ResponseAdminMsg msg = new ResponseAdminMsg();
        Bot bot = null;
        if (GameUtils.isBot) {
            bot = BotManager.instance().getBotByName(nickName);
        }
        User u = null;
        if (bot != null) {
            u = bot.user;
        }
        if (u == null) {
            u = ExtensionUtility.globalUserManager.getUserByName(nickName);
        }
        if (u != null) {
            res.put("online", true);
            GameRoom room = (GameRoom)u.getProperty((Object)"GAME_ROOM");
            if (room != null) {
                if (list.contains(1)) {
                    res.put("Room", (Object)room.toJSONObject());
                } else {
                    res.put("Room", (Object)"Not request room info");
                }
            } else {
                res.put("Room", (Object)"Not In Anyroom");
            }
        }
        if ((pInfo = PlayerInfo.copyFromDB(nickName)) != null && list.contains(2)) {
            res.put("Info", (Object)pInfo.toJSONObject());
        }
        if ((nmInfo = NormalMoneyInfo.copyFromDB(nickName)) != null && list.contains(3)) {
            res.put("Xu", (Object)nmInfo.toJSONObject());
        }
        if ((vmInfo = VipMoneyInfo.copyFromDB(nickName)) != null && list.contains(4)) {
            res.put("Vin", (Object)vmInfo.toJSONObject());
        }
        this.response(user, res, 0);
    }

    public boolean checkJoinRoom(User user, GameRoom room) {
        if (room.isFull()) {
            return false;
        }
        if (user.getProperty((Object)"GAME_ROOM") != null) {
            return false;
        }
        GameMoneyInfo moneyInfo = new GameMoneyInfo(user, room.setting);
        boolean result = moneyInfo.startGameUpdateMoney();
        if (result) {
            user.setProperty((Object)"GAME_MONEY_INFO", (Object)moneyInfo);
            return true;
        }
        return false;
    }
}

