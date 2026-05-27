/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.PotModel
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.utils.UserUtil
 */
package com.vinplay.dal.dao.impl;

import com.vinplay.dal.dao.PotDao;
import com.vinplay.vbee.common.models.PotModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.utils.UserUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PotDaoImpl
implements PotDao {
    @Override
    public List<PotModel> getAll() throws SQLException {
        ArrayList<PotModel> listModel = new ArrayList<PotModel>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");){
            PotModel model = null;
            String sql = "SELECT * FROM hu_game_bai";
            PreparedStatement stm = conn.prepareStatement("SELECT * FROM hu_game_bai");
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                model = UserUtil.parseResultSetToPotModel((ResultSet)rs);
                listModel.add(model);
            }
            rs.close();
            stm.close();
        }
        return listModel;
    }
}

