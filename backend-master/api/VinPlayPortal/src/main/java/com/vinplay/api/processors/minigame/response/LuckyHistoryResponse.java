/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.entities.vqmm.LuckyHistory
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.minigame.response;

import com.vinplay.usercore.entities.vqmm.LuckyHistory;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class LuckyHistoryResponse
extends BaseResponseModel {
    private int totalPages;
    private List<LuckyHistory> results = new ArrayList<LuckyHistory>();

    public LuckyHistoryResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public LuckyHistoryResponse(boolean success, String errorCode, int totalPages, List<LuckyHistory> results) {
        super(success, errorCode);
        this.totalPages = totalPages;
        this.results = results;
    }

    public int getTotalPages() {
        return this.totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public List<LuckyHistory> getResults() {
        return this.results;
    }

    public void setResults(List<LuckyHistory> results) {
        this.results = results;
    }
}

