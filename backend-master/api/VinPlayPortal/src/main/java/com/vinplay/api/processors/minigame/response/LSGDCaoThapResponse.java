/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.entities.caothap.LSGDCaoThap
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.minigame.response;

import com.vinplay.dal.entities.caothap.LSGDCaoThap;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class LSGDCaoThapResponse
extends BaseResponseModel {
    private int totalPages;
    private List<LSGDCaoThap> results = new ArrayList<LSGDCaoThap>();

    public LSGDCaoThapResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<LSGDCaoThap> getResults() {
        return this.results;
    }

    public void setResults(List<LSGDCaoThap> results) {
        this.results = results;
    }

    public int getTotalPages() {
        return this.totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}

