/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.TranferMoneyResponse
 */
package com.vinplay.dal.service;

import com.vinplay.vbee.common.response.TranferMoneyResponse;
import java.util.List;

public interface ListMoneyTranferService {
    public List<TranferMoneyResponse> listMoneyTranfer(String var1, int var2, int var3, int var4);

    public TranferMoneyResponse getMoneyTranferByTransNo(String var1);

    public int countTotalRecord(String var1, int var2, int var3, int var4);
}

