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
import com.payment.entities.HistoryBankEntity;
import com.payment.model.Code;
import com.payment.response.BankInfo;

public class BankInResult {
    private HistoryBankEntity historyBank;
    private BankInfo bankInfo;
    private Code code;
    private String msg;

    public BankInfo getBankInfo() {
        return this.bankInfo;
    }

    public void setBankInfo(BankInfo bankInfo) {
        this.bankInfo = bankInfo;
    }

    public BankInResult(Code code) {
        this.code = code;
    }

    public HistoryBankEntity getHistoryBank() {
        return this.historyBank;
    }

    public void setHistoryBank(HistoryBankEntity historyBank) {
        this.historyBank = historyBank;
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
        result.data = this.bankInfo;
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
        private BankInfo data;
        private Code status;
        private String msg;

        public BankInfo getData() {
            return this.data;
        }

        public void setData(BankInfo data) {
            this.data = data;
        }

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

