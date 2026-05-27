/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.response.HistoryLog;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.List;

public class LogMoneyUserResponse
extends BaseResponseModel {
    private long totalData;
    private long totalFee;
    private long totalMoneyExchange;
    private List<HistoryLog> list;

    public LogMoneyUserResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public LogMoneyUserResponse(boolean success, String errorCode, Object data) {
        super(success, errorCode, data);
    }

    public long getTotalData() {
        return this.totalData;
    }

    public void setTotalData(long totalData) {
        this.totalData = totalData;
    }

    public long getTotalFee() {
        return this.totalFee;
    }

    public void setTotalFee(long totalFee) {
        this.totalFee = totalFee;
    }

    public long getTotalMoneyExchange() {
        return this.totalMoneyExchange;
    }

    public void setTotalMoneyExchange(long totalMoneyExchange) {
        this.totalMoneyExchange = totalMoneyExchange;
    }

    public List<HistoryLog> getList() {
        return this.list;
    }

    public void setList(List<HistoryLog> list) {
        this.list = list;
    }

    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString((Object)this);
        }
        catch (JsonProcessingException mapper) {
            return "{\"success\":false,\"errorCode\":\"1001\",\"totalData\":\"0\"}";
        }
    }
}

