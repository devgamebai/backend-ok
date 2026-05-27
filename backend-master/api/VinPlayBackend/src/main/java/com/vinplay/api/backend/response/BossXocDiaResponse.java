/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.gamebai.entities.BossXocDiaModel
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.backend.response;

import com.vinplay.gamebai.entities.BossXocDiaModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class BossXocDiaResponse
extends BaseResponseModel {
    private List<BossXocDiaModel> bossList = new ArrayList<BossXocDiaModel>();

    public BossXocDiaResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<BossXocDiaModel> getBossList() {
        return this.bossList;
    }

    public void setBossList(List<BossXocDiaModel> bossList) {
        this.bossList = bossList;
    }
}

