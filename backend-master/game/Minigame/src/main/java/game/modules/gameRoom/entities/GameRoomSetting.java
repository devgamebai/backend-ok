/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.CommonHandle
 *  org.json.JSONException
 *  org.json.JSONObject
 */
package game.modules.gameRoom.entities;

import bitzero.util.common.business.CommonHandle;
import game.modules.gameRoom.cmd.rev.JoinGameRoomCmd;
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

    public GameRoomSetting(JSONObject config) {
        try {
            this.maxUserPerRoom = config.getInt("maxUserPerRoom");
            this.moneyType = config.getInt("moneyType");
            this.moneyBet = config.getInt("moneyBet");
            this.requiredMoney = config.getLong("requiredMoney");
            this.outMoney = config.getLong("outMoney");
            this.commisionRate = config.getInt("commisionRate");
            this.rule = config.getInt("rule");
            StringBuilder sb = new StringBuilder();
            sb.append(this.maxUserPerRoom).append("_").append(this.moneyType).append("_").append(this.moneyBet).append("_").append(this.rule);
            this.setting_name = sb.toString();
        }
        catch (JSONException e) {
            CommonHandle.writeErrLog((Throwable)e);
        }
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

    public String getSettingName() {
        return this.setting_name;
    }
}

