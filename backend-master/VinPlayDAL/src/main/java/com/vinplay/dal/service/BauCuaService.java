/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.minigame.TopWin
 *  com.vinplay.vbee.common.models.minigame.baucua.ResultBauCua
 *  com.vinplay.vbee.common.models.minigame.baucua.ToiChonCa
 *  com.vinplay.vbee.common.models.minigame.baucua.TransactionBauCua
 *  com.vinplay.vbee.common.models.minigame.baucua.TransactionBauCuaDetail
 */
package com.vinplay.dal.service;

import com.vinplay.vbee.common.models.minigame.TopWin;
import com.vinplay.vbee.common.models.minigame.baucua.ResultBauCua;
import com.vinplay.vbee.common.models.minigame.baucua.ToiChonCa;
import com.vinplay.vbee.common.models.minigame.baucua.TransactionBauCua;
import com.vinplay.vbee.common.models.minigame.baucua.TransactionBauCuaDetail;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

public interface BauCuaService {
    public void saveTransactionBauCuaDetail(TransactionBauCuaDetail var1) throws IOException, TimeoutException, InterruptedException;

    public void saveTransactionBauCua(TransactionBauCua var1) throws IOException, TimeoutException, InterruptedException;

    public void saveResultBauCua(ResultBauCua var1) throws IOException, TimeoutException, InterruptedException;

    public List<TransactionBauCua> getLSGDBauCua(String var1, int var2, byte var3);

    public int countLSGDBauCua(String var1, byte var2);

    public ResultBauCua getPhienBauCua(long var1);

    public List<TopWin> getTopBauCua(byte var1, String var2, String var3);

    public void calculteToiChonCa(byte[] var1, List<TransactionBauCua> var2) throws IOException, TimeoutException, InterruptedException;

    public List<ResultBauCua> getLichSuPhien(int var1, byte var2);

    public List<ToiChonCa> getTopToiChonCa(String var1, String var2);

    public void updateAllTop();

    void setKetQuaBauCua(short[] ketQua);
    short[] getKetQuaBauCua();
    short[] layKetQuaBauCua();
}

