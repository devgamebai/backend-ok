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

public class BaseResponse {
    private int code;
    private String message;
    private double before_balance;
    private double balance;

    public BaseResponse(int code, String message, double before_balance, double balance) {
        this.code = code;
        this.message = message;
        this.before_balance = before_balance;
        this.balance = balance;
    }

    public BaseResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public BaseResponse() {
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

    public double getBefore_balance() {
        return this.before_balance;
    }

    public void setBefore_balance(double before_balance) {
        this.before_balance = before_balance;
    }

    public double getBalance() {
        return this.balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
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

