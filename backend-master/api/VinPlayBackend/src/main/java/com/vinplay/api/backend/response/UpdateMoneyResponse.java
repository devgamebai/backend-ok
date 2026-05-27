/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.backend.response;

import com.vinplay.api.backend.models.UpdateMoneyModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class UpdateMoneyResponse
extends BaseResponseModel {
    private List<UpdateMoneyModel> listResponse = new ArrayList<UpdateMoneyModel>();

    public UpdateMoneyResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<UpdateMoneyModel> getListResponse() {
        return this.listResponse;
    }

    public void setListResponse(List<UpdateMoneyModel> listResponse) {
        this.listResponse = listResponse;
    }
}

