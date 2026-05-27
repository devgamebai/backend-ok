/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.payment.entities;

import com.vinplay.payment.entities.DepositPaygateModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.List;

public class DepositPaygateReponse
extends BaseResponseModel {
    public long TotalTrans;
    public long TotalMoney;
    public long TotalSuccess;
    public List<DepositPaygateModel> ListTrans;

    public DepositPaygateReponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public DepositPaygateReponse(int totalTrans, long totalMoney, List<DepositPaygateModel> listTrans) {
        super(false, "1001");
        this.TotalTrans = totalTrans;
        this.TotalMoney = totalMoney;
        this.ListTrans = listTrans;
    }

    public DepositPaygateReponse(long totalTrans, long totalMoney, long totalSuccess, List<DepositPaygateModel> listTrans) {
        super(true, "0");
        this.TotalTrans = totalTrans;
        this.TotalMoney = totalMoney;
        this.TotalSuccess = totalSuccess;
        this.ListTrans = listTrans;
    }
}

