/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.PotModel
 */
package com.vinplay.dal.dao;

import com.vinplay.vbee.common.models.PotModel;
import java.sql.SQLException;
import java.util.List;

public interface PotDao {
    public List<PotModel> getAll() throws SQLException;
}

