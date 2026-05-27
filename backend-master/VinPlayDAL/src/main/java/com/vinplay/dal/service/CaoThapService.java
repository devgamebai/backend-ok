/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dal.service;

import com.vinplay.dal.entities.caothap.LSGDCaoThap;
import com.vinplay.dal.entities.caothap.TopCaoThap;
import com.vinplay.dal.entities.caothap.VinhDanhCaoThap;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeoutException;

public interface CaoThapService {
    public long[] getPotCaoThap() throws SQLException;

    public long[] getFundCaoThap() throws SQLException;

    public void updatePotCaoThap(String var1, long var2) throws IOException, TimeoutException, InterruptedException;

    public void updateFundCaoThap(String var1, long var2) throws IOException, TimeoutException, InterruptedException;

    public void logCaoThap(long var1, String var3, long var4, short var6, long var7, String var9, long var10, long var12, int var14, short var15, int var16) throws IOException, TimeoutException, InterruptedException;

    public void logCaoThapWin(long var1, String var3, long var4, short var6, long var7, String var9, int var10) throws IOException, TimeoutException, InterruptedException;

    public int countLichSuGiaoDich(String var1, int var2);

    public List<LSGDCaoThap> getLichSuGiaoDich(String var1, int var2, int var3);

    public int countVinhDanh(int var1);

    public List<VinhDanhCaoThap> getBangVinhDanh(int var1, int var2);

    public long getLastReferenceId();

    public List<TopCaoThap> geTopCaoThap(String var1, String var2);

    public void insertBotEvent(String var1, long var2, long var4, String var6);
}

