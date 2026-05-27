/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.vippoint.EventVPBonusModel
 *  com.vinplay.vbee.common.models.vippoint.UserVPEventModel
 */
package com.vinplay.usercore.dao;

import com.vinplay.vbee.common.models.vippoint.EventVPBonusModel;
import com.vinplay.vbee.common.models.vippoint.UserVPEventModel;
import com.vinplay.vippoint.entiies.EventVPMapModel;
import com.vinplay.vippoint.entiies.EventVPTopIntelModel;
import com.vinplay.vippoint.entiies.EventVPTopStrongModel;
import com.vinplay.vippoint.entiies.EventVPTopVipModel;
import java.sql.SQLException;
import java.util.List;

public interface VippointDao {
    public List<EventVPBonusModel> getEventVPBonus() throws SQLException;

    public boolean updateEventVPBonus(int var1) throws SQLException;

    public int getVPEvent(String var1) throws SQLException;

    public int getVPEventReal(String var1) throws SQLException;

    public List<EventVPMapModel> getEventMaps() throws SQLException;

    public List<EventVPTopIntelModel> getEventIntel() throws SQLException;

    public List<EventVPTopStrongModel> getEventStrong() throws SQLException;

    public List<EventVPTopVipModel> getEventVips() throws SQLException;

    public UserVPEventModel getUserVPByNickName(String var1) throws SQLException;

    public int getEventIntelIndex(int var1, int var2, int var3) throws SQLException;

    public int getEventStrongIndex(int var1, int var2, int var3) throws SQLException;

    public int getEventVipsIndex(int var1) throws SQLException;

    public boolean logVippointEvent(String var1, int var2, int var3, int var4);

    public int getNumRunInDay(String var1, int var2) throws SQLException;

    public boolean updateNumInDay(String var1, int var2) throws SQLException;

    public boolean resetEvent() throws SQLException;
}

