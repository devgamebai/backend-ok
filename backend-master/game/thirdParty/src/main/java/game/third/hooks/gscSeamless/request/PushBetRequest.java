/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.google.gson.Gson
 *  com.google.gson.JsonSyntaxException
 */
package game.third.hooks.gscSeamless.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.util.List;

public class PushBetRequest {
    private String operator_code;
    private String currency;
    private List<Transaction> transactions;
    private List<Transaction> wagers;
    private String sign;
    private String request_time;

    public List<Transaction> resolveTransactions() {
        if (transactions != null && !transactions.isEmpty()) return transactions;
        return wagers;
    }

    public PushBetRequest(String operator_code, String currency, List<Transaction> transactions, String sign, String request_time) {
        this.operator_code = operator_code;
        this.currency = currency;
        this.transactions = transactions;
        this.sign = sign;
        this.request_time = request_time;
    }

    public PushBetRequest() {
    }

    public String getOperator_code() {
        return this.operator_code;
    }

    public void setOperator_code(String operator_code) {
        this.operator_code = operator_code;
    }

    public String getCurrency() {
        return this.currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<Transaction> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public String getSign() {
        return this.sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getRequest_time() {
        return this.request_time;
    }

    public void setRequest_time(String request_time) {
        this.request_time = request_time;
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

    public static PushBetRequest fromJson(String json) {
        Gson gson = new Gson();
        try {
            return (PushBetRequest)gson.fromJson(json, PushBetRequest.class);
        }
        catch (JsonSyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static class Transaction
    extends game.third.hooks.gscSeamless.request.Transaction {
        private String currency;
        public String getCurrency() { return currency; }
        private String member_account;

        public String getMember_account() {
            return this.member_account;
        }

        public void setMember_account(String member_account) {
            this.member_account = member_account;
        }
    }
}

