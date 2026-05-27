/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.List;

public class AgentLevel2Response
extends BaseResponseModel {
    private List<String> listAgent;

    public AgentLevel2Response(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<String> getListAgent() {
        return this.listAgent;
    }

    public void setListAgent(List<String> listAgent) {
        this.listAgent = listAgent;
    }
}

