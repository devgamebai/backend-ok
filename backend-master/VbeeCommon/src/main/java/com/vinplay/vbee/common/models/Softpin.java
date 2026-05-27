/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models;

public class Softpin {
    private String serial;
    private String pin;
    private String expire;

    public Softpin(String serial, String pin, String expire) {
        this.serial = serial;
        this.pin = pin;
        this.expire = expire;
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

