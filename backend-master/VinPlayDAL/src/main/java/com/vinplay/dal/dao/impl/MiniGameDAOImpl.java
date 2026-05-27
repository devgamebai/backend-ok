/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.response.BonusFundResponse
 */
package com.vinplay.dal.dao.impl;

import com.vinplay.dal.dao.MiniGameDAO;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.response.BonusFundResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MiniGameDAOImpl
implements MiniGameDAO {
    @Override
    public long getReferenceId(int gameId) throws SQLException {
        long referenceId = -1L;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");){
            String sql = "SELECT value FROM `references` WHERE game_id=" + gameId;
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                referenceId = rs.getLong("value");
            }
            rs.close();
            stmt.close();
        }
        return referenceId;
    }

    @Override
    public long getPot(String potName) throws SQLException {
        long value = 0L;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");){
            String sql = "SELECT value FROM minigame_pots WHERE minigame_pots.pot_name = '" + potName + "'";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                value = rs.getLong("value");
            }
            rs.close();
            stmt.close();
        }
        return value;
    }

    @Override
    public long[] getPots(String potName) throws SQLException {
        ArrayList<Long> result = new ArrayList<Long>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");){
            String sql = "SELECT value FROM minigame_pots WHERE minigame_pots.pot_name like '" + potName + "%'";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(rs.getLong("value"));
            }
            rs.close();
            stmt.close();
        }
        long[] arr = new long[result.size()];
        for (int i = 0; i < result.size(); ++i) {
            arr[i] = (Long)result.get(i);
        }
        return arr;
    }

    @Override
    public long getFund(String fundName) throws SQLException {
        long value = 0L;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");){
            String sql = "SELECT value FROM minigame_funds WHERE minigame_funds.fund_name = '" + fundName + "'";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                value = rs.getLong("value");
            }
            rs.close();
            stmt.close();
        }
        return value;
    }

    @Override
    public long[] getFunds(String fundName) throws SQLException {
        ArrayList<Long> result = new ArrayList<Long>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");){
            String sql = "SELECT value FROM minigame_funds WHERE minigame_funds.fund_name like '" + fundName + "%'";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(rs.getLong("value"));
            }
            rs.close();
            stmt.close();
        }
        long[] arr = new long[result.size()];
        for (int i = 0; i < result.size(); ++i) {
            arr[i] = (Long)result.get(i);
        }
        return arr;
    }

    @Override
    public List<BonusFundResponse> getFunds() throws SQLException {
        ArrayList<BonusFundResponse> results = new ArrayList<BonusFundResponse>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");){
            String sql = "SELECT fund_name, value FROM vinplay_minigame.minigame_funds";
            PreparedStatement stmt = conn.prepareStatement("SELECT fund_name, value FROM vinplay_minigame.minigame_funds");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                BonusFundResponse bonusFund = new BonusFundResponse();
                bonusFund.name = rs.getString("fund_name");
                bonusFund.value = rs.getLong("value");
                results.add(bonusFund);
            }
            rs.close();
            stmt.close();
        }
        return results;
    }

    @Override
    public boolean saveReferenceId(long newReferenceId, int gameId) throws SQLException {
        boolean success = false;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");){
            String sql = "UPDATE `references` SET value=" + newReferenceId + " WHERE game_id=" + gameId;
            PreparedStatement stmt = conn.prepareStatement(sql);
            success = stmt.execute();
            stmt.close();
        }
        return success;
    }
}

