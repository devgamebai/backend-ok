/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonProperty
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package game.third.hooks.game568win.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import game.third.hooks.game568win.response.BaseResponse;

public class BalanceResponse
extends BaseResponse {
    @JsonProperty(value="Balance")
    private long balance;

    public long getBalance() {
        return this.balance;
    }

    public void setBalance(double balance) {
        this.balance = (long)balance;
    }

    public String toJson() {
        try {
            String json = new ObjectMapper().writeValueAsString((Object)this);
            System.out.println("BalanceResponse.toJson: " + json);
            return json;
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}

