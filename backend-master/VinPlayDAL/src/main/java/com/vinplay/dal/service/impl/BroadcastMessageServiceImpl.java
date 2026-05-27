/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.vinplay.vbee.common.models.BroadcastMsgEntry
 */
package com.vinplay.dal.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.dal.service.BroadcastMessageService;
import com.vinplay.vbee.common.cache.CacheFactory;
import com.vinplay.vbee.common.cache.DistCache;
import com.vinplay.vbee.common.cache.LockHandle;
import com.vinplay.vbee.common.models.BroadcastMsgEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

public class BroadcastMessageServiceImpl
implements BroadcastMessageService {
    private static final Logger logger = Logger.getLogger((String)"rmq");
    private static int MAX_SIZE = 20;
    public static int MIN_MONEY = 10000;
    private static final String KEY_BROADCAST = "keyBroadcast";

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void putMessage(int gameId, String nickname, long money) {
        if (money >= (long)MIN_MONEY) {
            BroadcastMsgEntry newEntry = new BroadcastMsgEntry(gameId, nickname, money);
            DistCache<String, ArrayList> map = CacheFactory.get("cacheBroadcast", ArrayList.class);
            if (map != null && map.containsKey(KEY_BROADCAST)) {
                try (LockHandle h = map.acquireLock(KEY_BROADCAST, 2, TimeUnit.SECONDS)) {
                    if (h == null) {
                        logger.warn("putMessage: lock timeout for KEY_BROADCAST");
                        return;
                    }
                    BroadcastMsgEntry minEntry;
                    @SuppressWarnings("unchecked")
                    List<BroadcastMsgEntry> entries = (List<BroadcastMsgEntry>)map.get(KEY_BROADCAST);
                    if (entries.size() < MAX_SIZE) {
                        this.add(entries, newEntry);
                    } else if (entries.size() == MAX_SIZE && (minEntry = entries.get(entries.size() - 1)).getM() < money) {
                        entries.remove(entries.size() - 1);
                        this.add(entries, newEntry);
                    }
                    map.put(KEY_BROADCAST, new ArrayList<BroadcastMsgEntry>(entries));
                }
                catch (Exception entries) {
                }
            } else if (map != null) {
                ArrayList<BroadcastMsgEntry> entries = new ArrayList<BroadcastMsgEntry>();
                entries.add(newEntry);
                map.putIfAbsent(KEY_BROADCAST, new ArrayList<BroadcastMsgEntry>(entries), 0L, TimeUnit.SECONDS);
            }
        }
    }

    private void add(List<BroadcastMsgEntry> entries, BroadcastMsgEntry newEntry) {
        int index = -1;
        for (int i = 0; i < entries.size(); ++i) {
            BroadcastMsgEntry entry = entries.get(i);
            if (entry.getM() >= newEntry.getM()) continue;
            index = i;
            break;
        }
        if (index > -1) {
            entries.add(index, newEntry);
        } else {
            entries.add(newEntry);
        }
    }

    @Override
    public String toJson() {
        try {
            DistCache<String, ArrayList> map = CacheFactory.get("cacheBroadcast", ArrayList.class);
            @SuppressWarnings("unchecked")
            List<BroadcastMsgEntry> entries = (List<BroadcastMsgEntry>)map.get(KEY_BROADCAST);
            BroadcastMessageServiceImpl this$0 = new BroadcastMessageServiceImpl();
            this$0.getClass();
            BroadcastMsgModel model = this$0.new BroadcastMsgModel();
            model.setEntries(entries);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(model);
        }
        catch (JsonProcessingException e) {
            return "{\"success\":false,\"errorCode\":\"1001\"}";
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void clearMessage() {
        DistCache<String, ArrayList> map = CacheFactory.get("cacheBroadcast", ArrayList.class);
        if (map != null && map.containsKey(KEY_BROADCAST)) {
            try (LockHandle h = map.acquireLock(KEY_BROADCAST, 2, TimeUnit.SECONDS)) {
                if (h == null) {
                    logger.warn("clearMessage: lock timeout for KEY_BROADCAST");
                    return;
                }
                @SuppressWarnings("unchecked")
                List<BroadcastMsgEntry> entries = (List<BroadcastMsgEntry>)map.get(KEY_BROADCAST);
                entries.clear();
                map.put(KEY_BROADCAST, new ArrayList<BroadcastMsgEntry>(entries));
            }
            catch (Exception entries) {
            }
        }
    }

    public class BroadcastMsgModel {
        private List<BroadcastMsgEntry> entries = new ArrayList<BroadcastMsgEntry>();

        public List<BroadcastMsgEntry> getEntries() {
            return this.entries;
        }

        public void setEntries(List<BroadcastMsgEntry> entries) {
            this.entries = entries;
        }
    }

}
