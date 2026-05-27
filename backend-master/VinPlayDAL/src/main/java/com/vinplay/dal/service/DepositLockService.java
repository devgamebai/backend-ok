package com.vinplay.dal.service;

import java.util.Map;

public interface DepositLockService {
    boolean tryLock(long txId, String operatorName, String platform);
    boolean release(long txId, String operatorName);
    void forceRelease(long txId);
    Map<String, Object> getLockInfo(long txId);
    boolean isLocked(long txId);
    boolean checkRateLimit(long userId);
}
