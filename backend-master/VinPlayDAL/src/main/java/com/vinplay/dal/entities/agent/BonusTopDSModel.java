/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dal.entities.agent;

public class BonusTopDSModel {
    private String nickname;
    private long ds;
    private int top;
    private long bonusFix;
    private long bonusMore;
    private long ds2;
    private int top2;
    private long bonusFix2;
    private long bonusMore2;
    private long bonusTotal;
    private String month;
    private int code;
    private String description;
    private String timeLog;
    private long bonusVinplayCard;
    private long bonusVinCash;
    private int percent;

    public BonusTopDSModel(String nickname, long ds, int top, long bonusFix, long bonusMore, long ds2, int top2, long bonusFix2, long bonusMore2, long bonusTotal, String month, int code, String description, String timeLog, long bonusVinplayCard, long bonusVinCash, int percent) {
        this.nickname = nickname;
        this.ds = ds;
        this.top = top;
        this.bonusFix = bonusFix;
        this.bonusMore = bonusMore;
        this.ds2 = ds2;
        this.top2 = top2;
        this.bonusFix2 = bonusFix2;
        this.bonusMore2 = bonusMore2;
        this.bonusTotal = bonusTotal;
        this.month = month;
        this.code = code;
        this.description = description;
        this.timeLog = timeLog;
        this.bonusVinplayCard = bonusVinplayCard;
        this.bonusVinCash = bonusVinCash;
        this.percent = percent;
    }

    public long getDs2() {
        return this.ds2;
    }

    public void setDs2(long ds2) {
        this.ds2 = ds2;
    }

    public int getTop2() {
        return this.top2;
    }

    public void setTop2(int top2) {
        this.top2 = top2;
    }

    public long getBonusFix2() {
        return this.bonusFix2;
    }

    public void setBonusFix2(long bonusFix2) {
        this.bonusFix2 = bonusFix2;
    }

    public long getBonusMore2() {
        return this.bonusMore2;
    }

    public void setBonusMore2(long bonusMore2) {
        this.bonusMore2 = bonusMore2;
    }

    public int getPercent() {
        return this.percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public long getBonusVinplayCard() {
        return this.bonusVinplayCard;
    }

    public void setBonusVinplayCard(long bonusVinplayCard) {
        this.bonusVinplayCard = bonusVinplayCard;
    }

    public long getBonusVinCash() {
        return this.bonusVinCash;
    }

    public void setBonusVinCash(long bonusVinCash) {
        this.bonusVinCash = bonusVinCash;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getBonusTotal() {
        return this.bonusTotal;
    }

    public void setBonusTotal(long bonusTotal) {
        this.bonusTotal = bonusTotal;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public BonusTopDSModel() {
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public long getDs() {
        return this.ds;
    }

    public void setDs(long ds) {
        this.ds = ds;
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

    public String getMonth() {
        return this.month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getTimeLog() {
        return this.timeLog;
    }

    public void setTimeLog(String timeLog) {
        this.timeLog = timeLog;
    }
}

