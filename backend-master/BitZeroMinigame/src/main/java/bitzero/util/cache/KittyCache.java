/*
 * Decompiled with CFR 0_116.
 */
package bitzero.util.cache;

import bitzero.util.cache.KCache;
import bitzero.util.common.business.CommonHandle;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class KittyCache<K, V>
implements KCache<K, V> {
    private Map<K, KittyCache<K, V>> cache;
    private Queue<K> queue;
    private int maxSize;
    private AtomicInteger cacheSize = new AtomicInteger();

    public KittyCache(int maxSize) {
        this.maxSize = maxSize;
        this.cache = new ConcurrentHashMap<K, KittyCache<K, V>>(maxSize);
        this.queue = new ConcurrentLinkedQueue<K>();
    }

    @Override
    public V get(K key) {
        if (key == null) {
            return null;
        }
        CacheEntry entry = (CacheEntry)((Object)this.cache.get(key));
        if (entry == null) {
            return null;
        }
        long timestamp = entry.getExpireBy();
        if (timestamp != -1 && System.currentTimeMillis() > timestamp) {
            this.remove(key);
            return null;
        }
        return (V)entry.getEntry();
    }

    @Override
    public V removeAndGet(K key) {
        if (key == null) {
            return null;
        }
        CacheEntry entry = (CacheEntry)((Object)this.cache.get(key));
        if (entry != null) {
            this.cacheSize.decrementAndGet();
            return (V)((CacheEntry)((Object)this.cache.remove(key))).getEntry();
        }
        return null;
    }

    @Override
    public void put(K key, V value, int secondsToLive) {
        if (key == null) {
            return;
        }
        if (value == null) {
            return;
        }
        long expireBy = secondsToLive != -1 ? System.currentTimeMillis() + (long)(secondsToLive * 1000) : (long)secondsToLive;
        boolean exists = this.cache.containsKey(key);
        if (!exists) {
            this.cacheSize.incrementAndGet();
            if (this.cacheSize.get() > this.maxSize) {
                this.checkExpireAllKey();
            }
        }
        //this.cache.put(key, (KittyCache<K, CacheEntry<V>>)((Object)new CacheEntry<V>(expireBy, value)));
        this.cache.put(key, (KittyCache<K, V>)((Object)new CacheEntry<V>(expireBy, value)));
    }

    public void checkExpireAllKey() {
        try {
            System.out.println("checkExpireAllKey : cacheSize :" + this.cacheSize.get() + " cache : " + this.cache.size() + " queue: " + this.queue.size());
            Set<K> keySet = this.cache.keySet();
            for (K key : keySet) {
                this.get(key);
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            System.out.println("Time :" + dateFormat.format(Calendar.getInstance().getTime()) + " End checkExpireAllKey : cacheSize :" + this.cacheSize.get() + "cache : " + this.cache.size() + " queue: " + this.queue.size());
        }
        catch (Exception e) {
            CommonHandle.writeErrLog(e);
        }
    }

    @Override
    public boolean remove(K key) {
        return this.removeAndGet(key) != null;
    }

    @Override
    public int size() {
        return this.cacheSize.get();
    }

    @Override
    public Map<K, V> getAll(Collection<K> collection) {
        HashMap<K, V> ret = new HashMap<K, V>();
        for (K o : collection) {
            ret.put(o, this.get(o));
        }
        return ret;
    }

    @Override
    public void clear() {
        this.cache.clear();
    }

    public int mapSize() {
        return this.cache.size();
    }

    public int queueSize() {
        return this.queue.size();
    }

    private class CacheEntry<V> {
        private long expireBy;
        private V entry;

        public CacheEntry(long expireBy, V entry) {
            this.expireBy = expireBy;
            this.entry = entry;
        }

        public long getExpireBy() {
            return this.expireBy;
        }

        public V getEntry() {
            return this.entry;
        }
    }

}

