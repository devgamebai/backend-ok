/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.fasterxml.jackson.databind.ObjectWriter
 *  com.fasterxml.jackson.databind.SerializationFeature
 */
package com.vinplay.dichvuthe.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.vinplay.dichvuthe.response.RechargeResponse;

public class RechargePaywellResponse
extends RechargeResponse {
    private String data;

    public String getData() {
        return this.data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public RechargePaywellResponse(String data) {
        super(1, 0L, 0, 0L);
        this.data = data;
    }

    public RechargePaywellResponse(int code, long currentMoney, int fail, long time, String data) {
        super(code, currentMoney, fail, time);
        this.data = data;
    }

    @Override
    public String toJson() {
        ObjectWriter ow = new ObjectMapper().writer();
        ow.with(SerializationFeature.INDENT_OUTPUT);
        try {
            String json = ow.writeValueAsString(this);
            return json;
        }
        catch (Exception e) {
            return null;
        }
    }
}

