/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dichvuthe.entities;

public class BankcoObj {
    private String id;
    private String bank;
    private String account;
    private String name;
    private int amount;
    private int status;
    private String message;
    private String sign;

    public BankcoObj(String id, String bank, String account, String name, int amount, int status, String message, String sign) {
        this.id = id;
        this.bank = bank;
        this.account = account;
        this.name = name;
        this.amount = amount;
        this.status = status;
        this.message = message;
        this.sign = sign;
    }

    public BankcoObj() {
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBank() {
        return this.bank;
    }

    public void setBank(String bank) {
        this.bank = bank;
    }

    public String getAccount() {
        return this.account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getSign() {
        return this.sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }
}

