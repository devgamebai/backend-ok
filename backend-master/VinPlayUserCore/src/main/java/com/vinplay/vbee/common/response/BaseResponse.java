/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.fasterxml.jackson.databind.ObjectWriter
 *  com.fasterxml.jackson.databind.SerializationFeature
 */
package com.vinplay.vbee.common.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.Serializable;

public class BaseResponse<T>
implements Serializable {
    protected boolean success;
    protected String errorCode;
    protected String message;
    protected T statistic;
    protected long totalRecords;
    protected T data;

    public long getTotalRecords() {
        return this.totalRecords;
    }

    public void setTotalRecords(long totalRecords) {
        this.totalRecords = totalRecords;
    }

    public boolean isSuccess() {
        return this.success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorCode() {
        return this.errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return this.data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public T getStatistic() {
        return this.statistic;
    }

    public void setStatistic(T statistic) {
        this.statistic = statistic;
    }

    public BaseResponse(boolean success, String errorCode, String message, T data, long totalRecords) {
        this.success = success;
        this.errorCode = errorCode;
        this.message = message;
        this.totalRecords = totalRecords;
        this.data = data;
    }

    public BaseResponse(boolean success, String errorCode, String message, T data, T statistic, long totalRecords) {
        this.success = success;
        this.errorCode = errorCode;
        this.message = message;
        this.totalRecords = totalRecords;
        this.statistic = statistic;
        this.data = data;
    }

    public BaseResponse(boolean success, String errorCode, String message, T data) {
        this.success = success;
        this.errorCode = errorCode;
        this.message = message;
        this.totalRecords = 0L;
        this.statistic = null;
        this.data = data;
    }

    public static String error(String errorCode, String message) {
        BaseResponse<Object> base = new BaseResponse<Object>(false, errorCode, message, null);
        return base.toJson();
    }

    public static String success(String errorCode, String message, Object data) {
        BaseResponse<Object> base = new BaseResponse<Object>(true, "0", message, data);
        return base.toJson();
    }

    public static String success(Object data, long totalRecords) {
        BaseResponse<Object> base = new BaseResponse<Object>(true, "0", null, data, totalRecords);
        return base.toJson();
    }

    public static String success(Object data, long totalRecords, Object statistic) {
        BaseResponse<Object> base = new BaseResponse<Object>(true, "0", null, data, statistic, totalRecords);
        return base.toJson();
    }

    public String success(T data, String message) {
        BaseResponse<T> base = new BaseResponse<T>(true, "0", message, data);
        return base.toJson();
    }

    public String success(T data) {
        BaseResponse<T> base = new BaseResponse<T>(true, "0", null, data);
        return base.toJson();
    }

    public BaseResponse() {
        this.success = false;
        this.errorCode = null;
        this.message = "";
        this.data = null;
        this.totalRecords = 0L;
    }

    public BaseResponse(String errorCode, String message) {
        this.success = false;
        this.errorCode = errorCode;
        this.message = message;
        this.totalRecords = 0L;
    }

    public BaseResponse(String errorCode, String message, T data) {
        this.data = data;
        this.success = true;
        this.errorCode = errorCode;
        this.message = message;
        this.totalRecords = 0L;
    }

    public String toString() {
        return "BaseResponse{success=" + this.success + ", errorCode='" + this.errorCode + '\'' + ", message='" + this.message + '\'' + ", totalRecords=" + this.totalRecords + 39 + ", statistic=" + this.statistic + '\'' + ", data=" + this.data + '}';
    }

    public String toJson() {
        ObjectWriter ow = new ObjectMapper().writer();
        ow.with(SerializationFeature.INDENT_OUTPUT);
        try {
            String json = ow.writeValueAsString(this);
            return json;
        }
        catch (Exception e) {
            return null;
        }
    }
}

