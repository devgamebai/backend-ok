/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package com.vinplay.vtcpay.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TopupResponse {
    private int status;
    private String responseCode;
    private String description;
    private String timeResponse;

    public TopupResponse(int status, String responseCode, String description, String timeResponse) {
        this.status = status;
        this.responseCode = responseCode;
        this.description = description;
        this.timeResponse = timeResponse;
    }

    public TopupResponse() {
    }

    public String getTimeResponse() {
        return this.timeResponse;
    }

    public void setTimeResponse(String timeResponse) {
        this.timeResponse = timeResponse;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getResponseCode() {
        return this.responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
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
}

