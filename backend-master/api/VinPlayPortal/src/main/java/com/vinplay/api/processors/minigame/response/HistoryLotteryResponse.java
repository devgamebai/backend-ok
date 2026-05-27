/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.minigame.LotteryMessage
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.minigame.response;

import com.vinplay.vbee.common.messages.minigame.LotteryMessage;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class HistoryLotteryResponse
extends BaseResponseModel {
    private List<LotteryMessage> historyLottery = new ArrayList<LotteryMessage>();

    public HistoryLotteryResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<LotteryMessage> getHistoryLottery() {
        return this.historyLottery;
    }

    public void setHistoryLottery(List<LotteryMessage> historyLottery) {
        this.historyLottery = historyLottery;
    }
}

