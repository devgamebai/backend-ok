/*
 * Decompiled with CFR 0.152.
 */
package com.gamebase.service;

import com.gamebase.entities.MissionStatus;
import com.gamebase.entities.UserMission;
import java.util.List;

public interface UserMissionService {
    public UserMission getMission(String var1, String var2) throws Exception;

    public void updateMission(UserMission var1) throws Exception;

    public void setStatusMission(String var1, String var2, MissionStatus var3) throws Exception;

    public List<UserMission> getAllMission(String var1) throws Exception;

    public List<UserMission> getListMissionByEvent(String var1, int var2) throws Exception;

    public List<UserMission> createMissionForUser(String var1) throws Exception;

    public void listenChangeMission() throws Exception;
}

