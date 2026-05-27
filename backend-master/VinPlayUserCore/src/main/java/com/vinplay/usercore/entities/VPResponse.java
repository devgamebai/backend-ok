/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.usercore.entities;

import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.List;

public class VPResponse
extends BaseResponseModel {
    private int vippoint;
    private int vippointSave;
    private List<Integer> ratioList;

    public VPResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<Integer> getRatioList() {
        return this.ratioList;
    }

    public void setRatioList(List<Integer> ratioList) {
        this.ratioList = ratioList;
    }

    public int getVippoint() {
        return this.vippoint;
    }

    public void setVippoint(int vippoint) {
        this.vippoint = vippoint;
    }

    public int getVippointSave() {
        return this.vippointSave;
    }

    public void setVippointSave(int vippointSave) {
        this.vippointSave = vippointSave;
    }

    public VPResponse(boolean success, String errorCode, int vippoint, int vippointSave) {
        super(success, errorCode);
        this.vippoint = vippoint;
        this.vippointSave = vippointSave;
    }
}

