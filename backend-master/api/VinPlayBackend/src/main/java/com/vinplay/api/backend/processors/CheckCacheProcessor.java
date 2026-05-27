/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import javax.servlet.http.HttpServletRequest;

public class CheckCacheProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String cacheName = request.getParameter("cn");
        String key = request.getParameter("k");
        String delete = request.getParameter("delete");
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap map = client.getMap(cacheName);
        if (map != null) {
            if (map.containsKey((Object)key)) {
                Object obj = map.get((Object)key);
                if (delete != null && delete.equals("1")) {
                    map.remove((Object)key);
                }
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    return mapper.writeValueAsString(obj);
                }
                catch (JsonProcessingException e) {
                    return "{\"success\":false,\"errorCode\":\"" + e.getMessage() + "\"}";
                }
            }
            return "KEY " + key + " DOESN'T EXIST";
        }
        return "CACHE NAME " + cacheName + " DOESN'T EXIST";
    }
}

