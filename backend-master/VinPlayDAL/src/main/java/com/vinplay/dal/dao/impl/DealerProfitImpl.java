/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.Block
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.vippoint.UserVPEventModel
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.response.userMission.LogReceivedRewardObj
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 *  com.vinplay.vbee.common.utils.UserUtil
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dal.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.dao.DealerProfitDao;
import com.vinplay.dal.dao.UserDao;
import com.vinplay.dal.entities.taixiu.DealerProfit;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.vippoint.UserVPEventModel;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.response.userMission.LogReceivedRewardObj;
import com.vinplay.vbee.common.utils.CommonUtils;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import com.vinplay.vbee.common.utils.UserUtil;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class DealerProfitImpl
        implements DealerProfitDao {

    @Override
    public boolean addDealerProfit(long phienid, int result, long total_money_tai, long total_money_xiu
            , long total_profit, long last_balance) throws SQLException {
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
        try{
            String sql = " INSERT INTO vinplay.report_dealer_by_day  (phienid, result, total_money_tai" +
                    ", total_money_xiu, total_profit, last_balance, created_time)  VALUES  (?, ?, ?, ?, ?, ?, now()) ";
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setLong(1, phienid);
            stm.setInt(2, result);
            stm.setLong(3, total_money_tai);
            stm.setLong(4, total_money_xiu);
            stm.setLong(5, total_profit);
            stm.setLong(6, last_balance);
            stm.executeUpdate();
            stm.close();
        }
        finally {
            if (conn != null) {
                conn.close();
            }
        }
        return true;
    }

    @Override
    public ArrayList<DealerProfit> getListDealerProfit(String startTime, String endTime,int lastId) throws SQLException {
        ArrayList<DealerProfit> lstDealerProfits = new ArrayList<>();
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
        try{
            String sql = "call vinplay.get_report_dealer(?,?,?)";
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setString(1,startTime);
            stm.setString(2,endTime);
            stm.setInt(3,lastId);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                long phienid = rs.getLong("phienid");
                int result = rs.getInt("result");
                long total_money_tai = rs.getLong("total_money_tai");
                long total_money_xiu = rs.getLong("total_money_xiu");
                long total_profit = rs.getLong("total_profit");
                long last_balance = rs.getLong("last_balance");
                Timestamp date = rs.getTimestamp("created_time");
                String created_time = CommonUtils.convertTimestampToString((java.util.Date)date);

                lstDealerProfits.add(new DealerProfit(id,phienid, result, total_money_tai, total_money_xiu, total_profit, last_balance, created_time));
            }
            rs.close();
            stm.close();
        }
        finally {
            if (conn != null) {
                conn.close();
            }
        }
        return lstDealerProfits;
    }
}

