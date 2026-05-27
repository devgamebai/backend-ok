/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.gamebai.entities.TopTourModel
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.gamebai.response;

import com.vinplay.gamebai.entities.TopTourModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.List;

public class TopGameTourResponse
extends BaseResponseModel {
    private int totalPages;
    private List<TopTourModel> tops;

    public TopGameTourResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public int getTotalPages() {
        return this.totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public List<TopTourModel> getTops() {
        return this.tops;
    }

    public void setTops(List<TopTourModel> tops) {
        this.tops = tops;
    }
}

