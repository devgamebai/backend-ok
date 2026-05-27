/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages.pay;

import com.vinplay.vbee.common.messages.BaseMessage;

public class ExchangeMessage
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    public String nickname;
    public String merchantId;
    public String merchantTransId;
    public String transNo;
    public long money;
    public String moneyType;
    public String type;
    public long exchangeMoney;
    public long fee;
    public int code;
    public String ip;

    public ExchangeMessage(String nickname, String merchantId, String merchantTransId, String transNo, long money, String moneyType, String type, long exchangeMoney, long fee, int code, String ip) {
        this.nickname = nickname;
        this.merchantId = merchantId;
        this.merchantTransId = merchantTransId;
        this.transNo = transNo;
        this.money = money;
        this.moneyType = moneyType;
        this.type = type;
        this.exchangeMoney = exchangeMoney;
        this.fee = fee;
        this.code = code;
        this.ip = ip;
    }
}

