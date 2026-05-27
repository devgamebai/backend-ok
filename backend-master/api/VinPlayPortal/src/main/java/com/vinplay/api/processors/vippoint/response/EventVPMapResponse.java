/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.vinplay.vippoint.entiies.EventVPMapModel
 *  com.vinplay.vippoint.entiies.EventVPTopVipModel
 */
package com.vinplay.api.processors.vippoint.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.vippoint.entiies.EventVPMapModel;
import com.vinplay.vippoint.entiies.EventVPTopVipModel;
import java.util.List;

public class EventVPMapResponse {
    public List<EventVPMapModel> places;
    public EventVPMapModel place;
    public List<EventVPTopVipModel> vips;
    public EventVPTopVipModel vip;
    public int status;
    public String des;

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

