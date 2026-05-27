/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.gamebai.entities;

import java.util.Calendar;

public class GameFreeCodeDetail {
    public int id;
    public int packageId;
    public String code;
    public String gamename;
    public int type;
    public int amount;
    public int status;
    public Calendar expire;
    public Calendar createTime;
    public String nickname;
    public String addInfo;
    public Calendar useTime;

    public GameFreeCodeDetail(int id, int packageId, String code, String gamename, int type, int amount, int status, Calendar expire, Calendar createTime, String nickname, String addInfo, Calendar useTime) {
        this.id = id;
        this.packageId = packageId;
        this.code = code;
        this.gamename = gamename;
        this.type = type;
        this.amount = amount;
        this.status = status;
        this.expire = expire;
        this.createTime = createTime;
        this.nickname = nickname;
        this.addInfo = addInfo;
        this.useTime = useTime;
    }

    public GameFreeCodeDetail() {
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPackageId() {
        return this.packageId;
    }

    public void setPackageId(int packageId) {
        this.packageId = packageId;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getGamename() {
        return this.gamename;
    }

    public void setGamename(String gamename) {
        this.gamename = gamename;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Calendar getExpire() {
        return this.expire;
    }

    public void setExpire(Calendar expire) {
        this.expire = expire;
    }

    public Calendar getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(Calendar createTime) {
        this.createTime = createTime;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAddInfo() {
        return this.addInfo;
    }

    public void setAddInfo(String addInfo) {
        this.addInfo = addInfo;
    }

    public Calendar getUseTime() {
        return this.useTime;
    }

    public void setUseTime(Calendar useTime) {
        this.useTime = useTime;
    }
}

