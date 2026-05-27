/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages.statistic;

import com.vinplay.vbee.common.messages.BaseMessage;

public class LoginPortalInfoMsg
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    private int userId;
    private String username;
    private String nickname;
    private String ip;
    private String agent;
    private int type;
    private String platform;
    private String deviceName;
    private String location;
    private String loginMethod;
    private long createdAtTs;
    private boolean isAbnormal;
    private java.util.List<String> abnormalReasons;
    private int riskScore;

    public LoginPortalInfoMsg(int userId, String username, String nickname, String ip, String agent, int type, String platform, String deviceName, String location, String loginMethod, long createdAtTs) {
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.ip = ip;
        this.agent = agent;
        this.type = type;
        this.platform = platform;
        this.deviceName = deviceName;
        this.location = location;
        this.loginMethod = loginMethod;
        this.createdAtTs = createdAtTs;
    }

    public int getUserId() {
        return this.userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
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

    public String getIp() {
        return this.ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getAgent() {
        return this.agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public static long getSerialversionuid() {
        return 1L;
    }

    public String getPlatform() {
        return this.platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLoginMethod() {
        return loginMethod;
    }

    public void setLoginMethod(String loginMethod) {
        this.loginMethod = loginMethod;
    }

    public long getCreatedAtTs() {
        return createdAtTs;
    }

    public void setCreatedAtTs(long createdAtTs) {
        this.createdAtTs = createdAtTs;
    }

    public boolean isAbnormal() {
        return isAbnormal;
    }

    public void setAbnormal(boolean isAbnormal) {
        this.isAbnormal = isAbnormal;
    }

    public java.util.List<String> getAbnormalReasons() {
        return abnormalReasons;
    }

    public void setAbnormalReasons(java.util.List<String> abnormalReasons) {
        this.abnormalReasons = abnormalReasons;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }
}

