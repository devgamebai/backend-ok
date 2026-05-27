/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.MoneyMessageInMinigame
 *  com.vinplay.vbee.common.messages.SafeMoneyMessage
 *  com.vinplay.vbee.common.messages.VippointMessage
 *  com.vinplay.vbee.common.messages.userMission.LogReceivedRewardMissionMessage
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.userMission.MissionObj
 *  com.vinplay.vbee.common.models.userMission.UserMissionCacheModel
 */
package com.vinplay.vbee.dao;

import com.vinplay.vbee.common.messages.MoneyMessageInMinigame;
import com.vinplay.vbee.common.messages.SafeMoneyMessage;
import com.vinplay.vbee.common.messages.VippointMessage;
import com.vinplay.vbee.common.messages.userMission.LogReceivedRewardMissionMessage;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.userMission.MissionObj;
import com.vinplay.vbee.common.models.userMission.UserMissionCacheModel;
import java.sql.SQLException;
import java.util.List;

public interface UserDao {
    public List<Long> getMoneyUser(int var1, String var2) throws SQLException;

    public long getFreezeMoney(String var1) throws SQLException;

    public long getSafeMoney(int var1) throws SQLException;

    public boolean safeMoney(SafeMoneyMessage var1) throws SQLException;

    public boolean updateMoney(MoneyMessageInMinigame var1, int var2) throws SQLException;

    public boolean updateVP(VippointMessage var1) throws SQLException;

    public boolean updateUserMission(String var1, String var2, String var3, int var4) throws SQLException;

    public UserMissionCacheModel getUserMission(String var1, String var2) throws Exception;

    public void logReceivedRewardMission(LogReceivedRewardMissionMessage var1) throws Exception;

    public UserModel getUserByNickName(String var1) throws SQLException;

    public void insertUserMission(String var1, MissionObj var2, UserModel var3) throws SQLException;

    public UserMissionCacheModel initUserMission(String var1, String var2, String var3, List<String> var4) throws Exception;

    public long getFeeUser(String var1, String var2) throws Exception;
}

