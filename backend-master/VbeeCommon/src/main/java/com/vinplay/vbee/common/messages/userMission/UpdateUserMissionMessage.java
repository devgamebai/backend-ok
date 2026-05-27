/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages.userMission;

import com.vinplay.vbee.common.messages.BaseMessage;

public class UpdateUserMissionMessage
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    private String moneyType;
    private String nickname;
    private String actionName;

    public UpdateUserMissionMessage() {
    }

    public String getMoneyType() {
        return this.moneyType;
    }

    public void setMoneyType(String moneyType) {
        this.moneyType = moneyType;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getActionName() {
        return this.actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public UpdateUserMissionMessage(String moneyType, String nickname, String actionName) {
        this.moneyType = moneyType;
        this.nickname = nickname;
        this.actionName = actionName;
    }
}

