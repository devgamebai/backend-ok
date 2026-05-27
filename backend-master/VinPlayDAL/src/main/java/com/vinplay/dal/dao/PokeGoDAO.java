/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.minigame.pokego.LSGDPokeGo
 *  com.vinplay.vbee.common.models.minigame.pokego.TopPokeGo
 */
package com.vinplay.dal.dao;

import com.vinplay.vbee.common.models.minigame.pokego.LSGDPokeGo;
import com.vinplay.vbee.common.models.minigame.pokego.TopPokeGo;
import java.util.List;

public interface PokeGoDAO {
    public List<TopPokeGo> getTopPokeGo(int var1, int var2);

    public List<TopPokeGo> getTop(int var1, int var2);

    public int countLSGD(String var1, int var2);

    public List<LSGDPokeGo> getLSGD(String var1, int var2, int var3);

    public long getLastRefenceId();
}

