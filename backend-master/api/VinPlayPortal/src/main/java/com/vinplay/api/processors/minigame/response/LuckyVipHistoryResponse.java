/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.entities.vqmm.LuckyVipHistory
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.minigame.response;

import com.vinplay.usercore.entities.vqmm.LuckyVipHistory;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class LuckyVipHistoryResponse
extends BaseResponseModel {
    private int totalPages;
    private List<LuckyVipHistory> results = new ArrayList<LuckyVipHistory>();

    public LuckyVipHistoryResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public int getTotalPages() {
        return this.totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public List<LuckyVipHistory> getResults() {
        return this.results;
    }

    public void setResults(List<LuckyVipHistory> results) {
        this.results = results;
    }
}

