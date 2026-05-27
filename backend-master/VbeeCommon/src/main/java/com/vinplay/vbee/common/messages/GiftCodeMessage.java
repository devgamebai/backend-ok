/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages;

import com.vinplay.vbee.common.messages.BaseMessage;

public class GiftCodeMessage
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    public String GiftCode;
    public String Price;
    public int Quantity;
    public String Source;
    public int GiftCodeUse;
    public int Status;
    public int MoneyType;
    public String CreateTime;
    public String Release;
    public String GiftCodeFull;
    public String Mobile;
    public String Type;
    public String eventType;
    public String reqAdmin;

    public GiftCodeMessage() {
    }

    public GiftCodeMessage(String giftCode, String price, int quantity, String source, int giftCodeUse, int status, int moneyType, String release, String type, String reqAdmin) {
        this.GiftCode = giftCode;
        this.Price = price;
        this.Quantity = quantity;
        this.Source = source;
        this.GiftCodeUse = giftCodeUse;
        this.Status = status;
        this.MoneyType = moneyType;
        this.Release = release;
        this.Type = type;
        this.reqAdmin = reqAdmin;
    }

    public String getReqAdmin() {
        return this.reqAdmin;
    }

    public void setReqAdmin(String reqAdmin) {
        this.reqAdmin = reqAdmin;
    }

    public String getEventType() {
        return this.eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getType() {
        return this.Type;
    }

    public void setType(String type) {
        this.Type = type;
    }

    public String getMobile() {
        return this.Mobile;
    }

    public void setMobile(String mobile) {
        this.Mobile = mobile;
    }

    public String getGiftCode() {
        return this.GiftCode;
    }

    public void setGiftCode(String giftCode) {
        this.GiftCode = giftCode;
    }

    public String getPrice() {
        return this.Price;
    }

    public void setPrice(String price) {
        this.Price = price;
    }

    public int getQuantity() {
        return this.Quantity;
    }

    public void setQuantity(int quantity) {
        this.Quantity = quantity;
    }

    public String getSource() {
        return this.Source;
    }

    public void setSource(String source) {
        this.Source = source;
    }

    public int getGiftCodeUse() {
        return this.GiftCodeUse;
    }

    public void setGiftCodeUse(int giftCodeUse) {
        this.GiftCodeUse = giftCodeUse;
    }

    public int getStatus() {
        return this.Status;
    }

    public void setStatus(int status) {
        this.Status = status;
    }

    public int getMoneyType() {
        return this.MoneyType;
    }

    public void setMoneyType(int moneyType) {
        this.MoneyType = moneyType;
    }

    @Override
    public String getCreateTime() {
        return this.CreateTime;
    }

    @Override
    public void setCreateTime(String createTime) {
        this.CreateTime = createTime;
    }

    public String getRelease() {
        return this.Release;
    }

    public void setRelease(String release) {
        this.Release = release;
    }

    public String getGiftCodeFull() {
        return this.GiftCodeFull;
    }

    public void setGiftCodeFull(String giftCodeFull) {
        this.GiftCodeFull = giftCodeFull;
    }
}

