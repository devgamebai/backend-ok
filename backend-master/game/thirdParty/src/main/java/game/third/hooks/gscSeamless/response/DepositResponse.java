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

public class DepositResponse {
    private int code;
    private String message;
    private double before_balance;
    private double balance;

    public DepositResponse(int code, String message, double before_balance, double balance) {
        this.code = code;
        this.message = message;
        this.before_balance = before_balance;
        this.balance = balance;
    }

    public DepositResponse() {
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
        java.text.DecimalFormatSymbols sym = new java.text.DecimalFormatSymbols();
        sym.setDecimalSeparator('.');
        java.text.DecimalFormat df = new java.text.DecimalFormat("0.####", sym);
        String bb = df.format(this.before_balance);
        String b = df.format(this.balance);
        return "{\"code\":" + this.code + ",\"message\":\"" + this.message + "\",\"data\":[{\"code\":" + this.code + ",\"message\":\"" + this.message + "\",\"before_balance\":" + bb + ",\"balance\":" + b + "}]}";
    }
}

