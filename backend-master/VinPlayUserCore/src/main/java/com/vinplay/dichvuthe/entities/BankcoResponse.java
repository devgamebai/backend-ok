/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dichvuthe.entities;

public class BankcoResponse {
    private String id;
    private String bank;
    private String account;
    private String name;
    private int amount;
    private int code;

    public BankcoResponse(String id, String bank, String account, String name, int amount, int code) {
        this.id = id;
        this.bank = bank;
        this.account = account;
        this.name = name;
        this.amount = amount;
        this.code = code;
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

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}

