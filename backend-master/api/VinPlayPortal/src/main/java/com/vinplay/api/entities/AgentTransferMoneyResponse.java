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

public class AgentTransferMoneyResponse {
    public String nick_name_send;
    public String nick_name_receive;
    public long money_send;
    public long money_receive;
    public int status;
    public long fee;
    public String trans_time;
    public int top_ds;
    public int process;
    public String des_send;
    public String des_receive;
    public String trans_id;
    public int code;
    public String message;

    public AgentTransferMoneyResponse(int _code, String _message) {
        this.code = _code;
        this.message = _message;
    }

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

