/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  com.vinplay.usercore.service.MoneyInGameService
 *  com.vinplay.usercore.service.UserService
 *  com.vinplay.usercore.service.impl.MoneyInGameServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.enums.FreezeInGame
 *  com.vinplay.vbee.common.response.FreezeMoneyResponse
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  org.json.JSONException
 *  org.json.JSONObject
 *  scala.collection.mutable.StringBuilder
 */
package game.modules.gameRoom.entities;

import bitzero.server.entities.User;
import com.vinplay.usercore.service.MoneyInGameService;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.MoneyInGameServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.enums.FreezeInGame;
import com.vinplay.vbee.common.response.FreezeMoneyResponse;
import com.vinplay.vbee.common.response.MoneyResponse;
import game.entities.UserScore;
import game.modules.gameRoom.entities.GameRoomSetting;
import game.modules.gameRoom.entities.MoneyException;
import game.utils.DataUtils;
import game.utils.GameUtils;
import game.utils.LoggerUtils;
import java.util.Random;
import org.json.JSONException;
import org.json.JSONObject;
import scala.collection.mutable.StringBuilder;

public class GameMoneyInfo {
    public static final long MIN_BUY_IN_RATE_POKER = 40;
    public static final long MIN_BUY_IN_RATE_LIENG = 5;
    public static final String GAME_MONEY_INFO = "GAME_MONEY_INFO";
    public static MoneyInGameService moneyService = new MoneyInGameServiceImpl();
    public static Random rd = new Random();
    public static UserService userService = new UserServiceImpl();
    public String sessionId = "";
    public int countToRemove = 0;
    public int roomId;
    public String nickName;
    public String moneyBet;
    public int moneyType;
    public String moneyTypeName;
    public long currentMoney = 0;
    public long requireMoney;
    public long outMoney;
    public long freezeMoney;
    private static final boolean dependOnGame = false;

    public boolean moneyCheckInGame() {
        return this.getMoneyUseInGame() >= this.outMoney;
    }

    public long getMoneyUseInGame() {
        if (GameUtils.gameName.equals("Sam") || GameUtils.gameName.equals("Tlmn")) {
            return this.freezeMoney;
        }
        return userService.getMoneyUserCache(this.nickName, this.moneyTypeName) + this.freezeMoney;
    }

    public MoneyResponse updateMoney(long money, int roomId, int gameId, long fee, boolean bExactly) {
        MoneyResponse res = null;
        res = money > 0 ? moneyService.addingMoneyInGame(this.sessionId, this.nickName, GameUtils.gameName, String.valueOf(roomId), money, this.moneyTypeName, this.requireMoney, String.valueOf(gameId), fee) : (bExactly ? moneyService.subtractMoneyInGameExactly(this.sessionId, this.nickName, GameUtils.gameName, String.valueOf(roomId), Math.abs(money), this.moneyTypeName, String.valueOf(gameId)) : moneyService.subtractMoneyInGame(this.sessionId, this.nickName, GameUtils.gameName, String.valueOf(roomId), Math.abs(money), this.moneyTypeName, String.valueOf(gameId)));
        if (res.isSuccess()) {
            this.currentMoney = res.getCurrentMoney();
            this.freezeMoney = res.getFreezeMoney();
        }
        return res;
    }

    public MoneyResponse updateMoneyByFreeze(long money, int roomId, int gameId, long fee) {
        MoneyResponse res = moneyService.updateMoneyInGameByFreeze(this.sessionId, this.nickName, GameUtils.gameName, String.valueOf(roomId), money, this.moneyTypeName, String.valueOf(gameId), fee);
        if (res.isSuccess()) {
            this.currentMoney = res.getCurrentMoney();
            this.freezeMoney = res.getFreezeMoney();
        }
        return res;
    }

    public MoneyResponse addFreezeMoney(long money, int roomId, int gameId, FreezeInGame type) {
        MoneyResponse res = moneyService.addFreezeMoneyInGame(this.sessionId, this.nickName, GameUtils.gameName, String.valueOf(roomId), String.valueOf(gameId), money, this.moneyTypeName, type);
        if (res.isSuccess()) {
            this.currentMoney = res.getCurrentMoney();
            this.freezeMoney = res.getFreezeMoney();
        }
        return res;
    }

    public GameMoneyInfo(User user, GameRoomSetting setting) {
        this.nickName = user.getName();
        this.moneyType = setting.moneyType;
        this.moneyBet = String.valueOf(setting.moneyBet);
        this.moneyTypeName = this.moneyType == 1 ? "vin" : "xu";
        this.requireMoney = setting.requiredMoney;
        this.outMoney = setting.outMoney;
    }

