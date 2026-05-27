/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.userMission.LogReceivedRewardObj
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.UserDaoImpl;
import com.vinplay.dal.entities.taixiu.DealerProfit;
import com.vinplay.dal.service.DealerProfitService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DealerProfitServiceImpl
implements DealerProfitService {
    @Override
    public boolean addDealerProfit(long phienid, int result, long total_money_tai, long total_money_xiu
            , long total_profit, Date created_time){
        return false;
    }
    @Override
    public ArrayList<DealerProfit> getListDealerProfit(Date startTime, Date endTime){
        return null;
    }
}

