/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.gamebai.entities.PokerTourInfoDetail
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.gamebai.response;

import com.vinplay.gamebai.entities.PokerTourInfoDetail;
import com.vinplay.vbee.common.response.BaseResponseModel;

public class PokerTourDetailResponse
extends BaseResponseModel {
    private PokerTourInfoDetail tour;

    public PokerTourDetailResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public PokerTourInfoDetail getTour() {
        return this.tour;
    }

    public void setTour(PokerTourInfoDetail tour) {
        this.tour = tour;
    }
}

