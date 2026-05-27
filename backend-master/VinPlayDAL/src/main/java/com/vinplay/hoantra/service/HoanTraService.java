/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.HoanTraModel
 */
package com.vinplay.hoantra.service;

import com.vinplay.vbee.common.models.HoanTraModel;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

public interface HoanTraService {
    public List<HoanTraModel> getMoneyHoanTra(Date var1) throws SQLException;

    public List<HoanTraModel> getListHoanTra(Date var1, String var2) throws SQLException;

    public List<HoanTraModel> getListHoanTraHistories(Date var1, String var2, int var3, int var4) throws SQLException;

    public int updateHoanTra(HoanTraModel var1, Boolean var2, String var3) throws SQLException;

    public int deleteHoanTra(Date var1, Boolean var2) throws SQLException;

    public int insertHoanTraList(List<HoanTraModel> var1) throws SQLException;

    public int[] generateAllHoanTra(Date var1) throws SQLException;

    public long countListHoanTraHistories(Date var1, String var2) throws SQLException;

    public long countListHoanTra(Date var1, String var2) throws SQLException;

    public int insertHoanTraHistory(HoanTraModel var1, Boolean var2, String var3) throws SQLException;
}

