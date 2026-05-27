/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.userMission.LogReceivedRewardObj
 */
package com.vinplay.dal.service;

import com.vinplay.vbee.common.response.userMission.LogReceivedRewardObj;
import java.sql.SQLException;
import java.util.List;

public interface UserMissionService {
    public List<LogReceivedRewardObj> getLogReceivedReward(String var1, String var2, String var3, String var4, String var5, int var6) throws SQLException;

    public boolean updateUserMission(String var1, String var2, String var3, int var4) throws SQLException;
}

