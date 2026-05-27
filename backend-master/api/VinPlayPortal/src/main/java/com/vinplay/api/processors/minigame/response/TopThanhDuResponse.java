/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.cache.ThanhDuTXModel
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 */
package com.vinplay.api.processors.minigame.response;

import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.models.cache.ThanhDuTXModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.util.ArrayList;
import java.util.List;

public class TopThanhDuResponse
extends BaseResponseModel {

    private List<TopThanhDuModel> results = new ArrayList<TopThanhDuModel>();

    public TopThanhDuResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public void setTopThanhDu(List<ThanhDuTXModel> input, String[] prizes) {
        for (int i = 0; i < input.size(); ++i) {
            ThanhDuTXModel entry = input.get(i);
            TopThanhDuModel model = new TopThanhDuModel();
            model.username = entry.username;
            model.number = entry.number;
            model.totalMoney = entry.totalValue;
            model.referenceId = entry.currentReferenceId;
            if (i < prizes.length) {
                model.prize = prizes[i];
            }
            this.results.add(model);
        }
    }

    public static String[] getWinPrizesDaily() {
        String [] winPrizesDaily = new String[0];
        try {
            winPrizesDaily = GameCommon.getValueStr("THANH_DU_WIN_PRIZES_DAILY").split(",");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return  winPrizesDaily;
    }

    public static String[] getLossPrizesDaily() {
        String [] winPrizesDaily = new String[0];
        try {
            winPrizesDaily = GameCommon.getValueStr("THANH_DU_LOSS_PRIZES_DAILY").split(",");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return  winPrizesDaily;
    }

    public static String[] getWinPrizesMonthly() {
        String [] winPrizesMonthly = new String[0];
        try {
            winPrizesMonthly = GameCommon.getValueStr("THANH_DU_WIN_PRIZES_MONTHLY").split(",");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return  winPrizesMonthly;
    }

    public static String[] getLossPrizesMonthly() {
        String [] lossPrizesMonthly = new String[0];
        try {
            lossPrizesMonthly = GameCommon.getValueStr("THANH_DU_LOSS_PRIZES_MONTHLY").split(",");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return  lossPrizesMonthly;
    }

    public List<TopThanhDuModel> getResults() {
        return this.results;
    }

    public class TopThanhDuModel {
        public String username;
        public int number;
        public long totalMoney;
        public long referenceId;
        public String prize = "";
    }

}

