/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.FreezeModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 */
package com.vinplay.usercore.dao;

import com.vinplay.usercore.entities.LogTransferAgentModel;
import com.vinplay.vbee.common.models.FreezeModel;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import java.sql.SQLException;
import java.util.List;

public interface MoneyInGameDao {
    public List<FreezeModel> getAllFreeze() throws SQLException;

    public FreezeModel getFreeze(String var1) throws SQLException;

    public boolean updateVippoint(String var1, int var2, int var3) throws SQLException;

    public boolean updateVippointAgent(String var1, int var2, int var3, int var4) throws SQLException;

    public boolean restoreAllGame(List<String> var1) throws SQLException;

    public UserCacheModel getUserByNickName(String var1) throws SQLException;

    public List<FreezeModel> getListFreezeMoneyAgentTranfer(String var1, String var2, String var3, String var4, String var5, int var6, String var7) throws SQLException;

    public boolean updateSafeMoney(long var1, long var3) throws SQLException;

    public String getNickNameFreezeMoneyAgentTranferBySessionId(String var1) throws SQLException;

    public FreezeModel getFreezeMoneyAgentTranferBySessionId(String var1) throws SQLException;

    public LogTransferAgentModel getMoneyAgentTranferBySessionId(String var1) throws SQLException;
}

