/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.AgentResponse;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class ResultAgentRespone
extends BaseResponseModel {
    private List<AgentResponse> transactions = new ArrayList<AgentResponse>();

    public ResultAgentRespone(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<AgentResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<AgentResponse> transactions) {
        this.transactions = transactions;
    }
}

