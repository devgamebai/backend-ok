/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages.userMission;

import com.vinplay.vbee.common.messages.BaseMessage;

public class LogReceivedRewardMissionMessage
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    private int userId;
    private String userName;
    private String nickName;
    private String missionName;
    private int levelReceivedReward;
    private long moneyBonus;
    private long moneyUser;
    private String moneyType;

    public LogReceivedRewardMissionMessage() {
    }

    public LogReceivedRewardMissionMessage(int userId, String userName, String nickName, String missionName, int levelReceivedReward, long moneyBonus, long moneyUser, String moneyType) {
        this.userId = userId;
        this.userName = userName;
        this.nickName = nickName;
        this.missionName = missionName;
        this.levelReceivedReward = levelReceivedReward;
        this.moneyBonus = moneyBonus;
        this.moneyUser = moneyUser;
        this.moneyType = moneyType;
    }

    public int getUserId() {
        return this.userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getMissionName() {
        return this.missionName;
    }

    public void setMissionName(String missionName) {
        this.missionName = missionName;
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
}

