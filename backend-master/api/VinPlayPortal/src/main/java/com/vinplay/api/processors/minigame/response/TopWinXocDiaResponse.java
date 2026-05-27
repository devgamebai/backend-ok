/*
 * Decompiled with CFR 0.152.
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

public class TopWinXocDiaResponse
extends BaseResponseModel {
    private List<TopWin> topXocDia = new ArrayList<TopWin>();

    public TopWinXocDiaResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<TopWin> getTopXocDia() {
        return this.topXocDia;
    }

    public void setTopXocDia(List<TopWin> topXocDia) {
        this.topXocDia = topXocDia;
    }
}

