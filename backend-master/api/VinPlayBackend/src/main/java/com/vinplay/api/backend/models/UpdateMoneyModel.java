/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.api.backend.models;

public class UpdateMoneyModel {
    public String nickname;
    public long money;
    public boolean isBot;
    public String errorCode;

    public UpdateMoneyModel(String nickname, long money, boolean isBot, String errorCode) {
        this.nickname = nickname;
        this.money = money;
        this.isBot = isBot;
        this.errorCode = errorCode;
    }

    public UpdateMoneyModel() {
    }
}

