/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.entities.caothap.VinhDanhCaoThap
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.minigame.response;

import com.vinplay.dal.entities.caothap.VinhDanhCaoThap;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class VinhDanhCaoThapResponse
extends BaseResponseModel {
    private int totalPages;
    private List<VinhDanhCaoThap> results = new ArrayList<VinhDanhCaoThap>();

    public VinhDanhCaoThapResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<VinhDanhCaoThap> getResults() {
        return this.results;
    }

    public void setResults(List<VinhDanhCaoThap> results) {
        this.results = results;
    }

    public int getTotalPages() {
        return this.totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}

