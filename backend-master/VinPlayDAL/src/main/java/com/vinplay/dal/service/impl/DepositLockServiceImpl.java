package com.vinplay.dal.service.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.service.DepositLockService;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.json.JSONObject;

public class DepositLockServiceImpl implements DepositLockService {

    private static final Logger logger = Logger.getLogger("api");
    private static final String LOCK_MAP_NAME = "deposit_locks";
    private static final String RATE_MAP_NAME = "deposit_rate";
    private static final int LOCK_TTL_MINUTES = 30;
    private static final int RATE_LIMIT_SECONDS = 5;

    private IMap<String, String> getLockMap() {
        HazelcastInstance instance = HazelcastClientFactory.getInstance();
        return instance.getMap(LOCK_MAP_NAME);
    }

    private IMap<String, String> getRateMap() {
        HazelcastInstance instance = HazelcastClientFactory.getInstance();
        return instance.getMap(RATE_MAP_NAME);
    }

    @Override
    public boolean tryLock(long txId, String operatorName, String platform) {
        try {
            IMap<String, String> lockMap = getLockMap();
            String key = "tx:" + txId;
            JSONObject lockInfo = new JSONObject();
            lockInfo.put("operator", operatorName);
            lockInfo.put("platform", platform);
            lockInfo.put("locked_at", System.currentTimeMillis());
            String existing = lockMap.putIfAbsent(key, lockInfo.toString(), LOCK_TTL_MINUTES, TimeUnit.MINUTES);
            return existing == null;
        } catch (Exception e) {
            logger.error("DepositLockService.tryLock error txId=" + txId, e);
            return false;
        }
    }

    @Override
    public boolean release(long txId, String operatorName) {
        try {
            IMap<String, String> lockMap = getLockMap();
            String key = "tx:" + txId;
            String value = lockMap.get(key);
            if (value == null) {
                return false;
            }
            JSONObject lockInfo = new JSONObject(value);
            String lockedOperator = lockInfo.optString("operator", "");
            if (!lockedOperator.equals(operatorName)) {
                logger.warn("DepositLockService.release operator mismatch txId=" + txId +
                        " locked_by=" + lockedOperator + " release_by=" + operatorName);
                return false;
            }
            lockMap.remove(key);
            return true;
        } catch (Exception e) {
            logger.error("DepositLockService.release error txId=" + txId, e);
            return false;
        }
    }

    @Override
    public void forceRelease(long txId) {
        try {
            IMap<String, String> lockMap = getLockMap();
            String key = "tx:" + txId;
            lockMap.remove(key);
        } catch (Exception e) {
            logger.error("DepositLockService.forceRelease error txId=" + txId, e);
        }
    }

    @Override
    public Map<String, Object> getLockInfo(long txId) {
        try {
            IMap<String, String> lockMap = getLockMap();
            String key = "tx:" + txId;
            String value = lockMap.get(key);
            if (value == null) {
                return null;
            }
            JSONObject json = new JSONObject(value);
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("operator", json.optString("operator", ""));
            result.put("platform", json.optString("platform", ""));
            result.put("locked_at", json.optLong("locked_at", 0));
            return result;
        } catch (Exception e) {
            logger.error("DepositLockService.getLockInfo error txId=" + txId, e);
            return null;
        }
    }

    @Override
    public boolean isLocked(long txId) {
        try {
            IMap<String, String> lockMap = getLockMap();
            String key = "tx:" + txId;
            return lockMap.containsKey(key);
        } catch (Exception e) {
            logger.error("DepositLockService.isLocked error txId=" + txId, e);
            return false;
        }
    }

    @Override
    public boolean checkRateLimit(long userId) {
        try {
            IMap<String, String> rateMap = getRateMap();
            String key = "rate:" + userId;
            String existing = rateMap.putIfAbsent(key, "1", RATE_LIMIT_SECONDS, TimeUnit.SECONDS);
            return existing == null;
        } catch (Exception e) {
            logger.error("DepositLockService.checkRateLimit error userId=" + userId, e);
            return true;
        }
    }
}
