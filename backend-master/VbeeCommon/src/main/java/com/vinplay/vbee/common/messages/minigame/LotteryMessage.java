/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.vbee.common.messages.minigame;

import java.util.Date;

public class LotteryMessage {
    private long id;
    private long userId;
    private String nickName;
    private long betValue;
    private long mode;
    private String ticket;
    private Long prize;
    private Date createdDate;
    private Date updatedDate;
    // SUN-1295: per-bet rate/prize snapshot — frozen at purchase time so a
    // future LotteryMode change can't retroactively rewrite a pending payout.
    // null on legacy rows that pre-date the schema migration; getPrize falls
    // back to LotteryMode enum lookup in that case.
    private Long betUnit;
    private Integer rateAtPurchase;
    private Integer prizeMultiplier;

    public Long getBetUnit() { return this.betUnit; }
    public void setBetUnit(Long betUnit) { this.betUnit = betUnit; }
    public Integer getRateAtPurchase() { return this.rateAtPurchase; }
    public void setRateAtPurchase(Integer rateAtPurchase) { this.rateAtPurchase = rateAtPurchase; }
    public Integer getPrizeMultiplier() { return this.prizeMultiplier; }
    public void setPrizeMultiplier(Integer prizeMultiplier) { this.prizeMultiplier = prizeMultiplier; }

    public long getUserId() {
        return this.userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public long getBetValue() {
        return this.betValue;
    }

    public void setBetValue(long betValue) {
        this.betValue = betValue;
    }

    public long getMode() {
        return this.mode;
    }

    public void setMode(long mode) {
        this.mode = mode;
    }

    public String getTicket() {
        return this.ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public Long getPrize() {
        return this.prize;
    }

    public void setPrize(Long prize) {
        this.prize = prize;
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getCreatedDate() {
        return this.createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public LotteryMessage() {
    }

    public LotteryMessage(long id, long userId, String nickName, long betValue, long mode, String ticket, Long prize) {
        this.id = id;
        this.userId = userId;
        this.nickName = nickName;
        this.betValue = betValue;
        this.mode = mode;
        this.ticket = ticket;
        this.prize = prize;
    }

    public LotteryMessage(long userId, String nickName, long betValue, long mode, String ticket, Long prize) {
        this.userId = userId;
        this.nickName = nickName;
        this.betValue = betValue;
        this.mode = mode;
        this.ticket = ticket;
        this.prize = prize;
    }

    public LotteryMessage(long id, long userId, String nickName, long betValue, long mode, String ticket, Long prize, Date createdDate) {
        this.id = id;
        this.userId = userId;
        this.nickName = nickName;
        this.betValue = betValue;
        this.mode = mode;
        this.ticket = ticket;
        this.prize = prize;
        this.createdDate = createdDate;
    }

    public Date getUpdatedDate() {
        return this.updatedDate;
    }

    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
    }
}

