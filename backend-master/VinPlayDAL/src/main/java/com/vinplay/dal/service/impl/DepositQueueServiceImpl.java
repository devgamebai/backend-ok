package com.vinplay.dal.service.impl;

import com.vinplay.dal.service.DepositQueueService;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.DepositMessage;
import com.vinplay.vbee.common.statics.RMQCommands;

import org.apache.log4j.Logger;

public class DepositQueueServiceImpl implements DepositQueueService {

    private static final Logger logger = Logger.getLogger("api");
    private static final String QUEUE_NAME = "deposit_bank_queue";

    @Override
    public void publishNewDeposit(long txId, String txCode, long userId, String nickName,
            long amount, String bankName, String bankNumber) throws Exception {
        publishNewDeposit(txId, txCode, userId, nickName, amount, bankName, bankNumber, null);
    }

    public void publishNewDeposit(long txId, String txCode, long userId, String nickName,
            long amount, String bankName, String bankNumber, String holderName) throws Exception {
        DepositMessage message = new DepositMessage(txId, txCode, userId, nickName,
                amount, bankName, bankNumber, holderName);
        message.setId(String.valueOf(txId));
        logger.info("Publishing deposit message txId=" + txId + " txCode=" + txCode +
                " userId=" + userId + " amount=" + amount);
        MessageBusFactory.get(QUEUE_NAME).publish(QUEUE_NAME, message, RMQCommands.DEPOSIT_BANK_NEW);
    }
}
