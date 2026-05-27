/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.LuckyMessage
 */
package com.vinplay.vbee.dao;

import com.vinplay.vbee.common.messages.LuckyMessage;
import java.sql.SQLException;

public interface LuckyDao {
    public boolean saveResultLucky(int var1, String var2, String var3, int var4, int var5, int var6, int var7, int var8) throws SQLException;

    public boolean saveLuckyHistory(LuckyMessage var1);

    public long getLastReferenceId();
}

