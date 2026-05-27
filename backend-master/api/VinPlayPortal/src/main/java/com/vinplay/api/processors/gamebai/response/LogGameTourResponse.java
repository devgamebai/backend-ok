/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.gamebai.entities.UserTourModel
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.gamebai.response;

import com.vinplay.gamebai.entities.UserTourModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.List;

public class LogGameTourResponse
extends BaseResponseModel {
    private int totalPages;
    private List<UserTourModel> tours;

    public LogGameTourResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public int getTotalPages() {
        return this.totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public List<UserTourModel> getTours() {
        return this.tours;
    }

    public void setTours(List<UserTourModel> tours) {
        this.tours = tours;
    }
}

