/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay._1pay;

public class SerialsObj {
    private String serial;
    private String pin;
    private int price;
    private String cardType;
    private String expiredDate;

    public SerialsObj(String serial, String pin, int price, String cardType, String expiredDate) {
        this.serial = serial;
        this.pin = pin;
        this.price = price;
        this.cardType = cardType;
        this.expiredDate = expiredDate;
    }

    public SerialsObj() {
    }

    public String getSerial() {
        return this.serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getPin() {
        return this.pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public int getPrice() {
        return this.price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getCardType() {
        return this.cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getExpiredDate() {
        return this.expiredDate;
    }

    public void setExpiredDate(String expiredDate) {
        this.expiredDate = expiredDate;
    }
}

