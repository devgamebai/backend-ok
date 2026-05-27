/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dichvuthe.entities;

public class SendSoftpinObj {
    private String id;
    private String provider;
    private int amount;
    private int quantity;
    private String sign;

    public SendSoftpinObj() {
    }

    public SendSoftpinObj(String id, String provider, int amount, int quantity, String sign) {
        this.id = id;
        this.provider = provider;
        this.amount = amount;
        this.quantity = quantity;
        this.sign = sign;
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

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getQuantity() {
        return this.quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getSign() {
        return this.sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }
}

