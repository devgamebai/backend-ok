/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.usercore.dao.impl.MoneyInGameDaoImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.FreezeModel
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.cache;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.usercore.dao.impl.MoneyInGameDaoImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.FreezeModel;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class UpdateCacheFreezeProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        String res = "1";
        try {
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            IMap<String, FreezeModel> freezeMap = client.getMap("freeze");
            MoneyInGameDaoImpl dao = new MoneyInGameDaoImpl();
            for (Map.Entry entry : freezeMap.entrySet()) {
                FreezeModel model = (FreezeModel)entry.getValue();
                if (model.getCreateTime() != null) continue;
                model = dao.getFreeze((String)entry.getKey());
                freezeMap.put((String) entry.getKey(), model);
            }
            res = "0";
        }
        catch (Exception client) {
            // empty catch block
        }
        return res;
    }
}

