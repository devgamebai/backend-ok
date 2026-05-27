/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.usercore.entities.vqmm;

import java.io.Serializable;

public class LuckyVipHistory
implements Serializable {
    private static final long serialVersionUID = 1L;
    private long transId;
    private String nickname;
    private int resultVin;
    private int resultMulti;
    private String transTime;

    public LuckyVipHistory() {
    }

    public LuckyVipHistory(long transId, String nickname, int resultVin, int resultMulti, String transTime) {
        this.transId = transId;
        this.nickname = nickname;
        this.resultVin = resultVin;
        this.resultMulti = resultMulti;
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

    public int getResultVin() {
        return this.resultVin;
    }

    public void setResultVin(int resultVin) {
        this.resultVin = resultVin;
    }

    public int getResultMulti() {
        return this.resultMulti;
    }

    public void setResultMulti(int resultMulti) {
        this.resultMulti = resultMulti;
    }

    public String getTransTime() {
        return this.transTime;
    }

    public void setTransTime(String transTime) {
        this.transTime = transTime;
    }
}

