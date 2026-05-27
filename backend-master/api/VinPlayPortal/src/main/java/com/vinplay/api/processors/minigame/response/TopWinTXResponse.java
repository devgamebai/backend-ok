/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.minigame.TopWin
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.minigame.response;

import com.vinplay.vbee.common.models.minigame.TopWin;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class TopWinTXResponse
extends BaseResponseModel {
    private List<TopWin> topTX = new ArrayList<TopWin>();

    public TopWinTXResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<TopWin> getTopTX() {
        return this.topTX;
    }

    public void setTopTX(List<TopWin> topTX) {
        this.topTX = topTX;
    }
}

