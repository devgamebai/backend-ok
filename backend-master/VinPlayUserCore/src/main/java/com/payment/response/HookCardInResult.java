/*
 * Decompiled with CFR 0.152.
 */
package com.payment.response;

import com.payment.model.Code;
import com.payment.model.Result;

public class HookCardInResult
extends Result<String> {
    private String requestId;
    private String result_message;
    private long amount;

    public HookCardInResult(Code code) {
        super(code);
    }

    public static HookCardInResult success(String data) {
        HookCardInResult result = new HookCardInResult(Code.SUCCESS);
        result.setData(data);
        return result;
    }

    public static HookCardInResult error(String msg) {
        HookCardInResult result = new HookCardInResult(Code.ERROR);
        result.setData(msg);
        return result;
    }

    public String getRequestId() {
        return this.requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getResult_message() {
        return this.result_message;
    }

    public void setResult_message(String result_message) {
        this.result_message = result_message;
    }

    public long getAmount() {
        return this.amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }
}

