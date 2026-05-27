/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.core.type.TypeReference
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package com.gamebase.entities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamebase.entities.MissionStatus;
import com.gamebase.entities.UserMissionRule;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class UserMission
implements Serializable {
    private static final long serialVersionUID = -2345345632346L;
    private int id;
    private String uId;
    private int eventId;
    private int userId;
    private String nickname;
    private String missionId;
    private int progress;
    private long point;
    private long work;
    private MissionStatus status;
    private Date created_at;
    private Date updated_at;
    private Map<String, UserMissionRule> rules = new HashMap<String, UserMissionRule>();
    private Map<String, String> metaData = new HashMap<String, String>();

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return this.userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
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

    public Date getCreated_at() {
        return this.created_at;
    }

    public void setCreated_at(Date created_at) {
        this.created_at = created_at;
    }

    public Date getUpdated_at() {
        return this.updated_at;
    }

    public void setUpdated_at(Date updated_at) {
        this.updated_at = updated_at;
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

    public int getEventId() {
        return this.eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public boolean isComplete() {
        return this.status == MissionStatus.COMPLETE;
    }

    public boolean isReward() {
        return this.status == MissionStatus.REWARD || this.status == MissionStatus.REWARD_IN_PROGRESS;
    }

    public String getUId() {
        return this.uId;
    }

    public void setUId(String uId) {
        this.uId = uId;
    }

    public Map<String, UserMissionRule> getRules() {
        return this.rules;
    }

    public void setRules(Map<String, UserMissionRule> rules) {
        this.rules = rules;
    }

    public String getRuleString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this.rules);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void setRuleString(String saveRule) {
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<HashMap<String, UserMissionRule>> typeRef = new TypeReference<HashMap<String, UserMissionRule>>(){};
        try {
            this.rules = (Map)mapper.readValue(saveRule, (TypeReference)typeRef);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, String> getMetaData() {
        return this.metaData;
    }

    public void setMetaData(Map<String, String> metaData) {
        this.metaData = metaData;
    }

    public String getMetaDataString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this.metaData);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void setMetaDataString(String saveRule) {
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<HashMap<String, String>> typeRef = new TypeReference<HashMap<String, String>>(){};
        try {
            this.metaData = (Map)mapper.readValue(saveRule, (TypeReference)typeRef);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setMetaDataDeposit(String deposit, Long amount) {
        this.metaData.put(deposit, Long.toString(amount));
    }

    public Long getMetaDataDeposit(String deposit) {
        String amountStr = this.metaData.get(deposit);
        if (amountStr == null) {
            return 0L;
        }
        return Long.valueOf(amountStr);
    }
}

