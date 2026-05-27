/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.dichvuthe.dao.impl.RechargeDaoImpl
 *  com.vinplay.dichvuthe.utils.SMSUtils
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.hazelcast.HazelcastUtils
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.LogMoneyUserMessage
 *  com.vinplay.vbee.common.messages.MoneyMessageInMinigame
 *  com.vinplay.vbee.common.messages.sms.LogRechargeSMSMessage
 *  com.vinplay.vbee.common.models.cache.UserMoneyModel
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  com.vinplay.vbee.common.utils.PhoneUtils
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 */
package com.vinplay.vbee.rmq.log.processor;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dichvuthe.dao.impl.RechargeDaoImpl;
import com.vinplay.dichvuthe.utils.SMSUtils;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.hazelcast.HazelcastUtils;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.messages.MoneyMessageInMinigame;
import com.vinplay.vbee.common.messages.sms.LogRechargeSMSMessage;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.models.cache.UserMoneyModel;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.rmq.RMQApi;
import com.vinplay.vbee.common.utils.PhoneUtils;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.util.concurrent.TimeUnit;

public class LogRechargeSMSProcessor
implements BaseProcessor<byte[], Boolean> {
    public Boolean execute(Param<byte[]> param) {
        LogRechargeSMSMessage message;
        RechargeDaoImpl rcdao;
        block15 : {
            message = (LogRechargeSMSMessage)BaseMessage.fromBytes((byte[])((byte[])param.get()));
            rcdao = new RechargeDaoImpl();
            if (message.code == 0) {
                HazelcastInstance client = HazelcastClientFactory.getInstance();
                IMap userMap = HazelcastUtils.getMoneyMap((String)message.nickname);
                if (userMap.containsKey((Object)message.nickname)) {
                    try {
                        userMap.lock((Object)message.nickname);
                        if (rcdao.checkRequestIdSMS(message.requestId)) {
                            return true;
                        }
                        boolean query = false;
                        if (message.smsType == 1 || PhoneUtils.getProviderByMobile((String)message.mobile, (boolean)true) == 1) {
                            query = true;
                        }
                        boolean billing = true;
                        if (query) {
                            billing = SMSUtils.queryTransaction((String)message.requestId, (int)message.smsType);
                        }
                        if (billing) {
                            UserMoneyModel user = (UserMoneyModel)userMap.get((Object)message.nickname);
                            long moneyUser = user.getVin();
                            long currentMoney = user.getVinTotal();
                            long rechargeMoney = user.getRechargeMoney();
                            user.setVin(moneyUser += (long)message.money);
                            user.setVinTotal(currentMoney += (long)message.money);
                            user.setRechargeMoney(rechargeMoney += (long)message.money);
                            String description = "SDT: " + message.mobile + " .N\u1ed9i dung tin nh\u1eafn: " + message.moMessage + ". \u0110\u1ea7u s\u1ed1: " + message.shortCode;
                            MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), message.nickname, "RechargeBySMS", moneyUser, currentMoney, (long)message.money, "vin", 0L, 0, 0);
                            LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), message.nickname, "RechargeBySMS", "N\u1ea1p vin qua SMS", currentMoney, (long)message.money, "vin", description, 0L, false, user.isBot());
                            MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageMoney, (int)16);
                            MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage) messageLog, 601);
                            userMap.put((Object)message.nickname, (Object)user);
                        } else {
                            IMap msgMap = client.getMap("cacheSmsPlusPending");
                            msgMap.put((Object)(String.valueOf(message.requestId) + message.smsType), (Object)message, 6L, TimeUnit.HOURS);
                            message.code = 30;
                            message.des = "\u0110ang ch\u1edd x\u1eed l\u00fd";
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        break block15;
                    }
                    try {
                        userMap.unlock((Object)message.nickname);
                    }
                    catch (Exception e) {
                        // empty catch block
                    }
                }
            }
        }
        switch (message.smsType) {
            case 2: {
                rcdao.saveLogRechargeBySMSPlus(message.nickname, message.mobile, message.moMessage, message.amount, message.shortCode, message.errorCode, message.errorMessage, message.requestId, message.requestTime, message.code, message.des, message.money);
                break;
            }
            case 1: {
                rcdao.saveLogRechargeBySMS(message.nickname, message.mobile, message.moMessage, message.amount, message.shortCode, message.requestId, message.requestTime, message.code, message.des, message.money);
            }
        }
        return true;
    }
}

