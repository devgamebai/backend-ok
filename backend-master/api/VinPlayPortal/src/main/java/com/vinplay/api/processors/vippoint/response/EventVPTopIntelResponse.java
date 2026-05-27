/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.vinplay.vippoint.entiies.EventVPTopIntelModel
 */
package com.vinplay.api.processors.vippoint.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.vippoint.entiies.EventVPTopIntelModel;
import java.util.List;

public class EventVPTopIntelResponse {
    public List<EventVPTopIntelModel> intels;
    public int total;
    public EventVPTopIntelModel intel;

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

