/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

public class NapXuResponse {
    public static final byte SUCCESS = 0;
    public static final byte FAIL = 1;
    public static final byte NOT_ENOUGH_MONEY = 2;
    public static final byte NOT_SECURITY = 3;
    public static final byte SECURITY_TIME = 10;
    private byte result;
    private long currentMoneyVin;
    private long currentMoneyXu;
    private byte isAuth;

    public byte getResult() {
        return this.result;
    }

    public void setResult(byte result) {
        this.result = result;
    }

    public long getCurrentMoneyVin() {
        return this.currentMoneyVin;
    }

    public void setCurrentMoneyVin(long currentMoneyVin) {
        this.currentMoneyVin = BalanceGuard.clamp(currentMoneyVin, "NapXuResponse.currentMoneyVin");
    }

    public long getCurrentMoneyXu() {
        return this.currentMoneyXu;
    }

    public void setCurrentMoneyXu(long currentMoneyXu) {
        this.currentMoneyXu = BalanceGuard.clamp(currentMoneyXu, "NapXuResponse.currentMoneyXu");
    }

    public byte getIsAuth() {
        return isAuth;
    }

    public void setIsAuth(byte isAuth) {
        this.isAuth = isAuth;
    }
}

