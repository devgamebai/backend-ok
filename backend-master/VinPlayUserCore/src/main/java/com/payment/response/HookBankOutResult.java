/*
 * Decompiled with CFR 0.152.
 */
package com.payment.response;

import com.payment.model.Code;
import com.payment.model.Result;

public class HookBankOutResult
extends Result<String> {
    private boolean rollback;
    private String requestId;
    private String result_message;
    private long amount;

    public HookBankOutResult(Code code) {
        super(code);
    }

    public String getRequestId() {
        return this.requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public static HookBankOutResult success(String json) {
        HookBankOutResult result = new HookBankOutResult(Code.SUCCESS);
        result.setData(json);
        return result;
    }

    public static HookBankOutResult error(String msg) {
        HookBankOutResult result = new HookBankOutResult(Code.ERROR);
        result.setData(msg);
        return result;
    }

    public String getResult_message() {
        return this.result_message;
    }

    public void setResult_message(String result_message) {
        this.result_message = result_message;
    }

    public boolean isRollback() {
        return this.rollback;
    }

    public void setRollback(boolean rollback) {
        this.rollback = rollback;
    }

    public long getAmount() {
        return this.amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }
}

