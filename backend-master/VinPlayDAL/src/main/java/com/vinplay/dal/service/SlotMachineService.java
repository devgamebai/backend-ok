/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.cache.SlotFreeDaily
 *  com.vinplay.vbee.common.models.minigame.pokego.LSGDPokeGo
 *  com.vinplay.vbee.common.models.minigame.pokego.TopPokeGo
 *  com.vinplay.vbee.common.models.slot.NoHuModel
 *  com.vinplay.vbee.common.models.slot.SlotFreeSpin
 */
package com.vinplay.dal.service;

import com.vinplay.vbee.common.models.cache.SlotFreeDaily;
import com.vinplay.vbee.common.models.minigame.pokego.LSGDPokeGo;
import com.vinplay.vbee.common.models.minigame.pokego.TopPokeGo;
import com.vinplay.vbee.common.models.slot.NoHuModel;
import com.vinplay.vbee.common.models.slot.SlotFreeSpin;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

public interface SlotMachineService {
    public void logKhoBau(long var1, String var3, long var4, String var6, String var7, String var8, short var9, long var10, String var12) throws IOException, TimeoutException, InterruptedException;

    public void logAvengers(long var1, String var3, long var4, String var6, String var7, String var8, short var9, long var10, String var12) throws IOException, TimeoutException, InterruptedException;

    public void logMyNhanNgu(long var1, String var3, long var4, String var6, String var7, String var8, short var9, long var10, String var12) throws IOException, TimeoutException, InterruptedException;

    public void logNuDiepVien(long var1, String var3, long var4, String var6, String var7, String var8, short var9, long var10, String var12) throws IOException, TimeoutException, InterruptedException;

    public void logVQV(long var1, String var3, long var4, String var6, String var7, String var8, short var9, long var10, String var12) throws IOException, TimeoutException, InterruptedException;

    public int countLSDG(String var1, String var2);

    public List<LSGDPokeGo> getLSGD(String var1, String var2, int var3);

    public void addTop(String var1, String var2, int var3, long var4, String var6, int var7) throws IOException, TimeoutException, InterruptedException;

    public List<TopPokeGo> getTopSlotMachine(String var1, int var2);

    public long getLastReferenceId(String var1);

    public SlotFreeSpin updateLuotQuaySlotFree(String var1, String var2);

    public void setLuotQuayFreeSlot(String var1, String var2, String var3, int var4, int var5);

    public SlotFreeSpin getLuotQuayFreeSlot(String var1, String var2);

    public void setItemsWild(String var1, String var2, String var3);

    public int getPrizes(String var1, String var2);

    public void addPrizes(String var1, String var2, int var3);

    public SlotFreeDaily getLuotQuayFreeDaily(String var1, String var2, int var3);

    public boolean updateLuotQuayFreeDaily(String var1, String var2, int var3);

    public void logNoHu(long var1, String var3, String var4, int var5, String var6, String var7, String var8, String var9, long var10, short var12, String var13);

    public List<NoHuModel> getLogNoHu(String var1, int var2);

    public void logChiemtinh(long var1, String var3, long var4, String var6, String var7, String var8, short var9, long var10, String var12) throws IOException, TimeoutException, InterruptedException;

    public void logThanDen(long var1, String var3, long var4, String var6, String var7, String var8, short var9, long var10, String var12) throws IOException, TimeoutException, InterruptedException;

    public void logRollRoye(long var1, String var3, long var4, String var6, String var7, String var8, short var9, long var10, String var12) throws IOException, TimeoutException, InterruptedException;

    public void logBikini(long var1, String var3, long var4, String var6, String var7, String var8, short var9, long var10, String var12) throws IOException, TimeoutException, InterruptedException;

    public void logBenley(long var1, String var3, long var4, String var6, String var7, String var8, short var9, long var10, String var12) throws IOException, TimeoutException, InterruptedException;

    public void logAudition(long var1, String var3, long var4, String var6, String var7, String var8, short var9, long var10, String var12) throws IOException, TimeoutException, InterruptedException;

    public void logTamHung(long var1, String var3, long var4, String var6, String var7, String var8, short var9, long var10, String var12) throws IOException, TimeoutException, InterruptedException;

    public void logSpartan(long var1, String var3, long var4, String var6, String var7, String var8, short var9, long var10, String var12) throws IOException, TimeoutException, InterruptedException;

    public void logRangeRover(long var1, String var3, long var4, String var6, String var7, String var8, short var9, long var10, String var12) throws IOException, TimeoutException, InterruptedException;

    public void logSlot(String gameName, long var1, String var3, long var4, String var6, String var7, String var8, short var9, long var10, String var12) throws IOException, TimeoutException, InterruptedException;

    /** Jackpot force-win management — used by minigame rooms (MiniPoker, Galaxy, Candy) */
    public boolean isSetJackpotForUser(String nickname, String gameName, int betLevel);

    public void setUserJackpotForGame(String nickname, String gameName, int betLevel, int value);

    public void deleteUserJackpotForGame(String nickname, String gameName, int betLevel);

    public void setLuotQuayFreeDaily(String var1, String var2, int var3);
}

