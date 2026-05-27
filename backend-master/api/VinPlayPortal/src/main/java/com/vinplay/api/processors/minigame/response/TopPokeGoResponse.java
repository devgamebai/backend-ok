/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.minigame.pokego.TopPokeGo
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.minigame.response;

import com.vinplay.vbee.common.models.minigame.pokego.TopPokeGo;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class TopPokeGoResponse
extends BaseResponseModel {
    private int totalPages;
    private List<TopPokeGo> results = new ArrayList<TopPokeGo>();

    public TopPokeGoResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<TopPokeGo> getResults() {
        return this.results;
    }

    public void setResults(List<TopPokeGo> results) {
        this.results = results;
    }

    public int getTotalPages() {
        return this.totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}

