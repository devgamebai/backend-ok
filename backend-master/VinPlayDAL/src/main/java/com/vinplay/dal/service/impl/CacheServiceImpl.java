/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 */
package com.vinplay.dal.service.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.service.CacheService;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class CacheServiceImpl
implements CacheService {
    @Override
    public void setValue(String key, String value) {
        // SUN-1xxx (2026-05-11): retry-once on Hazelcast disconnect. The bare
        // map.put used to throw "Connection reset by peer" during HZ rolling
        // restarts (e.g., heap bumps), and that exception escaped up through
        // TaiXiuModule.startNewRoundTX → killed the ScheduledExecutor task →
        // TaiXiu froze for 12+ minutes until a manual game-minigame restart.
        // The HZ client auto-reconnects within ~50ms after a node bounce, so
        // a single 100ms-delayed retry catches the vast majority of transient
        // failures. Persistent failures still throw — caller decides what to do.
        Exception lastErr = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                HazelcastInstance instance = HazelcastClientFactory.getInstance();
                IMap map = instance.getMap("cacheConfig");
                map.put(key, value);
                return;
            } catch (Exception e) {
                lastErr = e;
                if (attempt < 1) {
                    try { Thread.sleep(100L); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }
        throw new RuntimeException("CacheServiceImpl.setValue(String) failed after retry, key=" + key
                + ": " + (lastErr != null ? lastErr.getMessage() : "unknown"), lastErr);
    }

    @Override
    public void setValue(String key, int value) {
        Exception lastErr = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                HazelcastInstance instance = HazelcastClientFactory.getInstance();
                IMap map = instance.getMap("cacheConfig");
                map.put(key, String.valueOf(value));
                return;
            } catch (Exception e) {
                lastErr = e;
                if (attempt < 1) {
                    try { Thread.sleep(100L); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }
        throw new RuntimeException("CacheServiceImpl.setValue(int) failed after retry, key=" + key
                + ": " + (lastErr != null ? lastErr.getMessage() : "unknown"), lastErr);
    }

    @Override
    public void setValue(String key, long value) {
        Exception lastErr = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                HazelcastInstance instance = HazelcastClientFactory.getInstance();
                IMap map = instance.getMap("cacheConfig");
                map.put(key, String.valueOf(value));
                return;
            } catch (Exception e) {
                lastErr = e;
                if (attempt < 1) {
                    try { Thread.sleep(100L); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }
        throw new RuntimeException("CacheServiceImpl.setValue(long) failed after retry, key=" + key
                + ": " + (lastErr != null ? lastErr.getMessage() : "unknown"), lastErr);
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
    public int getValueIntWithDefault(String key) {
        try{
            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap map = instance.getMap("cacheConfig");
            if (map.containsKey(key)) {
                return Integer.parseInt((String)map.get(key));
            }else{
                return -1;
            }
        }catch (Exception e){
            return -1;
        }
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

    @Override
    public void setValueJp(String key, Long value) {
        this.setValue(key, value);
    }

    @Override
    public Long getValueJP(String key) {
        try {
            String val = this.getValueStr(key);
            return val != null ? Long.parseLong(val) : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }
}

