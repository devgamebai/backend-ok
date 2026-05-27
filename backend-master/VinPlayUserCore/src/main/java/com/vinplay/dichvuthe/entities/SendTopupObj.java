/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dichvuthe.entities;

public class SendTopupObj {
    private String id;
    private String target;
    private int type;
    private int amount;
    private String sign;

    public SendTopupObj() {
    }

    public SendTopupObj(String id, String target, int type, int amount, String sign) {
        this.id = id;
        this.target = target;
        this.type = type;
        this.amount = amount;
        this.sign = sign;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTarget() {
        return this.target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getSign() {
        return this.sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }
}

