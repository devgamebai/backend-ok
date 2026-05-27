/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.minigame.pokego.LSGDPokeGo
 *  com.vinplay.vbee.common.models.minigame.pokego.TopPokeGo
 *  com.vinplay.vbee.common.models.slot.NoHuModel
 */
package com.vinplay.dal.dao;

import com.vinplay.vbee.common.models.minigame.pokego.LSGDPokeGo;
import com.vinplay.vbee.common.models.minigame.pokego.TopPokeGo;
import com.vinplay.vbee.common.models.slot.NoHuModel;
import java.sql.SQLException;
import java.util.List;

public interface SlotMachineDAO {
    public List<TopPokeGo> getTopByPage(String var1, int var2);

    public List<TopPokeGo> getTop(String var1, int var2);

    public int countLSGD(String var1, String var2);

    public List<LSGDPokeGo> getLSGD(String var1, String var2, int var3);

    public long getLastRefenceId(String var1);

    public boolean updateSlotFreeDaily(String var1, String var2, int var3, int var4) throws SQLException;

    public List<NoHuModel> getListNoHu(String var1, int var2);
}

