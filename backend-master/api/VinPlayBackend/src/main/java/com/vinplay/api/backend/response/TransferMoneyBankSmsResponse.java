/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.backend.response;

import com.vinplay.vbee.common.response.BaseResponseModel;

public class TransferMoneyBankSmsResponse extends BaseResponseModel {
    private long id;
    private String content;
    private String sms;
    private long amount;

    public TransferMoneyBankSmsResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSms() {
        return sms;
    }

    public void setSms(String sms) {
        this.sms = sms;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }
}

