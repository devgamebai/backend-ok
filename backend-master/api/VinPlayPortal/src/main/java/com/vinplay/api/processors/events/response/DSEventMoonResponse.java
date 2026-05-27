/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  com.vinplay.vbee.common.response.MoonEventResponse
 */
package com.vinplay.api.processors.events.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.MoonEventResponse;
import java.util.ArrayList;
import java.util.List;

public class DSEventMoonResponse
extends BaseResponseModel {
    private List<MoonEventResponse> lstMoonEvents = new ArrayList<MoonEventResponse>();

    public DSEventMoonResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public void setLstMoonEvents(List<MoonEventResponse> lstMoonEvents) {
        this.lstMoonEvents = lstMoonEvents;
    }

    public List<MoonEventResponse> getLstMoonEvents() {
        return this.lstMoonEvents;
    }
}

