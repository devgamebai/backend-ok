/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dal.service;

import com.vinplay.dal.entities.minipoker.LSGDMiniPoker;
import com.vinplay.dal.entities.minipoker.VinhDanhMiniPoker;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeoutException;

public interface MiniPokerService {
    public long[] getPotMiniPoker() throws SQLException;

    public long[] getFundMiniPoker() throws SQLException;

    public void updatePotMiniPoker(String var1, long var2) throws IOException, TimeoutException, InterruptedException;

    public void updateFundMiniPoker(String var1, long var2) throws IOException, TimeoutException, InterruptedException;

    public void logMiniPoker(String var1, long var2, short var4, long var5, String var7, long var8, long var10, int var12) throws IOException, TimeoutException, InterruptedException;

    public int countLichSuGiaoDich(String var1, int var2);

    public List<LSGDMiniPoker> getLichSuGiaoDich(String var1, int var2, int var3);

    public List<VinhDanhMiniPoker> getBangVinhDanh(int var1, int var2);
}

