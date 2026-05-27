package com.vinplay.dal.service.impl;

import com.vinplay.dal.service.ChatLobbyService;
import com.vinplay.vbee.common.cache.CacheFactory;
import com.vinplay.vbee.common.cache.DistCache;

public class ChatLobbyServiceImpl implements ChatLobbyService {

    private DistCache<String, Long> cache() {
        return CacheFactory.get("cacheBanChat", Long.class);
    }

    @Override
    public void banChatUser(String nickname, long time) {
        DistCache<String, Long> userMap = cache();
        if (time > 0L) {
            userMap.put(nickname, System.currentTimeMillis() + time);
        } else {
            userMap.put(nickname, time);
        }
    }

    @Override
    public long getBanTime(String nickname) {
        DistCache<String, Long> userMap = cache();
        if (userMap.containsKey(nickname)) {
            Long val = userMap.get(nickname);
            long timeUnBan = val != null ? val : 0L;
            if (timeUnBan < System.currentTimeMillis()) {
                timeUnBan = 0L;
                userMap.remove(nickname);
            }
            return timeUnBan;
        }
        return 0L;
    }
}
