/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;

public class UserIndexResponse
extends BaseResponseModel {
    private int register;
    private int recharge;
    private int secMobile;
    private int both;

    public UserIndexResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public int getRegister() {
        return this.register;
    }

    public void setRegister(int register) {
        this.register = register;
    }

    public int getRecharge() {
        return this.recharge;
    }

    public void setRecharge(int recharge) {
        this.recharge = recharge;
    }

    public int getSecMobile() {
        return this.secMobile;
    }

    public void setSecMobile(int secMobile) {
        this.secMobile = secMobile;
    }

    public int getBoth() {
        return this.both;
    }

    public void setBoth(int both) {
        this.both = both;
    }
}

