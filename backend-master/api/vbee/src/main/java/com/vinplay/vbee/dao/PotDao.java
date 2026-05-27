/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.NoHuMessage
 *  com.vinplay.vbee.common.messages.PotMessage
 */
package com.vinplay.vbee.dao;

import com.vinplay.vbee.common.messages.NoHuMessage;
import com.vinplay.vbee.common.messages.PotMessage;
import java.sql.SQLException;

public interface PotDao {
    public boolean addMoneyPot(PotMessage var1) throws SQLException;

    public boolean nohu(NoHuMessage var1) throws SQLException;

    public long getPotValue(String var1) throws SQLException;

    public boolean logHuGameBai(PotMessage var1);
}

