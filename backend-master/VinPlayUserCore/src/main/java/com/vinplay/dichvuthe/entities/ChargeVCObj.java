/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dichvuthe.entities;

public class ChargeVCObj {
    private String id;
    private String partner;
    private String target;
    private String serial;
    private String pin;
    private int amount;
    private int status;
    private String message;

    public ChargeVCObj() {
    }

    public ChargeVCObj(String id, String partner, String target, String serial, String pin, int amount, int status, String message) {
        this.id = id;
        this.partner = partner;
        this.target = target;
        this.serial = serial;
        this.pin = pin;
        this.amount = amount;
        this.status = status;
        this.message = message;
    }

    public String getTarget() {
        return this.target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPartner() {
        return this.partner;
    }

    public void setPartner(String partner) {
        this.partner = partner;
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

