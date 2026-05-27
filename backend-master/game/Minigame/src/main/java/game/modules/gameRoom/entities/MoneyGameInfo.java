/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  bitzero.util.common.business.Debug
 */
package game.modules.gameRoom.entities;

import bitzero.server.entities.User;
import bitzero.util.common.business.Debug;
import game.modules.gameRoom.entities.GameRoomSetting;

public class MoneyGameInfo {
    public static final String GAME_MONEY_INFO = "GAME_MONEY_INFO";
    public int userId;
    public int moneyType;
    public long currentMoney;
    public long winMoney;
    public long requireMoney;
    public long preChargedMoney;

    public MoneyGameInfo(User user, GameRoomSetting setting) {
        this.userId = user.getId();
        this.moneyType = setting.moneyType;
        this.requireMoney = setting.requiredMoney;
        this.startGameUpdateMoney();
    }

    public boolean startGameUpdateMoney() {
        boolean result = this.freezeMoney();
        if (result) {
            this.winMoney = 0L;
            return true;
        }
        return false;
    }

    public boolean moneyCheck() {
        this.preChargedMoney += this.winMoney;
        this.winMoney = 0L;
        if (this.preChargedMoney < this.requireMoney) {
            boolean result = this.freezeMoney();
            if (result) {
                return true;
            }
            this.restoreMoney();
            return false;
        }
        return true;
    }

    public boolean freezeMoney() {
        Debug.trace("Dong bang tai khoan khach hang");
        this.preChargedMoney = this.requireMoney;
        return true;
    }

    public void restoreMoney() {
        Debug.trace("Hoan tien cho tai khoan khach hang");
        this.preChargedMoney = 0L;
    }

    public boolean outGameUpdate() {
        this.restoreMoney();
        return true;
    }
}

