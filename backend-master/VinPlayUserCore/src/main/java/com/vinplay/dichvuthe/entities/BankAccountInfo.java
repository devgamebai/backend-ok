/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dichvuthe.entities;

public class BankAccountInfo {
    private String bank;
    private String name;
    private String account;

    public String getBank() {
        return this.bank;
    }

    public void setBank(String bank) {
        this.bank = bank;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccount() {
        return this.account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public BankAccountInfo(String bank, String name, String account) {
        this.bank = bank;
        this.name = name;
        this.account = account;
    }
}

