/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.vbee.common.models;

public class UserSunReal {
    private String username;
    private String nickname;
    private String pass;
    private long vinTotal;
    private String loginTime;

    public UserSunReal() {
    }

    public UserSunReal(String username, String nickname, String pass, long vinTotal, String loginTime) {
        this.username = username;
        this.nickname = nickname;
        this.pass = pass;
        this.vinTotal = vinTotal;
        this.loginTime = loginTime;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPass() {
        return this.pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public long getVinTotal() {
        return this.vinTotal;
    }

    public void setVinTotal(long vinTotal) {
        this.vinTotal = vinTotal;
    }

    public String getLoginTime() {
        return this.loginTime;
    }

    public void setLoginTime(String loginTime) {
        this.loginTime = loginTime;
    }
}

