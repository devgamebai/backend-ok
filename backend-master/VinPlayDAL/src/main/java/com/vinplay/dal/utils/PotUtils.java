/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cache.CacheFactory
 *  com.vinplay.vbee.common.cache.DistCache
 *  com.vinplay.vbee.common.models.PotModel
 */
package com.vinplay.dal.utils;

import com.vinplay.dal.dao.impl.PotDaoImpl;
import com.vinplay.vbee.common.cache.CacheFactory;
import com.vinplay.vbee.common.cache.DistCache;
import com.vinplay.vbee.common.models.PotModel;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class PotUtils {
    public static void init() throws SQLException, IOException {
        // 2026-05-07 Wave 2 batch I (SUN-1248): huGameBai moved to Redis via DistCache.
        DistCache<String, PotModel> potMap = CacheFactory.get("huGameBai", PotModel.class);
        PotDaoImpl dao = new PotDaoImpl();
        List<PotModel> listModel = dao.getAll();
        for (PotModel model : listModel) {
            if (potMap.containsKey(model.getPotName())) continue;
            potMap.put(model.getPotName(), model);
        }
    }
}

