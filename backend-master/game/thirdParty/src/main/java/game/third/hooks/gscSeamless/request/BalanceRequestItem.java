package game.third.hooks.gscSeamless.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * BalanceRequest class represents the request for a balance transaction.
 */

public class BalanceRequestItem {
    private String member_account;
    private int product_code;

    public BalanceRequestItem(String member_account, int product_code) {
        this.member_account = member_account;
        this.product_code = product_code;
    }

    public BalanceRequestItem() {}

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

    public String toJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{}"; // Return empty JSON object in case of error
        }
    }

    public static BalanceRequestItem fromJson(String json) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(json, BalanceRequestItem.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            return null; // Return null in case of error
        }
    }
}