/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.minigame.galaxy.LSGDGalaxy
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.minigame.response;

import com.vinplay.vbee.common.models.minigame.galaxy.LSGDGalaxy;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class LSGDGalaxyResponse
extends BaseResponseModel {
    private int totalPages;
    private List<LSGDGalaxy> results = new ArrayList<LSGDGalaxy>();

    public LSGDGalaxyResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public int getTotalPages() {
        return this.totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public List<LSGDGalaxy> getResults() {
        return this.results;
    }

    public void setResults(List<LSGDGalaxy> results) {
        this.results = results;
    }
}

