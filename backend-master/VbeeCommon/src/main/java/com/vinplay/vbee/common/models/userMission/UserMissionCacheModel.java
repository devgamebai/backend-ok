/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.userMission;

import com.vinplay.vbee.common.models.userMission.MissionObj;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class UserMissionCacheModel
implements Serializable {
    private static final long serialVersionUID = 1L;
    private Date lastActive;
    private long lastMessageId;
    private int userId;
    private String userName;
    private String nickName;
    private List<MissionObj> listMission;

    public UserMissionCacheModel() {
    }

    public UserMissionCacheModel(Date lastActive, long lastMessageId, int userId, String userName, String nickName, List<MissionObj> listMission) {
        this.lastActive = lastActive;
        this.lastMessageId = lastMessageId;
        this.userId = userId;
        this.userName = userName;
        this.nickName = nickName;
        this.listMission = listMission;
    }

    public List<MissionObj> getListMission() {
        return this.listMission;
    }

    public void setListMission(List<MissionObj> listMission) {
        this.listMission = listMission;
    }

    public Date getLastActive() {
        return this.lastActive;
    }

    public void setLastActive(Date lastActive) {
        this.lastActive = lastActive;
    }

    public long getLastMessageId() {
        return this.lastMessageId;
    }

    public void setLastMessageId(long lastMessageId) {
        this.lastMessageId = lastMessageId;
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
}

