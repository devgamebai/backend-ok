/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.entities.taixiu.VinhDanhRLTLModel
 *  com.vinplay.vbee.common.models.minigame.taixiu.XepHangRLTLModel
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.minigame.response;

import com.vinplay.dal.entities.taixiu.VinhDanhRLTLModel;
import com.vinplay.vbee.common.models.minigame.taixiu.XepHangRLTLModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class LichSuRLTLResponse
extends BaseResponseModel {
    private List<XepHangRLTLModel> xepHang = new ArrayList<XepHangRLTLModel>();
    private List<VinhDanhRLTLModel> vinhDanh = new ArrayList<VinhDanhRLTLModel>();
    private long myMoney = 0L;

    public LichSuRLTLResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<VinhDanhRLTLModel> getVinhDanh() {
        return this.vinhDanh;
    }

    public void setVinhDanh(List<VinhDanhRLTLModel> vinhDanh) {
        this.vinhDanh = vinhDanh;
    }

    public List<XepHangRLTLModel> getXepHang() {
        return this.xepHang;
    }

    public void setXepHang(List<XepHangRLTLModel> xepHang) {
        this.xepHang = xepHang;
    }

    public long getMyMoney() {
        return this.myMoney;
    }

    public void setMyMoney(long myMoney) {
        this.myMoney = myMoney;
    }
}

