/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.entities.caothap.TopCaoThap
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.minigame.response;

import com.vinplay.api.entities.CaoThapPrizes;
import com.vinplay.dal.entities.caothap.TopCaoThap;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class TopCaoThapResponse
extends BaseResponseModel {
    private List<CaoThapPrizes> prizes;
    private List<TopCaoThap> results = new ArrayList<TopCaoThap>();

    public TopCaoThapResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<CaoThapPrizes> getPrizes() {
        return this.prizes;
    }

    public void setPrizes(List<CaoThapPrizes> prizes) {
        this.prizes = prizes;
    }

    public List<TopCaoThap> getResults() {
        return this.results;
    }

    public void setResults(List<TopCaoThap> results) {
        this.results = results;
    }
}

