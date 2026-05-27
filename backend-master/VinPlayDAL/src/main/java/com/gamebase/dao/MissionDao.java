/*
 * Decompiled with CFR 0.152.
 */
package com.gamebase.dao;

import com.gamebase.entities.Mission;
import java.sql.SQLException;
import java.util.List;

public interface MissionDao {
    public void createMission(Mission var1) throws SQLException;

    public void updateMission(Mission var1) throws SQLException;

    public void deleteMission(String var1) throws SQLException;

    public Mission getMission(String var1) throws SQLException;

    public List<Mission> getListMission() throws SQLException;

    public List<Mission> getPartitionMission(String var1, int var2, int var3, int var4, int var5, int var6, String var7, String var8, String var9) throws SQLException;

    public int getCountMission(String var1, int var2, int var3, int var4, String var5, String var6, String var7) throws SQLException;
}

