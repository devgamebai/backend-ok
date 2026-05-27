package com.vinplay.dal.service;

public interface DepositQueueService {
    void publishNewDeposit(long txId, String txCode, long userId, String nickName,
        long amount, String bankName, String bankNumber) throws Exception;
}
