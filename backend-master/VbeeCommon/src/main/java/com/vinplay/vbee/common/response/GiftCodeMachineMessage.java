/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

public class GiftCodeMachineMessage {
    public String GiftCode;
    public String Price;
    public int Quantity;
    public String Source;
    public int GiftCodeUse;
    public String CreateTime;

    public GiftCodeMachineMessage(String giftCode, String price, int quantity, String source, int giftCodeUse) {
        this.GiftCode = giftCode;
        this.Price = price;
        this.Quantity = quantity;
        this.Source = source;
        this.GiftCodeUse = giftCodeUse;
    }
}

