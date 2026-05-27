/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package com.vinplay.dichvuthe.entities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SMSPlusResponse {
    private int status;
    private String sms;
    private String type;

    public SMSPlusResponse(int status, String sms, String type) {
        this.status = status;
        this.sms = sms;
        this.type = type;
    }

    public SMSPlusResponse() {
    }

    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        }
        catch (JsonProcessingException e) {
            return "{\"status\":0,\"sms\":\",\"type\":\"}";
        }
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getSms() {
        return this.sms;
    }

    public void setSms(String sms) {
        this.sms = sms;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

