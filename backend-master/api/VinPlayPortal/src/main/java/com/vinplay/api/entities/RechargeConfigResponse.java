/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package com.vinplay.api.entities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.api.entities.Provider;
import java.util.ArrayList;

public class RechargeConfigResponse {
    public int code;
    public ArrayList<Provider> providers;
    public int[] amounts;
    public double rate;

    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        }
        catch (JsonProcessingException mapper) {
            return "{\"code\":500,\"message\":\"error\"}";
        }
    }
}

