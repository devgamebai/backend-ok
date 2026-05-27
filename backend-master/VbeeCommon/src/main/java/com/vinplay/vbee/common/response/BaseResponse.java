package com.vinplay.vbee.common.response;

import org.json.JSONObject;
import java.io.Serializable;

public class BaseResponse<T> implements Serializable {
    protected boolean success;
    protected String errorCode;
    protected String message;
    protected T statistic;
    protected long totalRecords;
    protected T data;

    public BaseResponse() {}

    public BaseResponse(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
        this.success = "0".equals(errorCode);
    }

    public BaseResponse(String errorCode, String message, T data) {
        this(errorCode, message);
        this.data = data;
    }

    public BaseResponse(boolean success, String errorCode, String message, T data) {
        this.success = success;
        this.errorCode = errorCode;
        this.message = message;
        this.data = data;
    }

    public BaseResponse(boolean success, String errorCode, String message, T data, long totalRecords) {
        this(success, errorCode, message, data);
        this.totalRecords = totalRecords;
    }

    public BaseResponse(boolean success, String errorCode, String message, T data, T statistic, long totalRecords) {
        this(success, errorCode, message, data, totalRecords);
        this.statistic = statistic;
    }

    public long getTotalRecords() { return totalRecords; }
    public void setTotalRecords(long totalRecords) { this.totalRecords = totalRecords; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public T getStatistic() { return statistic; }
    public void setStatistic(T statistic) { this.statistic = statistic; }

    public static String error(String errorCode, String message) {
        JSONObject json = new JSONObject();
        json.put("success", false);
        json.put("errorCode", errorCode);
        json.put("message", message);
        return json.toString();
    }

    public static String success(String errorCode, String message, Object data) {
        JSONObject json = new JSONObject();
        json.put("success", true);
        json.put("errorCode", errorCode);
        json.put("message", message);
        json.put("data", data);
        return json.toString();
    }

    public static String success(Object data, long totalRecords) {
        JSONObject json = new JSONObject();
        json.put("success", true);
        json.put("errorCode", "0");
        json.put("data", data);
        json.put("totalRecords", totalRecords);
        return json.toString();
    }

    public static String success(Object data, long totalRecords, Object statistic) {
        JSONObject json = new JSONObject();
        json.put("success", true);
        json.put("errorCode", "0");
        json.put("data", data);
        json.put("totalRecords", totalRecords);
        json.put("statistic", statistic);
        return json.toString();
    }

    public String success(T data, String message) {
        this.success = true;
        this.errorCode = "0";
        this.data = data;
        this.message = message;
        return toJson();
    }

    public String success(T data) {
        return success(data, "");
    }

    public String toJson() {
        JSONObject json = new JSONObject();
        json.put("success", success);
        json.put("errorCode", errorCode != null ? errorCode : "");
        if (message != null) json.put("message", message);
        if (data != null) json.put("data", data);
        if (statistic != null) json.put("statistic", statistic);
        if (totalRecords > 0) json.put("totalRecords", totalRecords);
        return json.toString();
    }

    @Override
    public String toString() {
        return toJson();
    }
}
