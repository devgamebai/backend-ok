/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.vinplay.vippoint.entiies.EventVPTopStrongModel
 */
package com.vinplay.api.processors.vippoint.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.vippoint.entiies.EventVPTopStrongModel;
import java.util.List;

public class EventVPTopStrongResponse {
    public List<EventVPTopStrongModel> strongs;
    public int total;
    public EventVPTopStrongModel strong;

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

