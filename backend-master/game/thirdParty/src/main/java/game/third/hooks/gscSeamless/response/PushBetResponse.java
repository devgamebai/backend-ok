/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package game.third.hooks.gscSeamless.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PushBetResponse {
    private int code;
    private String message;

    public PushBetResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public PushBetResponse() {
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String toJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString((Object)this);
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{}";
        }
    }
}

