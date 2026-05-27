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

public class TopBauCuaResponse
extends BaseResponseModel {
    private List<TopWin> topBC = new ArrayList<TopWin>();

    public TopBauCuaResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<TopWin> getTopBC() {
        return this.topBC;
    }

    public void setTopBC(List<TopWin> topBC) {
        this.topBC = topBC;
    }
}

