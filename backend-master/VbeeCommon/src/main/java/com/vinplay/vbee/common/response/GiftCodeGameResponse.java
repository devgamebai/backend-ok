/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

public class GiftCodeGameResponse {
    public String giftCode;
    public String release;
    public String source;
    public String nickName;
    public String userName;
    public String timeLog;
    public String updateTime;
    public String surfing;
    public String giftCodeFull;
    public int quantity;
    public int moneyType;
    public int useGiftCode;
    public int block;

    public GiftCodeGameResponse() {
    }

    public GiftCodeGameResponse(String giftCode, String release, String source, String surfing, int quantity) {
        this.giftCode = giftCode;
        this.release = release;
        this.source = source;
        this.surfing = surfing;
        this.quantity = quantity;
    }
}