    public long getCurrentMoneyFromCache() {
        if (!GameUtils.enable_payment) {
            return this.currentMoney;
        }
        this.currentMoney = userService.getCurrentMoneyUserCache(this.nickName, this.moneyTypeName);
        return this.currentMoney;
    }

    public boolean startGameUpdateMoney() {
        return this.freezeMoneyBegining();
    }

    public boolean moneyCheck() {
        return this.moneyCheckInGame() && this.freezeMoney > 0;
    }

    public boolean freezeMoneyBegining() {
        if (!GameUtils.enable_payment) {
            Random rd = new Random();
            this.sessionId = GameUtils.gameName + Math.abs(rd.nextInt(1000000000));
            this.freezeMoney = this.requireMoney;
            this.currentMoney = this.requireMoney * 10;
            return true;
        }
        FreezeMoneyResponse res = moneyService.freezeMoneyInGame(this.nickName, GameUtils.gameName, this.moneyBet, this.requireMoney, this.moneyTypeName);
        LoggerUtils.info("game_money", "freezeMoneyBegining(void): isSucess?", res.isSuccess(), res.getErrorCode(), "sessionId:", res.getSessionId(), "currentMoney:", res.getCurrentMoney());
        if (res.isSuccess()) {
            this.sessionId = res.getSessionId();
            this.currentMoney = res.getCurrentMoney();
            this.freezeMoney = this.requireMoney;
            return true;
        }
        return false;
    }

    public boolean freezeMoneyBegining(long money) {
        if (!GameUtils.enable_payment) {
            Random rd = new Random();
            this.sessionId = GameUtils.gameName + Math.abs(rd.nextInt(1000000000));
            this.requireMoney = money;
            this.freezeMoney = money;
            this.currentMoney = money * 10;
            return true;
        }
        long now = System.currentTimeMillis();
        this.requireMoney = money;
        FreezeMoneyResponse res = moneyService.freezeMoneyInGame(this.nickName, GameUtils.gameName, this.moneyBet, this.requireMoney, this.moneyTypeName);
        LoggerUtils.info("game_money", "freezeMoneyBegining(long): isSucess?", res.getErrorCode(), res.isSuccess(), "sessionId:", res.getSessionId(), "currentMoney:", res.getCurrentMoney());
        if (res.isSuccess()) {
            this.sessionId = res.getSessionId();
            this.currentMoney = res.getCurrentMoney();
            this.freezeMoney = this.requireMoney;
            return true;
        }
        return false;
    }

    public boolean addFreezeMoney(long money, int roomId, int gameId) {
        if (!GameUtils.enable_payment) {
            this.freezeMoney += money;
        }
        MoneyResponse res = moneyService.addFreezeMoneyInGame(this.sessionId, this.nickName, GameUtils.gameName, String.valueOf(roomId), String.valueOf(gameId), money, this.moneyTypeName, FreezeInGame.MORE);
        LoggerUtils.info("game_money", "addFreezeMoney(long): isSucess?", res.getErrorCode(), res.isSuccess(), "sessionId:", this.sessionId, "currentMoney:", res.getCurrentMoney(), "freezeMoney:", res.getFreezeMoney());
        if (res.isSuccess()) {
            this.currentMoney = res.getCurrentMoney();
            this.freezeMoney = res.getFreezeMoney();
            return true;
        }
        return false;
    }

    public boolean restoreMoney(int roomId) {
        if (!GameUtils.enable_payment) {
            return true;
        }
        long now = System.currentTimeMillis();
        if (this.freezeMoney > 0) {
            FreezeMoneyResponse res = moneyService.restoreMoneyInGame(this.sessionId, this.nickName, GameUtils.gameName, String.valueOf(roomId), this.moneyTypeName);
            LoggerUtils.info("game_money", "restoreMoney: isSucess?", res.isSuccess(), "sessionId:", res.getSessionId(), roomId);
            if (!res.isSuccess()) {
                return false;
            }
            return true;
        }
        return true;
    }

    public long chargeMoneyInGame(UserScore score, int roomId, int gameId) throws MoneyException {
        if (score.money < 0) {
            return - this.subMoneyInGame(score, roomId, gameId);
        }
        return this.addMoneyInGame(score, roomId, gameId);
    }

