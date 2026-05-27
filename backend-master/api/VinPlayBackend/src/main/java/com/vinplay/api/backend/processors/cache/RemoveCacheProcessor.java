/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors.cache;

import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class RemoveCacheProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        String res = "1";
        HttpServletRequest request = (HttpServletRequest)param.get();
        try {
            IMap userMap;
            String nickname = request.getParameter("nn");
            String otp = request.getParameter("otp");
            if (nickname != null && !nickname.isEmpty() && otp != null && otp.equals("FQmMFjF9AKUrpuen") && (userMap = HazelcastClientFactory.getInstance().getMap("users")).containsKey((Object)nickname)) {
                userMap.remove((Object)nickname);
                res = "0";
            }
        }
        catch (Exception e) {
            logger.debug((Object)e);
        }
        return res;
    }
}

