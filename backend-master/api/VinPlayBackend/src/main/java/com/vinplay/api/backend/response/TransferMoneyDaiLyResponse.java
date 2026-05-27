/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.backend.response;

import com.vinplay.vbee.common.response.BaseResponseModel;

public class TransferMoneyDaiLyResponse
extends BaseResponseModel {
    private String nicknameSend;
    private String nicknameReceive;
    private long moneySend;
    private long moneyReceive;

    public TransferMoneyDaiLyResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public String getNicknameSend() {
        return this.nicknameSend;
    }

    public void setNicknameSend(String nicknameSend) {
        this.nicknameSend = nicknameSend;
    }

    public String getNicknameReceive() {
        return this.nicknameReceive;
    }

    public void setNicknameReceive(String nicknameReceive) {
        this.nicknameReceive = nicknameReceive;
    }

    public long getMoneySend() {
        return this.moneySend;
    }

    public void setMoneySend(long moneySend) {
        this.moneySend = moneySend;
    }

    public long getMoneyReceive() {
        return this.moneyReceive;
    }

    public void setMoneyReceive(long moneyReceive) {
        this.moneyReceive = moneyReceive;
    }
}

