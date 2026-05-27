/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 */
package com.vinplay.usercore.service.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.usercore.service.CacheService;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class CacheServiceImpl
implements CacheService {
    @Override
    public void setValue(String key, String value) {
        HazelcastInstance instance = HazelcastClientFactory.getInstance();
        IMap map = instance.getMap("cacheConfig");
        map.put(key, value);
    }

    @Override
    public void setValue(String key, int value) {
        HazelcastInstance instance = HazelcastClientFactory.getInstance();
        IMap map = instance.getMap("cacheConfig");
        map.put(key, String.valueOf(value));
    }

    @Override
    public String getValueStr(String key) throws KeyNotFoundException {
        HazelcastInstance instance = HazelcastClientFactory.getInstance();
        IMap map = instance.getMap("cacheConfig");
        if (map.containsKey(key)) {
            return (String)map.get(key);
        }
        throw new KeyNotFoundException();
    }

    @Override
    public int getValueInt(String key) throws KeyNotFoundException, NumberFormatException {
        HazelcastInstance instance = HazelcastClientFactory.getInstance();
        IMap map = instance.getMap("cacheConfig");
        if (map.containsKey(key)) {
            return Integer.parseInt((String)map.get(key));
        }
        throw new KeyNotFoundException();
    }

    @Override
    public boolean removeKey(String key) throws KeyNotFoundException {
        HazelcastInstance instance = HazelcastClientFactory.getInstance();
        IMap map = instance.getMap("cacheConfig");
        if (map.containsKey(key)) {
            map.remove(key);
            return true;
        }
        throw new KeyNotFoundException();
    }

    @Override
    public void setObject(String key, Object obj) {
        HazelcastInstance instance = HazelcastClientFactory.getInstance();
        IMap map = instance.getMap("cacheGameBai");
        map.put(key, obj);
    }

    @Override
    public Object getObject(String key) throws KeyNotFoundException {
        HazelcastInstance instance = HazelcastClientFactory.getInstance();
        IMap map = instance.getMap("cacheGameBai");
        if (map.containsKey(key)) {
            return map.get(key);
        }
        throw new KeyNotFoundException();
    }

    @Override
    public Object removeObject(String key) throws KeyNotFoundException {
        HazelcastInstance instance = HazelcastClientFactory.getInstance();
        IMap map = instance.getMap("cacheGameBai");
        if (map.containsKey(key)) {
            return map.remove(key);
        }
        throw new KeyNotFoundException();
    }

    @Override
    public Map<String, Object> getBulk(Set<String> keys) {
        HazelcastInstance instance = HazelcastClientFactory.getInstance();
        IMap map = instance.getMap("cacheGameBai");
        return map.getAll(keys);
    }

    @Override
    public void setObject(String key, int expireTime, Object obj) {
        HazelcastInstance instance = HazelcastClientFactory.getInstance();
        IMap map = instance.getMap("cacheGameBai");
        map.put(key, obj, (long)expireTime, TimeUnit.SECONDS);
    }
}

