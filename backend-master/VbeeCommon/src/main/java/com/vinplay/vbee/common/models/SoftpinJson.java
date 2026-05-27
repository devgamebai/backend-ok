/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models;

public class SoftpinJson {
    private String provider;
    private int amount;
    private String serial;
    private String pin;
    private String expire;

    public SoftpinJson(String provider, int amount, String serial, String pin, String expire) {
        this.provider = provider;
        this.amount = amount;
        this.serial = serial;
        this.pin = pin;
        this.expire = expire;
    }

    public SoftpinJson() {
    }

    public String getProvider() {
        return this.provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
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

    public String getExpire() {
        return this.expire;
    }

    public void setExpire(String expire) {
        this.expire = expire;
    }
}

