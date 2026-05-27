/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;

public class LuckyResponse
extends BaseResponseModel {
    private String resultVin;
    private String resultXu;
    private String resultSlot;
    private int rotateCount;
    private long currentMoneyVin;
    private long currentMoneyXu;

    public LuckyResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public LuckyResponse(boolean success, String errorCode, String resultVin, String resultXu, String resultSlot, int rotateCount, long currentMoneyVin, long currentMoneyXu) {
        super(success, errorCode);
        this.resultVin = resultVin;
        this.resultXu = resultXu;
        this.resultSlot = resultSlot;
        this.rotateCount = rotateCount;
        this.currentMoneyVin = currentMoneyVin;
        this.currentMoneyXu = currentMoneyXu;
    }

    public String getResultVin() {
        return this.resultVin;
    }

    public void setResultVin(String resultVin) {
        this.resultVin = resultVin;
    }

    public String getResultXu() {
        return this.resultXu;
    }

    public void setResultXu(String resultXu) {
        this.resultXu = resultXu;
    }

    public String getResultSlot() {
        return this.resultSlot;
    }

    public void setResultSlot(String resultSlot) {
        this.resultSlot = resultSlot;
    }

    public int getRotateCount() {
        return this.rotateCount;
    }

    public void setRotateCount(int rotateCount) {
        this.rotateCount = rotateCount;
    }

    public long getCurrentMoneyVin() {
        return this.currentMoneyVin;
    }

    public void setCurrentMoneyVin(long currentMoneyVin) {
        this.currentMoneyVin = currentMoneyVin;
    }

    public long getCurrentMoneyXu() {
        return this.currentMoneyXu;
    }

    public void setCurrentMoneyXu(long currentMoneyXu) {
        this.currentMoneyXu = currentMoneyXu;
    }
}

