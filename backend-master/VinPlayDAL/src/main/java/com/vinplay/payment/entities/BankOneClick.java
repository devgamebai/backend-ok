/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.payment.entities;

import java.io.Serializable;

public class BankOneClick
implements Serializable {
    public Integer id;
    public String code;
    public String bank_name;
    public String bank_logo;
    public Integer stat;
    public String created_at;
    public String updated_at;

    public BankOneClick() {
    }

    public BankOneClick(Integer id, String code, String bank_name, String bank_logo, Integer stat, String created_at, String updated_at) {
        this.id = id;
        this.code = code;
        this.bank_name = bank_name;
        this.bank_logo = bank_logo;
        this.stat = stat;
        this.created_at = created_at;
        this.updated_at = updated_at;
    }
}