    public long chargeMoneyInTour(UserScore score, int roomId, int gameId) {
        if (score.money < 0) {
            LoggerUtils.debug("tour", "chargeMoneyInTour - 1", this.nickName, "room ", roomId, "game", gameId, "currentMoney", this.currentMoney, " freeze", this.freezeMoney);
            long result = - this.subMoneyInTour(score, roomId, gameId);
            LoggerUtils.debug("tour", this.nickName, "chargeMoneyInTour - 2", "room ", roomId, "game", gameId, "currentMoney", this.currentMoney, " freeze", this.freezeMoney);
            return result;
        }
        LoggerUtils.debug("tour", this.nickName, "chargeMoneyInTour + 1", "room ", roomId, "game", gameId, "currentMoney", this.currentMoney, " freeze", this.freezeMoney);
        long result = this.addMoneyInTour(score, roomId, gameId);
        LoggerUtils.debug("tour", this.nickName, "chargeMoneyInTour + 2", "room ", roomId, "game", gameId, "currentMoney", this.currentMoney, " freeze", this.freezeMoney);
        return result;
    }

    private long subMoneyInGame(UserScore score, int roomId, int gameId) throws MoneyException {
        if (!GameUtils.enable_payment) {
            long newMoney = this.currentMoney + score.money;
            if (newMoney < 0) {
                score.money = - this.currentMoney;
                this.currentMoney = 0;
                this.freezeMoney = 0;
            } else if (newMoney < this.requireMoney) {
                this.freezeMoney = newMoney;
                this.currentMoney = newMoney;
            } else {
                this.currentMoney = newMoney;
            }
            return - score.money;
        }
        long now = System.currentTimeMillis();
        MoneyResponse res = moneyService.subtractMoneyInGame(this.sessionId, this.nickName, GameUtils.gameName, String.valueOf(roomId), - score.money, this.moneyTypeName, String.valueOf(gameId));
        LoggerUtils.info("game_money", "subMoneyInGame: isSucess?", res.isSuccess(), res.getErrorCode(), "sessionId:", this.sessionId, "gameId:", gameId);
        if (!res.isSuccess()) {
            throw new MoneyException();
        }
        this.currentMoney = res.getCurrentMoney();
        this.freezeMoney = res.getFreezeMoney();
        return res.getSubtractMoney();
    }

    private long subMoneyInTour(UserScore score, int roomId, int gameId) {
        long newMoney = this.currentMoney + score.money;
        if (newMoney < 0) {
            score.money = - this.currentMoney;
            this.currentMoney = 0;
            this.freezeMoney = 0;
        } else if (newMoney < this.requireMoney) {
            this.freezeMoney = newMoney;
            this.currentMoney = newMoney;
        } else {
            this.currentMoney = newMoney;
        }
        return - score.money;
    }

    private long addMoneyInGame(UserScore score, int roomId, int gameId) throws MoneyException {
        if (!GameUtils.enable_payment) {
            this.freezeMoney += score.money;
            if (this.freezeMoney > this.requireMoney) {
                this.freezeMoney = this.requireMoney;
            }
            this.currentMoney += score.money;
            return score.money;
        }
        MoneyResponse res = moneyService.addingMoneyInGame(this.sessionId, this.nickName, GameUtils.gameName, String.valueOf(roomId), score.money, this.moneyTypeName, this.requireMoney, String.valueOf(gameId), score.wastedMoney);
        LoggerUtils.info("game_money", "addMoneyInGame: isSucess?", res.isSuccess(), res.getErrorCode(), "sessionId:", this.sessionId, "gameId:", gameId);
        if (!res.isSuccess()) {
            throw new MoneyException();
        }
        this.currentMoney = res.getCurrentMoney();
        this.freezeMoney = res.getFreezeMoney();
        return score.money;
    }

    private long addMoneyInTour(UserScore score, int roomId, int gameId) {
        this.freezeMoney += score.money;
        if (this.freezeMoney > this.requireMoney) {
            this.freezeMoney = this.requireMoney;
        }
        this.currentMoney += score.money;
        return score.money;
    }

    public static GameMoneyInfo copyFromDB(String sessionId) {
        StringBuilder key = new StringBuilder(GameMoneyInfo.class.getSimpleName());
        key.append(sessionId);
        GameMoneyInfo info = (GameMoneyInfo)DataUtils.copyDataFromDB(key.toString(), GameMoneyInfo.class);
        return info;
    }

    public String toString() {
        return GameUtils.toJsonString(this);
    }

    public JSONObject toJSONObject() {
        JSONObject json = null;
        try {
            json = new JSONObject(this.toString());
        }
        catch (JSONException var2_2) {
            // empty catch block
        }
        return json;
    }
}

