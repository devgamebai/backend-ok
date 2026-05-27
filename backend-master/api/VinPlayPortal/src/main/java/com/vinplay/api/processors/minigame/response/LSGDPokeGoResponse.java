/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.minigame.pokego.LSGDPokeGo
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.minigame.response;

import com.vinplay.vbee.common.models.minigame.pokego.LSGDPokeGo;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class LSGDPokeGoResponse
extends BaseResponseModel {
    private int totalPages;
    private List<LSGDPokeGo> results = new ArrayList<LSGDPokeGo>();

    public LSGDPokeGoResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public int getTotalPages() {
        return this.totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public List<LSGDPokeGo> getResults() {
        return this.results;
    }

    public void setResults(List<LSGDPokeGo> results) {
        this.results = results;
    }
}

