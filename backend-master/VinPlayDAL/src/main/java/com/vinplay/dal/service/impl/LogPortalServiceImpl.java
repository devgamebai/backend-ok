package com.vinplay.dal.service.impl;

import com.vinplay.dal.service.LogPortalService;
import com.vinplay.vbee.common.cache.CacheFactory;
import com.vinplay.vbee.common.cache.DistCache;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import org.apache.log4j.Logger;

import java.util.concurrent.TimeUnit;

public class LogPortalServiceImpl implements LogPortalService {
    private static final long CACHE_LOG_PORTAL_TTL = 60L;
    private static final Logger logger = Logger.getLogger("count_request_portal_logger");
    private static final String FORMAT = ",%20s,\t%s,\t%6d";

    private DistCache<String, Long> cache() {
        return CacheFactory.get("cacheLogPortal", Long.class);
    }

    @Override
    public void log(String command) {
        DistCache<String, Long> map = cache();
        if (!map.containsKey(command)) {
            map.put(command, 1L, CACHE_LOG_PORTAL_TTL, TimeUnit.MINUTES);
        } else {
            Long current = map.get(command);
            long count = current != null ? current + 1 : 1L;
            map.put(command, count, CACHE_LOG_PORTAL_TTL, TimeUnit.MINUTES);
        }
    }

    @Override
    public void saveLog() {
        String time = DateTimeUtils.getCurrentTime();
        DistCache<String, Long> map = cache();
        for (String c : map.keySet()) {
            Long val = map.get(c);
            long count = val != null ? val : 0L;
            logger.debug(String.format(FORMAT, time, c, count));
            map.remove(c);
        }
    }
}
