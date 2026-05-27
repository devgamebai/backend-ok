/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.Debug
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 */
package com.vinplay.game.XocDia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.game.XocDia.XocDiaHistoryItem;
import com.vinplay.game.XocDia.XocDiaHistoryModel;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;

public class XocDiaSoiCauUtil {
    public static XocDiaHistoryModel getListSoiCau() {
        String key;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap map = client.getMap("cacheTop");
        if (map.containsKey((key = Games.XOC_DIA.getId() + ""))) {
            return (XocDiaHistoryModel)map.get(key);
        }
        XocDiaHistoryModel xocDiaHistoryModel = new XocDiaHistoryModel();
        map.put(key, xocDiaHistoryModel);
        return xocDiaHistoryModel;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static synchronized void addListSoiCau(long refID, byte[] result) {
        XocDiaHistoryItem xocDiaHistoryItem = new XocDiaHistoryItem(refID, result);
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap map = client.getMap("cacheTop");
        String key = Games.XOC_DIA.getId() + "";
        try {
            if (map.containsKey(key)) {
                map.lock(key);
                XocDiaHistoryModel xocDiaHistoryModel = (XocDiaHistoryModel)map.get(key);
                xocDiaHistoryModel.add(xocDiaHistoryItem);
            } else {
                XocDiaHistoryModel xocDiaHistoryModel = new XocDiaHistoryModel();
                xocDiaHistoryModel.add(xocDiaHistoryItem);
                map.put(key, xocDiaHistoryModel);
            }
        }
        catch (Exception e) {
            LoggerFactory.getLogger(XocDiaSoiCauUtil.class).debug("Error", e);
        }
        finally {
            map.unlock(key);
        }
    }
}

