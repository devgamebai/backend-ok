/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response.userMission;

public class LogReceivedRewardObj {
    public int userId;
    public String nickName;
    public String gameName;
    public int levelReceivedReward;
    public long moneyBonus;
    public long moneyUser;
    public String moneyType;
    public String timeLog;

    public LogReceivedRewardObj() {
    }

    public LogReceivedRewardObj(int userId, String nickName, String gameName, int levelReceivedReward, long moneyBonus, long moneyUser, String moneyType, String timeLog) {
        this.userId = userId;
        this.nickName = nickName;
        this.gameName = gameName;
        this.levelReceivedReward = levelReceivedReward;
        this.moneyBonus = moneyBonus;
        this.moneyUser = moneyUser;
        this.moneyType = moneyType;
        this.timeLog = timeLog;
    }

    public int getUserId() {
        return this.userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getGameName() {
        return this.gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public int getLevelReceivedReward() {
        return this.levelReceivedReward;
    }

    public void setLevelReceivedReward(int levelReceivedReward) {
        this.levelReceivedReward = levelReceivedReward;
    }

    public long getMoneyBonus() {
        return this.moneyBonus;
    }

    public void setMoneyBonus(long moneyBonus) {
        this.moneyBonus = moneyBonus;
    }

    public long getMoneyUser() {
        return this.moneyUser;
    }

    public void setMoneyUser(long moneyUser) {
        this.moneyUser = moneyUser;
    }

    public String getMoneyType() {
        return this.moneyType;
    }

    public void setMoneyType(String moneyType) {
        this.moneyType = moneyType;
    }

    public String getTimeLog() {
        return this.timeLog;
    }

    public void setTimeLog(String timeLog) {
        this.timeLog = timeLog;
    }
}

