/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.report;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import javax.servlet.http.HttpServletRequest;

public class ResetErrorDvtProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        String res = "ERROR";
        HttpServletRequest request = (HttpServletRequest)param.get();
        String action = request.getParameter("ac");
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap mapDvt = client.getMap("cacheDvt");
        if (mapDvt != null && mapDvt.size() > 0) {
            if (mapDvt.containsKey((Object)action)) {
                mapDvt.put((Object)action, (Object)0);
                res = "SUCCESS";
            } else {
                res = "ACTION NO EXIST";
            }
        } else {
            res = "MAP IS EMTY";
        }
        return res;
    }
}

