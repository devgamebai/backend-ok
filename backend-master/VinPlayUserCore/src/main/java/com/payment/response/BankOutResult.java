/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package com.payment.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.entities.HistoryApplyForEntity;
import com.payment.model.Code;

public class BankOutResult {
    private HistoryApplyForEntity historyApplyFor;
    private Code code;
    private String msg;

    public BankOutResult(Code code) {
        this.code = code;
    }

    public HistoryApplyForEntity getHistoryApplyFor() {
        return this.historyApplyFor;
    }

    public void setHistoryApplyFor(HistoryApplyForEntity historyApplyFor) {
        this.historyApplyFor = historyApplyFor;
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

    public String toResult() {
        Result result = new Result();
        result.status = this.code;
        result.msg = this.msg;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(result);
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{\"code\": 0,\"msg\": \"json parser error\"}";
        }
    }

    public static class Result {
        private Code status;
        private String msg;

        public String getMsg() {
            return this.msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public Code getStatus() {
            return this.status;
        }

        public void setStatus(Code status) {
            this.status = status;
        }
    }
}

