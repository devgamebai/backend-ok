/*
 * Decompiled with CFR 0.152.
 */
package com.gamebase.dao;

import com.gamebase.entities.MissionStatus;
import com.gamebase.entities.UserMission;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public interface UserMissionDao {
    public boolean createUserMission(UserMission var1) throws SQLException;

    public boolean createListUserMission(List<UserMission> var1) throws SQLException;

    public boolean updateUserMission(UserMission var1) throws SQLException;

    public boolean deleteUserMission(int var1) throws SQLException;

    public void deleteMissionBeforeDay(Date var1) throws SQLException;

    public UserMission getMission(String var1, String var2) throws SQLException;

    public List<UserMission> getAllMission(String var1, List<MissionStatus> var2) throws SQLException;

    public List<UserMission> getListMissionByEvent(String var1, int var2, List<MissionStatus> var3) throws SQLException;

    public List<String> checkListUID(String var1, List<String> var2) throws SQLException;
}

