/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.minigame.LotteryMessage
 */
package com.vinplay.dal.dao;

import com.vinplay.vbee.common.messages.minigame.LotteryMessage;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public interface LoDeDao {
    public void saveTransactionLode(LotteryMessage var1) throws SQLException;

    public void updatePrize(long var1, long var3);

    public List<LotteryMessage> getRecordsWithNullPrizeBefore1830Today(Date var1);

    public List<LotteryMessage> getRowsByNickname(String var1);

    public void saveToDatabase(String var1, Date var2);

    public String getLatestResult(Date var1);

    public List<String> getListOfResultsByDateRange();

    public List<LotteryMessage> search(String var1, String var2, String var3, String var4, String var5, int var6, int var7) throws SQLException;

    public long count(String var1, String var2, String var3, String var4, String var5) throws SQLException;
}

