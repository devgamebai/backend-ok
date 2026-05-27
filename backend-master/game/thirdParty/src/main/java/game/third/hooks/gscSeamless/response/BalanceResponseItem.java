package game.third.hooks.gscSeamless.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * BalanceRequest class represents the request for a balance transaction.
 */

public class BalanceResponseItem {
    private String member_account;
    private int product_code;
    private double balance;
    private int code;
    private String message;

    public BalanceResponseItem(String member_account, int product_code, double balance, int code, String message) {
        this.member_account = member_account;
        this.product_code = product_code;
        this.balance = balance;
        this.code = code;
        this.message = message;
    }

    public BalanceResponseItem() {}

    public String getMember_account() {
        return member_account;
    }

    public void setMember_account(String member_account) {
        this.member_account = member_account;
    }

    public int getProduct_code() {
        return product_code;
    }

    public void setProduct_code(int product_code) {
        this.product_code = product_code;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String toJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{}"; // Return empty JSON object in case of error
        }
    }

    public static BalanceResponseItem fromJson(String json) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(json, BalanceResponseItem.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            return null; // Return null in case of error
        }
    }
}