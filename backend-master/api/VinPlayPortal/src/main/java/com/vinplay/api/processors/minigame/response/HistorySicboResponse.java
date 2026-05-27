/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.minigame.HistorySicbo
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.minigame.response;

import com.vinplay.vbee.common.models.minigame.HistorySicbo;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class HistorySicboResponse
extends BaseResponseModel {
    private List<HistorySicbo> historySicbos = new ArrayList<HistorySicbo>();

    public HistorySicboResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<HistorySicbo> getHistorySicbos() {
        return this.historySicbos;
    }

    public void setHistorySicbos(List<HistorySicbo> historySicbos) {
        this.historySicbos = historySicbos;
    }
}

