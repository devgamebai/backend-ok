/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.gamebai.LogNoHuGameBaiMessage
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.gamebai.response;

import com.vinplay.vbee.common.messages.gamebai.LogNoHuGameBaiMessage;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.List;

public class LogNoHuGameBaiResponse
extends BaseResponseModel {
    private List<LogNoHuGameBaiMessage> noHu;
    private int totalPages;

    public LogNoHuGameBaiResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public LogNoHuGameBaiResponse(boolean success, String errorCode, List<LogNoHuGameBaiMessage> noHu, int totalPages) {
        super(success, errorCode);
        this.noHu = noHu;
        this.totalPages = totalPages;
    }

    public List<LogNoHuGameBaiMessage> getNoHu() {
        return this.noHu;
    }

    public void setNoHu(List<LogNoHuGameBaiMessage> noHu) {
        this.noHu = noHu;
    }

    public int getTotalPages() {
        return this.totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}

