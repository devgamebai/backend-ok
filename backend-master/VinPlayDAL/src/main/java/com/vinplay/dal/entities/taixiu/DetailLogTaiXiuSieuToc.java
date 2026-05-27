/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.entities.taixiu;

import java.io.Serializable;

public class DetailLogTaiXiuSieuToc
implements Serializable {
    private static final long serialVersionUID = 1L;
    public long id;
    public long referenceId;
    public long userId;
    public long betAmount;
    public long winAmount;
    public int typed;
    public int status;
    public String betTime;
    public String result;
    public String description;
    public long refundAmount;
    public String ip;
    public String updateDate;
    public int userType;
    public String nickname;

    public DetailLogTaiXiuSieuToc() {
    }

    public DetailLogTaiXiuSieuToc(long id, long referenceId, long userId, long betAmount, long winAmount, int typed, int status, String betTime, String result, String description, long refundAmount, String ip, String updateDate, int userType, String nickname) {
        this.id = id;
        this.referenceId = referenceId;
        this.userId = userId;
        this.betAmount = betAmount;
        this.winAmount = winAmount;
        this.typed = typed;
        this.status = status;
        this.betTime = betTime;
        this.result = result;
        this.description = description;
        this.refundAmount = refundAmount;
        this.ip = ip;
        this.updateDate = updateDate;
        this.userType = userType;
        this.nickname = nickname;
    }
}

