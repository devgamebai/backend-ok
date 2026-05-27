/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse
 *  com.vinplay.vbee.common.response.RechargeBySmsResponse
 */
package com.vinplay.dal.dao;

import com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse;
import com.vinplay.vbee.common.response.RechargeBySmsResponse;
import java.util.List;

public interface RechargeBySmsPlus9029DAO {
    public List<MoneyTotalRechargeByCardReponse> moneyTotalRechargeBySmsPlus9029(String var1, String var2, String var3);

    public List<RechargeBySmsResponse> exportDataRechargeBySmsPlus(String var1, String var2, String var3, String var4);
}

