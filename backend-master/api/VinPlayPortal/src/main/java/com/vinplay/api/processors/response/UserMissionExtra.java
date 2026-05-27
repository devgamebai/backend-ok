/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.gamebase.entities.UserMission
 */
package com.vinplay.api.processors.response;

import com.gamebase.entities.UserMission;
import java.util.Date;

public class UserMissionExtra
extends UserMission {
    private String missionName;
    private String missionDescription;
    private Date expiredAt;

    public UserMissionExtra(UserMission userMission) {
        this.setId(userMission.getId());
        this.setUserId(userMission.getUserId());
        this.setEventId(userMission.getEventId());
        this.setNickname(userMission.getNickname());
        this.setMissionId(userMission.getMissionId());
        this.setProgress(userMission.getProgress());
        this.setPoint(userMission.getPoint());
        this.setWork(userMission.getWork());
        this.setStatus(userMission.getStatus());
        this.setCreated_at(userMission.getCreated_at());
        this.setUpdated_at(userMission.getUpdated_at());
        this.setUId(userMission.getUId());
        this.setRules(userMission.getRules());
    }

    public String getMissionName() {
        return this.missionName;
    }

    public void setMissionName(String missionName) {
        this.missionName = missionName;
    }

    public String getMissionDescription() {
        return this.missionDescription;
    }

    public void setMissionDescription(String missionDescription) {
        this.missionDescription = missionDescription;
    }

    public Date getExpiredAt() {
        return this.expiredAt;
    }

    public void setExpiredAt(Date expiredAt) {
        this.expiredAt = expiredAt;
    }
}

