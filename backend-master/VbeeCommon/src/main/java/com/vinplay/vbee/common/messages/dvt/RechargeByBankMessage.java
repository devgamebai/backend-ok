/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages.dvt;

import com.vinplay.vbee.common.messages.BaseMessage;

public class RechargeByBankMessage
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    private String nickname;
    private long money;
    private String bank;
    private String transId;
    private String amount;
    private String orderInfo;
    private String ticketNo;
    private String platform;

    public RechargeByBankMessage(String nickname, long money, String bank, String transId, String amount, String orderInfo, String ticketNo, String platform) {
        this.nickname = nickname;
        this.money = money;
        this.bank = bank;
        this.transId = transId;
        this.amount = amount;
        this.orderInfo = orderInfo;
        this.ticketNo = ticketNo;
        this.platform = platform;
    }

    public RechargeByBankMessage(String nickname, long money, String bank, String transId, String amount, String orderInfo, String ticketNo) {
        this.nickname = nickname;
        this.money = money;
        this.bank = bank;
        this.transId = transId;
        this.amount = amount;
        this.orderInfo = orderInfo;
        this.ticketNo = ticketNo;
    }

    public String getAmount() {
        return this.amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public long getMoney() {
        return this.money;
    }

    public void setMoney(long money) {
        this.money = money;
    }

    public String getBank() {
        return this.bank;
    }

    public void setBank(String bank) {
        this.bank = bank;
    }

    public String getTransId() {
        return this.transId;
    }

    public void setTransId(String transId) {
        this.transId = transId;
    }

    public String getOrderInfo() {
        return this.orderInfo;
    }

    public void setOrderInfo(String orderInfo) {
        this.orderInfo = orderInfo;
    }

    public String getTicketNo() {
        return this.ticketNo;
    }

    public void setTicketNo(String ticketNo) {
        this.ticketNo = ticketNo;
    }

    public String getPlatform() {
        return this.platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }
}

