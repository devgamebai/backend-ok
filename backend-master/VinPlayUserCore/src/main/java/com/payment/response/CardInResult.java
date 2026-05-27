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
import com.payment.entities.TopUpEntity;
import com.payment.model.Code;

public class CardInResult {
    private TopUpEntity topUpEntity;
    private Code code;
    private String msg;

    public CardInResult(Code code) {
        this.code = code;
    }

    public TopUpEntity getTopUpEntity() {
        return this.topUpEntity;
    }

    public void setTopUpEntity(TopUpEntity topUpEntity) {
        this.topUpEntity = topUpEntity;
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
        return result.json();
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

        public static Result Error(String msg) {
            Result result = new Result();
            result.status = Code.ERROR;
            result.msg = msg;
            return result;
        }

        public String json() {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                return objectMapper.writeValueAsString(this);
            }
            catch (JsonProcessingException e) {
                e.printStackTrace();
                return "{\"code\": 0,\"msg\": \"json parser error\"}";
            }
        }
    }
}

