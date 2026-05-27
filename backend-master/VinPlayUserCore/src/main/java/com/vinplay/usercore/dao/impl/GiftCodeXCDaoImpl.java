/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.GiftCodeMessage
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 */
package com.vinplay.usercore.dao.impl;

import com.vinplay.usercore.dao.GiftCodeXCDao;
import com.vinplay.vbee.common.messages.GiftCodeMessage;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GiftCodeXCDaoImpl
implements GiftCodeXCDao {
    @Override
    public List<String> loadAllGiftcode() throws SQLException {
        ArrayList<String> results = new ArrayList<String>();
        String sql = " SELECT giftcode  FROM xc_giftcode.tbl_giftcode ";
        Connection conn = ConnectionPool.getInstance().getConnection("mysql_xc_giftcode");
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(" SELECT giftcode  FROM xc_giftcode.tbl_giftcode ");
            rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(rs.getString("giftcode"));
            }
            rs.close();
            stmt.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        finally {
            if (conn != null) {
                conn.close();
            }
        }
        return results;
    }

    @Override
    public void insertGiftcodeStore(GiftCodeMessage msg) throws SQLException {
        String insertGiftcode = " INSERT INTO xc_giftcode.tbl_giftcode  (  \tgiftcode,  \tamount,  \tmoneyType,  \tgiftcodeType,  \tstatus,  \tbatchId,  )  VALUES (?,?,?,?,?,?) ";
        String insertBatch = " INSERT INTO xc_giftcode.tbl_batch  (  \tamount,  \tquantity,  \t`status`,  \treqTime,  \treqAdmin,  )  VALUES (?,?,?,?,?) ";
        PreparedStatement stmt = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysql_xc_giftcode");){
            stmt = conn.prepareStatement(" INSERT INTO xc_giftcode.tbl_batch  (  \tamount,  \tquantity,  \t`status`,  \treqTime,  \treqAdmin,  )  VALUES (?,?,?,?,?) ");
            stmt.setInt(1, Integer.parseInt(msg.getPrice()));
            stmt.setInt(2, msg.getQuantity());
            stmt.setInt(3, 0);
            stmt.setString(4, DateTimeUtils.getCurrentTime((String)"yyyy-MM-dd HH:mm:ss"));
            stmt.setString(5, msg.getReqAdmin());
            stmt.executeUpdate();
        }
    }
}

