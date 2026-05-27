/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.minigame.galaxy.LSGDGalaxy
 *  com.vinplay.vbee.common.models.minigame.galaxy.TopGalaxy
 */
package com.vinplay.dal.dao;

import com.vinplay.vbee.common.models.minigame.galaxy.LSGDGalaxy;
import com.vinplay.vbee.common.models.minigame.galaxy.TopGalaxy;
import java.util.List;

public interface GalaxyDAO {
    public List<TopGalaxy> getTopGalaxy(int var1, int var2);

    public List<TopGalaxy> getTop(int var1, int var2);

    public int countLSGD(String var1, int var2);

    public List<LSGDGalaxy> getLSGD(String var1, int var2, int var3);

    public long getLastRefenceId();
}

