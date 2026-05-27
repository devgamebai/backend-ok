/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.slot.NoHuModel
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.slot.response;

import com.vinplay.vbee.common.models.slot.NoHuModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class LSNoHuResponse
extends BaseResponseModel {
    private int totalPages;
    private List<NoHuModel> results = new ArrayList<NoHuModel>();

    public LSNoHuResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public int getTotalPages() {
        return this.totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public List<NoHuModel> getResults() {
        return this.results;
    }

    public void setResults(List<NoHuModel> results) {
        this.results = results;
    }
}

