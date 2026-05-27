/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

public class TotalDoanhSoResponse {
    public String nickName;
    public long total;
    public String agentName;
    public int top;
    public long bonusFix;
    public long bonusMore;
    public long bonusTotal;
    public long bonusByVinCash;
    public long bonusByVinplayCard;
    public int percent;

    public TotalDoanhSoResponse() {
    }

    public TotalDoanhSoResponse(String nickName, long total, String agentName, int top, long bonusFix, long bonusMore, long bonusTotal, long bonusByVinCash, long bonusByVinplayCard, int percent) {
        this.nickName = nickName;
        this.total = total;
        this.agentName = agentName;
        this.top = top;
        this.bonusFix = bonusFix;
        this.bonusMore = bonusMore;
        this.bonusTotal = bonusTotal;
        this.bonusByVinCash = bonusByVinCash;
        this.bonusByVinplayCard = bonusByVinplayCard;
        this.percent = percent;
    }

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public long getTotal() {
        return this.total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public String getAgentName() {
        return this.agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public int getTop() {
        return this.top;
    }

    public void setTop(int top) {
        this.top = top;
    }

    public long getBonusFix() {
        return this.bonusFix;
    }

    public void setBonusFix(long bonusFix) {
        this.bonusFix = bonusFix;
    }

    public long getBonusMore() {
        return this.bonusMore;
    }

    public void setBonusMore(long bonusMore) {
        this.bonusMore = bonusMore;
    }

    public long getBonusTotal() {
        return this.bonusTotal;
    }

    public void setBonusTotal(long bonusTotal) {
        this.bonusTotal = bonusTotal;
    }

    public long getBonusByVinCash() {
        return this.bonusByVinCash;
    }

    public void setBonusByVinCash(long bonusByVinCash) {
        this.bonusByVinCash = bonusByVinCash;
    }

    public long getBonusByVinplayCard() {
        return this.bonusByVinplayCard;
    }

    public void setBonusByVinplayCard(long bonusByVinplayCard) {
        this.bonusByVinplayCard = bonusByVinplayCard;
    }

    public int getPercent() {
        return this.percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }
}

