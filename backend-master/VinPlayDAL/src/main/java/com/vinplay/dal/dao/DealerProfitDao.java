/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.vippoint.UserVPEventModel
 *  com.vinplay.vbee.common.response.userMission.LogReceivedRewardObj
 */
package com.vinplay.dal.dao;

import com.vinplay.dal.entities.taixiu.DealerProfit;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.vippoint.UserVPEventModel;
import com.vinplay.vbee.common.response.userMission.LogReceivedRewardObj;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public interface DealerProfitDao {
    public boolean addDealerProfit(long phienid, int result, long total_money_tai, long total_money_xiu
            , long total_profit, long last_balance) throws SQLException;
    public ArrayList<DealerProfit> getListDealerProfit(String startTime, String endTime,int lastId) throws SQLException;
}

