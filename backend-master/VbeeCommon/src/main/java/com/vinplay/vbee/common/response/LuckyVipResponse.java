/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;

public class LuckyVipResponse
extends BaseResponseModel {
    private int rotateCount;
    private int resultVin;
    private int resultMulti;
    private long currentMoney;

    public LuckyVipResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public LuckyVipResponse(boolean success, String errorCode, int rotateCount, int resultVin, int resultMulti, long currentMoney) {
        super(success, errorCode);
        this.rotateCount = rotateCount;
        this.resultVin = resultVin;
        this.resultMulti = resultMulti;
        this.currentMoney = currentMoney;
    }

    public int getRotateCount() {
        return this.rotateCount;
    }

    public void setRotateCount(int rotateCount) {
        this.rotateCount = rotateCount;
    }

    public int getResultVin() {
        return this.resultVin;
    }

    public void setResultVin(int resultVin) {
        this.resultVin = resultVin;
    }

    public int getResultMulti() {
        return this.resultMulti;
    }

    public void setResultMulti(int resultMulti) {
        this.resultMulti = resultMulti;
    }

    public long getCurrentMoney() {
        return this.currentMoney;
    }

    public void setCurrentMoney(long currentMoney) {
        this.currentMoney = currentMoney;
    }
}

