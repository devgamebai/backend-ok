/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.minigame.galaxy.TopGalaxy
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.minigame.response;

import com.vinplay.vbee.common.models.minigame.galaxy.TopGalaxy;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class TopGalaxyResponse
extends BaseResponseModel {
    private int totalPages;
    private List<TopGalaxy> results = new ArrayList<TopGalaxy>();

    public TopGalaxyResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<TopGalaxy> getResults() {
        return this.results;
    }

    public void setResults(List<TopGalaxy> results) {
        this.results = results;
    }

    public int getTotalPages() {
        return this.totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}

