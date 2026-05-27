/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors.cache;

import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class UpdateVippointProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        String res = "1";
        try {
            IMap<String, UserCacheModel> userMap = HazelcastClientFactory.getInstance().getMap("users");
            for (Map.Entry entry : userMap.entrySet()) {
                UserCacheModel user = (UserCacheModel)entry.getValue();
                if (user.getVippoint() <= 0 && user.getVippointSave() <= 0 && user.getMoneyVP() <= 0) continue;
                try {
                    userMap.lock((String)entry.getKey());
                    UserCacheModel userCache = userMap.get(entry.getKey());
                    userCache.setVippoint(0);
                    userCache.setVippointSave(0);
                    userCache.setMoneyVP(0);
                    userMap.put((String)entry.getKey(), userCache);
                }
                catch (Exception e) {
                    logger.debug(e);
                    continue;
                }
                try {
                    userMap.unlock((String)entry.getKey());
                }
                catch (Exception e) {}
            }
            res = "0";
        }
        catch (Exception e2) {
            logger.debug(e2);
        }
        return res;
    }
}

