/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.vinplay.vbee.common.messages.pay.ExchangeMessage
 */
package com.vinplay.api.processors.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.vbee.common.messages.pay.ExchangeMessage;

public class TransInfoResponse {
    private int errorCode;
    private String transId;
    private ExchangeMessage trans;

    public TransInfoResponse() {
    }

    public TransInfoResponse(int errorCode, String transId, ExchangeMessage trans) {
        this.errorCode = errorCode;
        this.trans = trans;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public ExchangeMessage getTrans() {
        return this.trans;
    }

    public void setTrans(ExchangeMessage trans) {
        this.trans = trans;
    }

    public String getTransId() {
        return this.transId;
    }

    public void setTransId(String transId) {
        this.transId = transId;
    }

    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString((Object)this);
        }
        catch (JsonProcessingException e) {
            return "";
        }
    }
}

