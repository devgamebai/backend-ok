/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.entities.report.ReportTotalMoneyModel
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.backend.response;

import com.vinplay.dal.entities.report.ReportTotalMoneyModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class ReportTotalMoneyResponse
extends BaseResponseModel {
    public List<ReportTotalMoneyModel> totals = new ArrayList<ReportTotalMoneyModel>();

    public ReportTotalMoneyResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }
}

