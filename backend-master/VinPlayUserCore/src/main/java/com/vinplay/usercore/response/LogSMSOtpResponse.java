/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.usercore.response;

import com.vinplay.usercore.entities.LogSmsOtp;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class LogSMSOtpResponse
extends BaseResponseModel {
    public int totalPages;
    public int totalMessage;
    public int numReceiveSuccess;
    public int numSendSuccess;
    public List<LogSmsOtp> records = new ArrayList<LogSmsOtp>();

    public LogSMSOtpResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public LogSMSOtpResponse(boolean success, String errorCode, int totalPages, int totalMessage, int numReceiveSuccess, int numSendSuccess, List<LogSmsOtp> records) {
        super(success, errorCode);
        this.totalPages = totalPages;
        this.totalMessage = totalMessage;
        this.numReceiveSuccess = numReceiveSuccess;
        this.numSendSuccess = numSendSuccess;
        this.records = records;
    }
}

