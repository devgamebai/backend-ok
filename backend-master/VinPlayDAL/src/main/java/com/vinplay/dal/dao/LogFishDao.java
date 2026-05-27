/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.dao;

import com.vinplay.dal.entities.fish.FishGameRecord;
import java.util.Map;

public interface LogFishDao {
    public Map<String, Object> search(String var1, String var2, String var3, int var4);

    public FishGameRecord findItem(Integer var1, Integer var2, String var3, String var4, String var5) throws Exception;

    public boolean Save(FishGameRecord var1) throws Exception;

    public boolean insert(FishGameRecord var1) throws Exception;

    public boolean update(FishGameRecord var1) throws Exception;

    public Long getLastUpdateTime();
}

