/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.minigame.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class HistoryLotteryResultResponse
extends BaseResponseModel {
    private List<String> historyLotteryResults = new ArrayList<String>();

    public HistoryLotteryResultResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<String> getHistoryLotteryResults() {
        return this.historyLotteryResults;
    }

    public void setHistoryLotteryResults(List<String> historyLotteryResults) {
        this.historyLotteryResults = historyLotteryResults;
    }
}

