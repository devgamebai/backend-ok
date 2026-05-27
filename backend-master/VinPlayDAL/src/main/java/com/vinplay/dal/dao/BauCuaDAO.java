/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.minigame.TopWin
 *  com.vinplay.vbee.common.models.minigame.baucua.ResultBauCua
 *  com.vinplay.vbee.common.models.minigame.baucua.ToiChonCa
 *  com.vinplay.vbee.common.models.minigame.baucua.TransactionBauCua
 */
package com.vinplay.dal.dao;

import com.vinplay.vbee.common.models.minigame.TopWin;
import com.vinplay.vbee.common.models.minigame.baucua.ResultBauCua;
import com.vinplay.vbee.common.models.minigame.baucua.ToiChonCa;
import com.vinplay.vbee.common.models.minigame.baucua.TransactionBauCua;
import java.util.List;

public interface BauCuaDAO {
    public List<TransactionBauCua> getLSGDBauCua(String var1, int var2, byte var3);

    public int countLSGDBauCua(String var1, byte var2);

    public ResultBauCua getPhienBauCua(long var1);

    public List<TopWin> getTopBauCua(byte var1, String var2, String var3);

    public List<ResultBauCua> getLichSuPhien(int var1, byte var2);

    public List<ToiChonCa> getTopToiChonCa(String var1, String var2);

    public short getHighScore(String var1);
}

