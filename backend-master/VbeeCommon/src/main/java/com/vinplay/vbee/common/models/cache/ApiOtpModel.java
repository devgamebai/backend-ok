/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.cache;

import java.io.Serializable;

public class ApiOtpModel
implements Serializable {
    private static final long serialVersionUID = 1L;
    private String requestId;
    private String transId;
    private String mobile;
    private String nickname;
    private int amount;

    public ApiOtpModel() {
    }

    public ApiOtpModel(String requestId, String transId, String mobile, String nickname, int amount) {
        this.requestId = requestId;
        this.transId = transId;
        this.mobile = mobile;
        this.nickname = nickname;
        this.amount = amount;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getMobile() {
        return this.mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getRequestId() {
        return this.requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTransId() {
        return this.transId;
    }

    public void setTransId(String transId) {
        this.transId = transId;
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}

