/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.BaseMessage
 */
package com.gamebase.messages.userMission;

import com.vinplay.vbee.common.messages.BaseMessage;

public class RewardUserMissionMessage
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    private String missionId;
    private String nickname;

    public String getMissionId() {
        return this.missionId;
    }

    public void setMissionId(String missionId) {
        this.missionId = missionId;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}

