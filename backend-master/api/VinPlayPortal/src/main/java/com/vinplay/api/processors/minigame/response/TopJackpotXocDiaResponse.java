/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.xocdia.TopJackpotXocDia
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.minigame.response;

import com.vinplay.vbee.common.models.xocdia.TopJackpotXocDia;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class TopJackpotXocDiaResponse
extends BaseResponseModel {
    private List<TopJackpotXocDia> topJackpot = new ArrayList<TopJackpotXocDia>();

    public TopJackpotXocDiaResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<TopJackpotXocDia> getTopJackpot() {
        return this.topJackpot;
    }

    public void setTopJackpot(List<TopJackpotXocDia> topJackpot) {
        this.topJackpot = topJackpot;
    }
}

