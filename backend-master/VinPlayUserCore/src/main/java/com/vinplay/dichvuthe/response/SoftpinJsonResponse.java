/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package com.vinplay.dichvuthe.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SoftpinJsonResponse {
    public String id;
    private String serial;
    private String pin;
    private String expire;

    public SoftpinJsonResponse() {
    }

    public SoftpinJsonResponse(String id, String serial, String pin, String expire) {
        this.id = id;
        this.serial = serial;
        this.pin = pin;
        this.expire = expire;
    }

    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        }
        catch (JsonProcessingException e) {
            return "";
        }
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSerial() {
        return this.serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getPin() {
        return this.pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getExpire() {
        return this.expire;
    }

    public void setExpire(String expire) {
        this.expire = expire;
    }
}

