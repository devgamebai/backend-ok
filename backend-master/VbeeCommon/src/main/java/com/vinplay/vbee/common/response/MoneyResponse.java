/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;

public class MoneyResponse
extends BaseResponseModel {
    private long currentMoney;
    private long currentMoneyXu;
    private long subtractMoney;
    private long safeMoney;
    private long freezeMoney;
    private long moneyUse;

    public MoneyResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public long getMoneyUse() {
        return this.moneyUse;
    }

    public void setMoneyUse(long moneyUse) {
        this.moneyUse = BalanceGuard.clamp(moneyUse, "MoneyResponse.moneyUse");
    }

    public long getCurrentMoney() {
        return this.currentMoney;
    }

    public void setCurrentMoney(long currentMoney) {
        this.currentMoney = BalanceGuard.clamp(currentMoney, "MoneyResponse.currentMoney");
    }

    public long getSubtractMoney() {
        return this.subtractMoney;
    }

    public void setSubtractMoney(long subtractMoney) {
        this.subtractMoney = subtractMoney;
    }

    public long getSafeMoney() {
        return this.safeMoney;
    }

    public void setSafeMoney(long safeMoney) {
        this.safeMoney = safeMoney;
    }

    public long getFreezeMoney() {
        return this.freezeMoney;
    }

    public void setFreezeMoney(long freezeMoney) {
        this.freezeMoney = freezeMoney;
    }

    public long getCurrentMoneyXu() {
        return this.currentMoneyXu;
    }

    public void setCurrentMoneyXu(long currentMoneyXu) {
        this.currentMoneyXu = BalanceGuard.clamp(currentMoneyXu, "MoneyResponse.currentMoneyXu");
    }

    public long getMoneyInGame() {
        return this.moneyUse + this.freezeMoney;
    }
}

