/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.entities.report.ReportMoneySystemModel
 *  com.vinplay.dal.entities.report.ReportTXModel
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.backend.response;

import com.vinplay.dal.entities.agent.UserDetailAgentModel;
import com.vinplay.dal.entities.report.ReportMoneySystemModel;
import com.vinplay.dal.entities.report.ReportTXModel;
import com.vinplay.vbee.common.models.LogReportModel;
import com.vinplay.vbee.common.response.BaseResponseModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReportLogUserResponse
extends BaseResponseModel {
    public int total = 0;
    public ReportLogUserResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public ReportLogUserResponse(boolean success, String errorCode, int total) {
        super(success, errorCode);
        this.total = total;
    }
}

