/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.entities.minipoker.LSGDMiniPoker
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.minigame.response;

import com.vinplay.dal.entities.minipoker.LSGDMiniPoker;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class LSGDMiniPokerResponse
extends BaseResponseModel {
    private int totalPages;
    private List<LSGDMiniPoker> results = new ArrayList<LSGDMiniPoker>();

    public LSGDMiniPokerResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<LSGDMiniPoker> getResults() {
        return this.results;
    }

    public void setResults(List<LSGDMiniPoker> results) {
        this.results = results;
    }

    public int getTotalPages() {
        return this.totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}

