/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;

public class UpdateAllPendingCardResponse
extends BaseResponseModel {
    private long totalRecord;
    private long successRecord;

    public UpdateAllPendingCardResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public long getTotalRecord() {
        return this.totalRecord;
    }

    public void setTotalRecord(long totalRecord) {
        this.totalRecord = totalRecord;
    }

    public long getSuccessRecord() {
        return this.successRecord;
    }

    public void setSuccessRecord(long successRecord) {
        this.successRecord = successRecord;
    }
}

