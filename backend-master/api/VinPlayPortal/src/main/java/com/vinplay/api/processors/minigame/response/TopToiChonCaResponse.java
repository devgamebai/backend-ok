/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.minigame.baucua.ToiChonCa
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.minigame.response;

import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.models.minigame.baucua.ToiChonCa;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class TopToiChonCaResponse
extends BaseResponseModel {

    private List<ToiChonCa> results = new ArrayList<ToiChonCa>();

    public TopToiChonCaResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<ToiChonCa> getResults() {
        return this.results;
    }

    public void setResults(List<ToiChonCa> results) {
        this.results = results;
        String[] prizes = getWinPrizesDaily();
        for (int i = 0; i < prizes.length && i < results.size(); ++i) {
            ToiChonCa entry = results.get(i);
            entry.prize = prizes[i];
        }
    }

    public static String[] getWinPrizesDaily() {
        String [] winPrizesDaily = new String[0];
        try {
            winPrizesDaily = GameCommon.getValueStr("TCT_WIN_PRIZES_DAILY").split(",");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return  winPrizesDaily;
    }
}

