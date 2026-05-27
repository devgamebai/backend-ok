package game.third.hooks.gscSeamless.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.List;

/**
 * BalanceRequest class represents the request for a balance transaction.
 */
public class BalanceRequest {
    private List<BalanceRequestItem> batch_requests;
    private String operator_code;
    private String currency;
    private String sign;
    private String request_time;



    public BalanceRequest(List<BalanceRequestItem> batch_requests, String operator_code, String currency, String sign, String request_time) {
        this.batch_requests = batch_requests;
        this.operator_code = operator_code;
        this.currency = currency;
        this.sign = sign;
        this.request_time = request_time;
    }

    public BalanceRequest() {}

    // Getters and Setters

    public List<BalanceRequestItem> getBatchRequests() {
        return batch_requests;
    }

    public void setBatch_requests(List<BalanceRequestItem> batch_requests) {
        this.batch_requests = batch_requests;
    }

    public String getOperator_code() {
        return operator_code;
    }

    public void setOperator_code(String operator_code) {
        this.operator_code = operator_code;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getRequest_time() {
        return request_time;
    }

    public void setRequest_time(String request_time) {
        this.request_time = request_time;
    }

    /**
     * Convert the BalanceRequest object to a JSON string.
     *
     * @return JSON representation of the BalanceRequest object.
     */
    public String toJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{}"; // Return empty JSON object in case of error
        }
    }

    /**
     * Create a BalanceRequest object from a JSON string.
     *
     * @param json JSON representation of the BalanceRequest object.
     * @return BalanceRequest object.
     */
    public static BalanceRequest fromJson(String json) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(json, BalanceRequest.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            return null; // Return null in case of error
        }
    }
}