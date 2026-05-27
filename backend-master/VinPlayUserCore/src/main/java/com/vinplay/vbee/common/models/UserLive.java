/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.vbee.common.models;

public class UserLive {
    private int id;
    private String username;
    private String nickname;
    private String mobile;
    private String email;
    private long vinTotal;
    private long xuTotal;
    private long safe;
    private String createTime;
    private boolean isLive;

    public UserLive() {
    }

    public UserLive(int id, String username, String nickname, String mobile, String email, long vinTotal, long xuTotal, long safe, String createTime, boolean isLive) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.mobile = mobile;
        this.email = email;
        this.vinTotal = vinTotal;
        this.xuTotal = xuTotal;
        this.safe = safe;
        this.createTime = createTime;
        this.isLive = isLive;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getMobile() {
        return this.mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public long getVinTotal() {
        return this.vinTotal;
    }

    public void setVinTotal(long vinTotal) {
        this.vinTotal = vinTotal;
    }

    public long getXuTotal() {
        return this.xuTotal;
    }

    public void setXuTotal(long xuTotal) {
        this.xuTotal = xuTotal;
    }

    public long getSafe() {
        return this.safe;
    }

    public void setSafe(long safe) {
        this.safe = safe;
    }

    public String getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public boolean isLive() {
        return this.isLive;
    }

    public void setLive(boolean isLive) {
        this.isLive = isLive;
    }
}

