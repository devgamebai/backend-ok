/*
 * Decompiled with CFR 0.152.
 */
package com.payment.response;

import com.payment.model.Code;
import com.payment.model.Result;

public class HookBankInResult
extends Result<String> {
    private String requestId;
    private long amount;

    public HookBankInResult(Code code) {
        super(code);
    }

    public static HookBankInResult success(String json) {
        HookBankInResult result = new HookBankInResult(Code.SUCCESS);
        result.setData(json);
        return result;
    }

    public static HookBankInResult error(String msg) {
        HookBankInResult result = new HookBankInResult(Code.ERROR);
        result.setData(msg);
        return result;
    }

    public String getRequestId() {
        return this.requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public long getAmount() {
        return this.amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }
}

