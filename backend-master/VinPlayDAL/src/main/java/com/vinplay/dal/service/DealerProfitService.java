/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.userMission.LogReceivedRewardObj
 */
package com.vinplay.dal.service;

import com.vinplay.dal.entities.taixiu.DealerProfit;
import com.vinplay.vbee.common.response.userMission.LogReceivedRewardObj;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public interface DealerProfitService {
    public boolean addDealerProfit(long phienid, int result, long total_money_tai, long total_money_xiu, long total_profit, Date created_time) throws SQLException;

    public ArrayList<DealerProfit> getListDealerProfit(Date startTime, Date endTime);
}

