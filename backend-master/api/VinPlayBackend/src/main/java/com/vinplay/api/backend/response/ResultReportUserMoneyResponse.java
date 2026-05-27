/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.entities.report.ReportUserMoneyModel
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.backend.response;

import com.vinplay.dal.entities.report.ReportUserMoneyModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class ResultReportUserMoneyResponse
extends BaseResponseModel {
    private List<String> lstExitsNickName = new ArrayList<String>();
    private List<ReportUserMoneyModel> transactions = new ArrayList<ReportUserMoneyModel>();

    public ResultReportUserMoneyResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<String> getLstExitsNickName() {
        return this.lstExitsNickName;
    }

    public void setLstExitsNickName(List<String> lstExitsNickName) {
        this.lstExitsNickName = lstExitsNickName;
    }

    public List<ReportUserMoneyModel> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<ReportUserMoneyModel> transactions) {
        this.transactions = transactions;
    }
}

