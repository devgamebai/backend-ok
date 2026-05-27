/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models;

public class LoginDailyMarketingModel {
    public String nickName;
    public String numLogin;
    public String timeLogin;

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getNumLogin() {
        return this.numLogin;
    }

    public void setNumLogin(String numLogin) {
        this.numLogin = numLogin;
    }

    public String getTimeLogin() {
        return this.timeLogin;
    }

    public void setTimeLogin(String timeLogin) {
        this.timeLogin = timeLogin;
    }

    public LoginDailyMarketingModel(String nickName, String numLogin, String timeLogin) {
        this.nickName = nickName;
        this.numLogin = numLogin;
        this.timeLogin = timeLogin;
    }
}

