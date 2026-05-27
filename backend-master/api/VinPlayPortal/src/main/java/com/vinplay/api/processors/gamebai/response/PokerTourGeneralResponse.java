/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.gamebai.entities.PokerTourInfoGeneral
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.gamebai.response;

import com.vinplay.gamebai.entities.PokerTourInfoGeneral;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.List;

public class PokerTourGeneralResponse
extends BaseResponseModel {
    private int totalPages;
    private List<PokerTourInfoGeneral> tours;

    public PokerTourGeneralResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public int getTotalPages() {
        return this.totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public List<PokerTourInfoGeneral> getTours() {
        return this.tours;
    }

    public void setTours(List<PokerTourInfoGeneral> tours) {
        this.tours = tours;
    }
}

