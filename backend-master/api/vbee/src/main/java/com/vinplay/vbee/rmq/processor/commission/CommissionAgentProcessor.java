/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.LogMoneyUserMessage
 *  com.vinplay.vbee.common.models.commission.AgentCommissionModel
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.vbee.rmq.processor.commission;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.models.commission.AgentCommissionModel;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import com.vinplay.vbee.dao.impl.ExceptionDaoImpl;
import com.vinplay.vbee.dao.impl.UserDaoImpl;
import java.util.Date;
import org.apache.log4j.Logger;

public class CommissionAgentProcessor
implements BaseProcessor<byte[], Boolean> {
    private static final Logger logger = Logger.getLogger((String)"vbee");

    public Boolean execute(Param<byte[]> param) {
        block8 : {
            LogMoneyUserMessage message = (LogMoneyUserMessage)BaseMessage.fromBytes((byte[])((byte[])param.get()));
            try {
                if (message.isBot() || message.getFee() <= 0L || !message.getMoneyType().equals("vin") || !message.isVp()) break block8;
                HazelcastInstance client = HazelcastClientFactory.getInstance();
                IMap agentCommMap = client.getMap("cacheAgentCommission");
                AgentCommissionModel commission = new AgentCommissionModel();
                UserDaoImpl dao = new UserDaoImpl();
                long fee = 0L;
                String timeEnd = DateTimeUtils.getCurrentTime((String)"yyyy-MM-dd HH:mm:ss");
                if (agentCommMap.containsKey((Object)message.getNickname())) {
                    try {
                        if (Long.parseLong(message.getId()) < commission.getLastMessageId()) {
                            logger.error((Object)("message kh\u00f4ng x\u1eed l\u00fd: " + message.getId()));
                            ExceptionDaoImpl ex = new ExceptionDaoImpl();
                            ex.insertLogExceptionDB(message.getMoneyType(), message.toJson(), "Message kh\u00f4ng x\u1eed l\u00fd, current process ID: " + commission.getLastMessageId());
                            return false;
                        }
                        agentCommMap.lock((Object)message.getNickname());
                        commission = (AgentCommissionModel)agentCommMap.get((Object)message.getNickname());
                        fee = commission.getFee() + message.getFee();
                        long volume = commission.getVolume() + Math.abs(message.getMoneyExchange());
                        commission.setFee(fee);
                        commission.setVolume(volume);
                        commission.setLastActive(new Date());
                        commission.setLastMessageId(Long.parseLong(message.getId()));
                        agentCommMap.put((Object)message.getNickname(), (Object)commission);
                    }
                    catch (Exception e) {
                        logger.error((Object)e);
                        e.printStackTrace();
                        return false;
                    }
                    try {
                        agentCommMap.unlock((Object)message.getNickname());
                    }
                    catch (Exception e) {}
                    break block8;
                }
                fee = dao.getFeeUser(message.getNickname(), timeEnd);
                commission.setFee(fee);
                // First occurence, capture volume from this message
                commission.setVolume(Math.abs(message.getMoneyExchange()));
                commission.setNickName(message.getNickname());
                commission.setUserId(message.getUserId());
                commission.setLastActive(new Date());
                commission.setLastMessageId(Long.parseLong(message.getId()));
                agentCommMap.put((Object)message.getNickname(), (Object)commission);
            }
            catch (Exception e2) {
                e2.printStackTrace();
                logger.error((Object)e2);
            }
        }
        return false;
    }
}

