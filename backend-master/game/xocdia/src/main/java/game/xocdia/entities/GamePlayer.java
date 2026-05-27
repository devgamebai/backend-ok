/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  com.vinplay.vbee.common.enums.FreezeInGame
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.utils.NumberUtils
 *  game.entities.PlayerInfo
 *  game.modules.gameRoom.entities.GameMoneyInfo
 *  game.utils.GameUtils
 *  game.xocdia.conf.XocDiaConfig
 *  org.json.JSONObject
 */
package game.xocdia.entities;

import bitzero.server.entities.User;
import com.vinplay.vbee.common.enums.FreezeInGame;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.utils.NumberUtils;
import game.entities.PlayerInfo;
import game.modules.gameRoom.entities.GameMoneyInfo;
import game.utils.GameUtils;
import game.xocdia.conf.XocDiaConfig;
import org.json.JSONObject;

public class GamePlayer {
    public static final int psVIEW = 1;
    public static final int psSIT = 2;
    public static final int psPLAY = 3;
    public User user;
    public PlayerInfo pInfo;
    public GameMoneyInfo gameMoneyInfo;
    public boolean isBanker;
    public int cntNoPlay;
    public int numPLay;
    public boolean isDisplay;
    public boolean isBoss;
    public boolean isBot;
    public boolean revMsg;
    public int playerStatus;
    public boolean reqLeaveRoom;
    public boolean reqKickRoom;
    public boolean reqDestroyRoom;
    public boolean isSubBanker;
    public boolean reqDestroyBanker;
    public long lastChatTime;

    public GamePlayer(User user, PlayerInfo pInfo, GameMoneyInfo gameMoneyInfo) {
        this.user = user;
        this.isBot = user.isBot();
        this.pInfo = pInfo;
        this.gameMoneyInfo = gameMoneyInfo;
        this.isBanker = false;
        this.cntNoPlay = 0;
        this.numPLay = 0;
        this.playerStatus = 1;
        this.reqLeaveRoom = false;
        this.reqKickRoom = false;
        this.isSubBanker = false;
        this.reqDestroyBanker = false;
        this.lastChatTime = 0L;
        this.isDisplay = false;
        this.isBoss = false;
        this.reqDestroyRoom = false;
        this.revMsg = true;
    }

    public void newGame(int roomId) {
        if (this.isSubBanker) {
            this.gameMoneyInfo.addFreezeMoney(this.gameMoneyInfo.requireMoney, roomId, 0, FreezeInGame.SET);
        }
        this.reqLeaveRoom = false;
        this.isSubBanker = false;
        this.reqDestroyBanker = false;
        this.cntNoPlay = this.playerStatus == 2 ? ++this.cntNoPlay : 0;
        this.playerStatus = 2;
        ++this.numPLay;
    }

    public boolean isPlaying() {
        return this.playerStatus == 3;
    }

    public void setPlaying(int roomId) {
        if (this.playerStatus != 3) {
            this.playerStatus = 3;
            this.pInfo.setIsHold(true);
            PlayerInfo.setRoomId((String)this.user.getName(), (int)roomId);
        }
    }

    public boolean isSitting() {
        return this.playerStatus != 1;
    }

    public boolean isView() {
        return this.playerStatus == 1;
    }

    public boolean checkMoneyCanPlay() {
        return this.gameMoneyInfo.moneyCheckInGame();
    }

    public boolean isConnected() {
        return this.user != null && this.user.isConnected();
    }

    public long getMoneyUseInGame() {
        if (this.isBanker) {
            return this.gameMoneyInfo.freezeMoney;
        }
        return this.gameMoneyInfo.getMoneyUseInGame();
    }

    public long getCurrentMoney() {
        return this.gameMoneyInfo.getCurrentMoneyFromCache();
    }

    public byte isLeaveRoom(boolean reqDestroyGame) {
        int maxNumPlay;
        if (this.isBoss) {
            return 0;
        }
        if (reqDestroyGame) {
            return 6;
        }
        if (GameUtils.isMainTain) {
            return 2;
        }
        if (!this.isConnected()) {
            return 4;
        }
        if (this.reqLeaveRoom) {
            return 3;
        }
        if (this.reqKickRoom) {
            return 7;
        }
        if (!this.isBanker && XocDiaConfig.noPlayNumber > 0 && this.cntNoPlay > XocDiaConfig.noPlayNumber) {
            return 5;
        }
        if (!this.checkMoneyCanPlay()) {
            return 1;
        }
        if (this.isBot && this.numPLay > (maxNumPlay = NumberUtils.ranDomIntMinMax((int)XocDiaConfig.normalMaxNumPlayMin, (int)XocDiaConfig.normalMaxNumPlayMin))) {
            return 3;
        }
        return 0;
    }

    public String toString() {
        try {
            JSONObject json = this.toJSONObject();
            if (json != null) {
                return json.toString();
            }
            return "{}";
        }
        catch (Exception e) {
            return "{}";
        }
    }

    public JSONObject toJSONObject() {
        try {
            JSONObject json = new JSONObject();
            json.put("nickname", (Object)this.user.getName());
            json.put("isBanker", this.isBanker);
            json.put("cntNoPlay", this.cntNoPlay);
            json.put("playerStatus", this.playerStatus);
            json.put("reqQuitRoom", this.reqLeaveRoom);
            json.put("isSubBanker", this.isSubBanker);
            json.put("reqDestroyBanker", this.reqDestroyBanker);
            json.put("lastChatTime", this.lastChatTime);
            json.put("isDisplay", this.isDisplay);
            if (this.gameMoneyInfo != null) {
                json.put("gameMoneyInfo", (Object)this.gameMoneyInfo.toJSONObject());
            }
            return json;
        }
        catch (Exception e) {
            return null;
        }
    }
}

