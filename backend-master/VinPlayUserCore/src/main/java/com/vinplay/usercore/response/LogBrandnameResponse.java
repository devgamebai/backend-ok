/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.usercore.response;

import com.vinplay.brandname.enties.LogBrandname;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class LogBrandnameResponse
extends BaseResponseModel {
    public int totalPages;
    public int numSend;
    public int numSuccess;
    public List<LogBrandname> records = new ArrayList<LogBrandname>();

    public LogBrandnameResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public LogBrandnameResponse(boolean success, String errorCode, int totalPages, int numSend, int numSuccess, List<LogBrandname> records) {
        super(success, errorCode);
        this.totalPages = totalPages;
        this.numSend = numSend;
        this.numSuccess = numSuccess;
        this.records = records;
    }
}

