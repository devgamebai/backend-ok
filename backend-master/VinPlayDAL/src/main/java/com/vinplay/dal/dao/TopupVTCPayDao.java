/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.MoneyTotalFollowFaceValue
 *  com.vinplay.vbee.common.response.topupVTCPay.LogTopupVTCPay
 */
package com.vinplay.dal.dao;

import com.vinplay.vbee.common.response.MoneyTotalFollowFaceValue;
import com.vinplay.vbee.common.response.topupVTCPay.LogTopupVTCPay;
import java.util.List;

public interface TopupVTCPayDao {
    public List<LogTopupVTCPay> getLogTopupVtcPay(String var1, String var2, String var3, String var4, String var5, String var6);

    public List<MoneyTotalFollowFaceValue> doiSoatTopupVtcPay(String var1, String var2);
}

