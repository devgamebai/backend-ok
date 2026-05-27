/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.entities.report.ReportMoneyUserModel
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.backend.response;

import com.vinplay.dal.entities.report.ReportMoneyUserModel;
import com.vinplay.vbee.common.response.BaseResponseModel;

public class ReportMoneyUserResponse
extends BaseResponseModel {
    public ReportMoneyUserModel users = new ReportMoneyUserModel();

    public ReportMoneyUserResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }
}

