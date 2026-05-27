/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.usercore.entities.vqmm;

import java.io.Serializable;

public class LuckyHistory
implements Serializable {
    private static final long serialVersionUID = 1L;
    private long transId;
    private String nickname;
    private String resultVin;
    private String resultXu;
    private String resultSlot;
    private String description;
    private String transTime;

    public LuckyHistory() {
    }

    public LuckyHistory(long transId, String nickname, String resultVin, String resultXu, String resultSlot, String description, String transTime) {
        this.transId = transId;
        this.nickname = nickname;
        this.resultVin = resultVin;
        this.resultXu = resultXu;
        this.resultSlot = resultSlot;
        this.description = description;
        this.transTime = transTime;
    }

    public long getTransId() {
        return this.transId;
    }

    public void setTransId(long transId) {
        this.transId = transId;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getResultVin() {
        return this.resultVin;
    }

    public void setResultVin(String resultVin) {
        this.resultVin = resultVin;
    }

    public String getResultXu() {
        return this.resultXu;
    }

    public void setResultXu(String resultXu) {
        this.resultXu = resultXu;
    }

    public String getResultSlot() {
        return this.resultSlot;
    }

    public void setResultSlot(String resultSlot) {
        this.resultSlot = resultSlot;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTransTime() {
        return this.transTime;
    }

    public void setTransTime(String transTime) {
        this.transTime = transTime;
    }
}

