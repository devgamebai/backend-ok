/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.usercore.response;

import com.vinplay.usercore.entities.LogRechargeBankNL;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class LogRechargeBankNLResponse
extends BaseResponseModel {
    public long totalPages;
    public long totalSuccess;
    public long totalMoney;
    public List<LogRechargeBankNL> records = new ArrayList<LogRechargeBankNL>();

    public LogRechargeBankNLResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public LogRechargeBankNLResponse(boolean success, String errorCode, long totalPages, long totalSuccess, long totalMoney, List<LogRechargeBankNL> records) {
        super(success, errorCode);
        this.totalPages = totalPages;
        this.totalSuccess = totalSuccess;
        this.totalMoney = totalMoney;
        this.records = records;
    }
}

