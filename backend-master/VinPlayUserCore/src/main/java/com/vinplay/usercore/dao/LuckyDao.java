/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.cache.SlotFreeDaily
 */
package com.vinplay.usercore.dao;

import com.vinplay.usercore.entities.vqmm.LuckyHistory;
import com.vinplay.usercore.entities.vqmm.LuckyVipHistory;
import com.vinplay.vbee.common.models.cache.SlotFreeDaily;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface LuckyDao {
    public int receiveRotateDaily(int var1, String var2) throws SQLException;

    public List<Integer> getRotateCount(int var1, String var2) throws SQLException;

    public List<LuckyHistory> getLuckyHistory(String var1, int var2);

    public List<LuckyVipHistory> getLuckyVipHistory(String var1, int var2);

    public Map<String, SlotFreeDaily> getAllSlotFree() throws SQLException;

    public boolean saveResultLucky(int var1, String var2, String var3, int var4, int var5, int var6, int var7, int var8, String var9, int var10, int var11) throws SQLException, UnsupportedEncodingException;

    public boolean logLuckyVip(long var1, String var3, String var4, int var5, int var6) throws Exception;

    public int getLuckyVipInMonth(String var1, String var2) throws Exception;

    public long getLuckyVipLastReferenceId();
}

