/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.api.backend.response;

public class UserInfoNormalModel {
    private String username;
    private String nickname;
    private String mobile;
    private long vinTotal;

    public UserInfoNormalModel(String username, String nickname, String mobile, long vinTotal) {
        this.username = username;
        this.nickname = nickname;
        this.mobile = mobile;
        this.vinTotal = vinTotal;
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

    public String getMobile() {
        return this.mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public long getVinTotal() {
        return this.vinTotal;
    }

    public void setVinTotal(long vinTotal) {
        this.vinTotal = vinTotal;
    }
}

