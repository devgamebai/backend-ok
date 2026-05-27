/*
 * Decompiled with CFR 0.152.
 */
package com.gamebase.cacheModels;

import com.gamebase.entities.MissionStatus;
import com.gamebase.entities.UserMission;

public class UserMissionCacheModel {
    private int id;
    private String nickname;
    private String missionId;
    private int progress;
    private long point;
    private long work;
    private MissionStatus status;

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMissionId() {
        return this.missionId;
    }

    public void setMissionId(String missionId) {
        this.missionId = missionId;
    }

    public int getProgress() {
        return this.progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public MissionStatus getStatus() {
        return this.status;
    }

    public void setStatus(MissionStatus status) {
        this.status = status;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public long getPoint() {
        return this.point;
    }

    public void setPoint(long point) {
        this.point = point;
    }

    public long getWork() {
        return this.work;
    }

    public void setWork(long work) {
        this.work = work;
    }

    public UserMission toUserMission() {
        UserMission userMission = new UserMission();
        userMission.setId(this.id);
        userMission.setNickname(this.nickname);
        userMission.setMissionId(this.missionId);
        userMission.setProgress(this.progress);
        userMission.setPoint(this.point);
        userMission.setWork(this.work);
        userMission.setStatus(this.status);
        return userMission;
    }

    public static UserMissionCacheModel fromUserMission(UserMission userMission) {
        UserMissionCacheModel cacheModel = new UserMissionCacheModel();
        cacheModel.setId(userMission.getId());
        cacheModel.setNickname(userMission.getNickname());
        cacheModel.setMissionId(userMission.getMissionId());
        cacheModel.setProgress(userMission.getProgress());
        cacheModel.setPoint(userMission.getPoint());
        cacheModel.setWork(userMission.getWork());
        cacheModel.setStatus(userMission.getStatus());
        return cacheModel;
    }
}

