/*
 * Decompiled with CFR 0.152.
 */
package com.payment.response;

import com.payment.model.Code;
import com.payment.response.Bank;
import java.util.List;

public class BankListResult {
    private List<Bank> banks;
    private Code code;
    private String msg;

    public BankListResult(Code code) {
        this.code = code;
    }

    public List<Bank> getBanks() {
        return this.banks;
    }

    public void setBanks(List<Bank> banks) {
        this.banks = banks;
    }

    public Code getCode() {
        return this.code;
    }

    public void setCode(Code code) {
        this.code = code;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}

