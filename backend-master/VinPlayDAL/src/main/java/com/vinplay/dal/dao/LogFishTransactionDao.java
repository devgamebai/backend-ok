/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.dao;

import com.vinplay.dal.entities.fish.FishGameRecord;
import com.vinplay.dal.entities.fish.FishTransaction;
import java.util.List;
import java.util.Map;

public interface LogFishTransactionDao {
    public List<Map<String, Object>> search(String var1, String var2, String var3, String var4, int var5);

    public FishGameRecord findItem(String var1) throws Exception;

    public boolean Save(FishTransaction var1) throws Exception;
}

