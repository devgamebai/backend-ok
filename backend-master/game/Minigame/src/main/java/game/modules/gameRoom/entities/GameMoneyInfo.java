/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  bitzero.util.common.business.Debug
 *  com.vinplay.usercore.service.MoneyInGameService
 *  com.vinplay.usercore.service.impl.MoneyInGameServiceImpl
 *  com.vinplay.vbee.common.response.FreezeMoneyResponse
 *  com.vinplay.vbee.common.response.MoneyResponse
 */
package game.modules.gameRoom.entities;

import bitzero.server.entities.User;
import bitzero.util.common.business.Debug;
import com.vinplay.usercore.service.MoneyInGameService;
import com.vinplay.usercore.service.impl.MoneyInGameServiceImpl;
import com.vinplay.vbee.common.response.FreezeMoneyResponse;
import com.vinplay.vbee.common.response.MoneyResponse;
import game.entities.UserScore;
import game.modules.gameRoom.entities.GameRoomSetting;
import game.utils.GameUtils;

public class GameMoneyInfo {
    public static final String GAME_MONEY_INFO = "GAME_MONEY_INFO";
    public String sessionId;
    public String nickName;
    public String moneyBet;
    public int moneyType;
    public String moneyTypeName;
    public long currentMoney;
    public long requireMoney;
    public long outMoney;
    public long freezeMoney;
    public MoneyInGameService moneyService = new MoneyInGameServiceImpl();
    public int sessionid;
    public boolean moneyCheck;

    public GameMoneyInfo(User user, GameRoomSetting setting) {
        this.nickName = user.getName();
        this.moneyType = setting.moneyType;
        this.moneyBet = String.valueOf(setting.moneyBet);
        this.moneyTypeName = this.moneyType == 1 ? "vin" : "xu";
        this.requireMoney = setting.requiredMoney;
        this.outMoney = setting.outMoney;
    }

    public boolean startGameUpdateMoney() {
        return this.freezeMoneyBegining();
    }

    public boolean moneyCheck() {
        return this.freezeMoney >= this.outMoney;
    }

    public boolean freezeMoneyBegining() {
        if (GameUtils.isCheat) {
            this.freezeMoney = this.requireMoney;
            this.currentMoney = this.requireMoney;
            return true;
        }
        FreezeMoneyResponse res = this.moneyService.freezeMoneyInGame(this.nickName, GameUtils.gameName, this.moneyBet, this.requireMoney, this.moneyTypeName);
        this.sessionId = res.getSessionId();
        Debug.trace((Object[])new Object[]{"Freeze:", this.nickName, this.requireMoney, this.moneyTypeName, this.sessionId});
        if (res.isSuccess()) {
            this.currentMoney = res.getCurrentMoney();
            this.freezeMoney = this.requireMoney;
            return true;
        }
        Debug.trace((Object[])new Object[]{"Error", res.getErrorCode()});
        return false;
    }

    public void restoreMoney(int roomId) {
        if (GameUtils.isCheat) {
            return;
        }
        FreezeMoneyResponse res = this.moneyService.restoreMoneyInGame(this.sessionId, this.nickName, GameUtils.gameName, String.valueOf(roomId), this.moneyTypeName);
        if (!res.isSuccess()) {
            Debug.trace((Object[])new Object[]{"Error invalid sessionId: ", this.sessionId});
        }
    }

    public boolean outGameUpdate(int roomId) {
        this.restoreMoney(roomId);
        return true;
    }

    public long chargeMoneyInGame(UserScore score, int roomId, int gameId) {
        Debug.trace((Object[])new Object[]{"chargeMoneyInGame", this.nickName, score.money, roomId, gameId});
        if (score.money < 0L) {
            return -this.subMoneyInGame(score, roomId, gameId);
        }
        return this.addMoneyInGame(score, roomId, gameId);
    }

    public long subMoneyInGame(UserScore score, int roomId, int gameId) {
        if (GameUtils.isCheat) {
            this.currentMoney += score.money;
            return score.money;
        }
        MoneyResponse res = this.moneyService.subtractMoneyInGame(this.sessionId, this.nickName, GameUtils.gameName, String.valueOf(roomId), -score.money, this.moneyTypeName, String.valueOf(gameId));
        this.currentMoney = res.getCurrentMoney();
        this.freezeMoney = res.getFreezeMoney();
        Debug.trace((Object[])new Object[]{"subMoneyInGame", this.sessionId, res.isSuccess(), res.getErrorCode()});
        return res.getSubtractMoney();
    }

    public long addMoneyInGame(UserScore score, int roomId, int gameId) {
        if (GameUtils.isCheat) {
            this.currentMoney += score.money;
            return score.money;
        }
        MoneyResponse res = this.moneyService.addingMoneyInGame(this.sessionId, this.nickName, GameUtils.gameName, String.valueOf(roomId), score.money, this.moneyTypeName, this.requireMoney, String.valueOf(gameId), score.wastedMoney);
        this.currentMoney = res.getCurrentMoney();
        this.freezeMoney = res.getFreezeMoney();
        Debug.trace((Object[])new Object[]{"addMoneyInGame", this.sessionId, res.isSuccess(), res.getErrorCode()});
        return score.money;
    }

    public long getFreezeMoney() {
        return this.freezeMoney;
    }
}

