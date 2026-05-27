/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.FreezeMoneyMessage
 *  com.vinplay.vbee.common.messages.MoneyMessageInGame
 */
package com.vinplay.vbee.dao;

import com.vinplay.vbee.common.messages.FreezeMoneyMessage;
import com.vinplay.vbee.common.messages.MoneyMessageInGame;
import java.sql.SQLException;

public interface MoneyInGameDao {
    public boolean freezeMoneyInGame(FreezeMoneyMessage var1) throws SQLException;

    public boolean restoreMoneyInGame(FreezeMoneyMessage var1) throws SQLException;

    public boolean updateMoneyInGame(MoneyMessageInGame var1) throws SQLException;

    public boolean updateTranferAgent(String var1, int var2, int var3, String var4) throws SQLException;

    public void updateTranferAgentMySQL(String var1, int var2, int var3, String var4) throws SQLException;
}

