/*
 * Decompiled with CFR 0.152.
 */
package com.payment.response;

public class Bank {
    private String name;
    private String bank_code;

    public Bank(String name, String bank_code) {
        this.name = name;
        this.bank_code = bank_code;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBank_code() {
        return this.bank_code;
    }

    public void setBank_code(String bank_code) {
        this.bank_code = bank_code;
    }
}

