/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.entities.minipoker.VinhDanhMiniPoker
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.minigame.response;

import com.vinplay.dal.entities.minipoker.VinhDanhMiniPoker;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class VinhDanhMiniPokerResponse
extends BaseResponseModel {
    private int totalPages;
    private List<VinhDanhMiniPoker> results = new ArrayList<VinhDanhMiniPoker>();

    public VinhDanhMiniPokerResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<VinhDanhMiniPoker> getResults() {
        return this.results;
    }

    public void setResults(List<VinhDanhMiniPoker> results) {
        this.results = results;
    }

    public int getTotalPages() {
        return this.totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}

