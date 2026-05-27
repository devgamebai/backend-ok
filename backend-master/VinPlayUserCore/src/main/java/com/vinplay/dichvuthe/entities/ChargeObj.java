/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dichvuthe.entities;

public class ChargeObj {
    private String id;
    private String provider;
    private String serial;
    private String pin;
    private int amount;
    private int status;
    private String message;
    private String channel;
    private String userId;

    public ChargeObj(String id, String provider, String serial, String pin, int amount, int status, String message, String channel, String userId) {
        this.id = id;
        this.provider = provider;
        this.serial = serial;
        this.pin = pin;
        this.amount = amount;
        this.status = status;
        this.message = message;
        this.channel = channel;
        this.userId = userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public ChargeObj(String id, String provider, String serial, String pin, int amount, int status, String message, String channel) {
        this.id = id;
        this.provider = provider;
        this.serial = serial;
        this.pin = pin;
        this.amount = amount;
        this.status = status;
        this.message = message;
        this.channel = channel;
    }

    public ChargeObj() {
    }

    public String getChannel() {
        return this.channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProvider() {
        return this.provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
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

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

