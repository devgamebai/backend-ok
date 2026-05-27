/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.CommonHandle
 *  org.json.JSONException
 *  org.json.JSONObject
 */
package game.modules.gameRoom.entities;

import bitzero.util.common.business.CommonHandle;
import game.modules.gameRoom.cmd.rev.CreateGameRoomCmd;
import game.modules.gameRoom.cmd.rev.JoinGameRoomCmd;
import game.modules.gameRoom.cmd.rev.RevGetRoomList;
import game.utils.GameUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class GameRoomSetting {
    public static final int VIP_ROOM = 1;
    public static final int NORMAL_ROOM = 2;
    public String setting_name = null;
    public int maxUserPerRoom;
    public long moneyBet;
    public int moneyType;
    public long requiredMoney;
    public long outMoney;
    public int commisionRate;
    public int rule;
    public int limitPlayer;
    public boolean createRoom = false;
    public String password = "";
    public String roomName = "";
    public int numberOfInitialRoom = 0;

    public GameRoomSetting(JSONObject config) {
        try {
            this.limitPlayer = this.maxUserPerRoom = config.getInt("maxUserPerRoom");
            this.moneyType = config.getInt("moneyType");
            this.moneyBet = config.getInt("moneyBet");
            // this.requiredMoney = this.moneyBet * config.getLong("requiredMoney");
            this.requiredMoney = config.getLong("requiredMoney");
            this.outMoney = this.moneyBet * config.getLong("outMoney");
            this.commisionRate = config.getInt("commisionRate");
            this.rule = config.getInt("rule");
            StringBuilder sb = new StringBuilder();
            sb.append(this.maxUserPerRoom).append("_").append(this.moneyType).append("_").append(this.moneyBet).append("_").append(this.rule);
            this.setting_name = sb.toString();
            this.numberOfInitialRoom = config.getInt("numberOfInitialRoom");
            this.roomName = config.getString("roomName");
        }
        catch (JSONException e) {
            CommonHandle.writeErrLog((Throwable)e);
        }
    }

    public GameRoomSetting(GameRoomSetting setting, int type, int bet, int max, int r) {
        this.maxUserPerRoom = max;
        this.limitPlayer = max;
        this.moneyType = type;
        this.moneyBet = bet;
        this.requiredMoney = (long)bet * (setting.requiredMoney / setting.moneyBet);
        this.outMoney = (long)bet * (setting.outMoney / setting.moneyBet);
        this.commisionRate = setting.commisionRate;
        this.rule = r;
        StringBuilder sb = new StringBuilder();
        sb.append(this.maxUserPerRoom).append("_").append(this.moneyType).append("_").append(this.moneyBet).append("_").append(this.rule);
        this.setting_name = sb.toString();
    }

    public GameRoomSetting(GameRoomSetting setting, String settingName) {
        this.setting_name = settingName;
        this.maxUserPerRoom = setting.maxUserPerRoom;
        this.moneyBet = setting.moneyBet;
        this.moneyType = setting.moneyType;
        this.requiredMoney = setting.requiredMoney;
        this.outMoney = setting.outMoney;
        this.commisionRate = setting.commisionRate;
        this.limitPlayer = setting.limitPlayer;
    }

    public GameRoomSetting(GameRoomSetting setting) {
        this.clone(setting);
    }

    public void clone(GameRoomSetting setting) {
        this.setting_name = setting.setting_name;
        this.maxUserPerRoom = setting.maxUserPerRoom;
        this.moneyBet = setting.moneyBet;
        this.moneyType = setting.moneyType;
        this.requiredMoney = setting.requiredMoney;
        this.outMoney = setting.outMoney;
        this.commisionRate = setting.commisionRate;
        this.rule = setting.rule;
        this.limitPlayer = setting.limitPlayer;
        this.roomName = setting.roomName;
        this.password = setting.password;
        this.numberOfInitialRoom = setting.numberOfInitialRoom;
    }

    public GameRoomSetting(CreateGameRoomCmd cmd) {
        this.maxUserPerRoom = cmd.maxUserPerRoom;
        this.moneyType = cmd.moneyType;
        this.moneyBet = cmd.moneyBet;
        this.rule = cmd.rule;
        StringBuilder sb = new StringBuilder();
        sb.append(this.maxUserPerRoom).append("_").append(this.moneyType).append("_").append(this.moneyBet).append("_").append(this.rule);
        this.setting_name = sb.toString();
        this.limitPlayer = cmd.limitPlayer;
        this.password = cmd.password;
        this.roomName = cmd.roomName;
        this.createRoom = true;
    }

    public GameRoomSetting(JoinGameRoomCmd cmd) {
        this.maxUserPerRoom = cmd.maxUserPerRoom;
        this.moneyType = cmd.moneyType;
        this.moneyBet = cmd.moneyBet;
        this.rule = cmd.rule;
        StringBuilder sb = new StringBuilder();
        sb.append(this.maxUserPerRoom).append("_").append(this.moneyType).append("_").append(this.moneyBet).append("_").append(this.rule);
        this.setting_name = sb.toString();
    }

    public GameRoomSetting(RevGetRoomList cmd) {
        this.maxUserPerRoom = cmd.maxUserPerRoom;
        this.moneyType = cmd.moneyType;
        this.moneyBet = cmd.moneyBet;
        this.rule = cmd.rule;
        StringBuilder sb = new StringBuilder();
        sb.append(this.maxUserPerRoom).append("_").append(this.moneyType).append("_").append(this.moneyBet).append("_").append(this.rule);
        this.setting_name = sb.toString();
    }

    public GameRoomSetting(int moneyType, long moneyBet, int maxUserPerRoom, int rule) {
        this.maxUserPerRoom = maxUserPerRoom;
        this.limitPlayer = maxUserPerRoom;
        this.moneyType = moneyType;
        this.moneyBet = moneyBet;
        this.rule = rule;
        StringBuilder sb = new StringBuilder();
        sb.append(maxUserPerRoom).append("_").append(moneyType).append("_").append(moneyBet).append("_").append(rule);
        this.setting_name = sb.toString();
    }

    public String getSettingName() {
        return this.setting_name;
    }

    public String toString() {
        return GameUtils.toJsonString(this);
    }

    public JSONObject toJONObject() {
        return GameUtils.toJSONObject(this);
    }
}

