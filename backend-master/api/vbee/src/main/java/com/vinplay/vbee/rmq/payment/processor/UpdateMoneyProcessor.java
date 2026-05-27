/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.IMap
 *  com.vinplay.usercore.utils.UserMakertingUtil
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastUtils
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.MoneyMessageInMinigame
 *  com.vinplay.vbee.common.models.cache.UserActiveModel
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.vbee.rmq.payment.processor;

import com.hazelcast.core.IMap;
import com.vinplay.usercore.utils.UserMakertingUtil;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastUtils;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.MoneyMessageInMinigame;
import com.vinplay.vbee.common.models.cache.UserActiveModel;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import com.vinplay.vbee.dao.impl.UserDaoImpl;
import java.util.Date;
import org.apache.log4j.Logger;

public class UpdateMoneyProcessor
implements BaseProcessor<byte[], Boolean> {
    private static final Logger logger = Logger.getLogger((String)"vbee");
    private static final long INTERVAL_TIME_UPDATE_MONEY = 10L;

    public Boolean execute(Param<byte[]> param) {
        byte[] body = (byte[])param.get();
        MoneyMessageInMinigame message = (MoneyMessageInMinigame)BaseMessage.fromBytes((byte[])body);
        try {
            boolean updateTimeOut;
            block20 : {
                String nickname = message.getNickname();
                IMap userMap = HazelcastUtils.getActiveMap((String)nickname);
                updateTimeOut = true;
                if (userMap.containsKey((Object)nickname)) {
                    // SUN-796 perf: lock-free pre-check for bot / stale message.
                    // Most queue_payment_minigame traffic is bot bets. Previously
                    // we always called userMap.lock() even for messages we'd
                    // immediately discard, AND `return true` skipped the unlock
                    // (lock leaked for ~5s lease) → 100 consumer threads queued
                    // on the same hot nicknames. Now bots/stale messages skip
                    // the lock entirely, and the real-player path uses
                    // try-finally so the lock can never leak.
                    UserActiveModel preCheck = (UserActiveModel)userMap.get((Object)nickname);
                    if (preCheck != null
                            && (Long.parseLong(message.getId()) < preCheck.getLastMessageId()
                                    || preCheck.isBot())) {
                        return true;
                    }

                    boolean locked = false;
                    try {
                        userMap.lock((Object)nickname);
                        locked = true;
                        UserActiveModel model = (UserActiveModel)userMap.get((Object)nickname);
                        // Re-check inside the lock — model state may have advanced
                        // between the pre-check read and the lock acquisition.
                        if (Long.parseLong(message.getId()) < model.getLastMessageId() || model.isBot()) {
                            return true;
                        }
                        long currentTime = System.currentTimeMillis();
                        long validTimeMs = currentTime - 600000L;
                        if (model.getLastActive() > validTimeMs) {
                            updateTimeOut = false;
                        } else {
                            if (message.getMoneyType().equals("vin")) {
                                boolean bl = updateTimeOut = !model.isBot() || VinPlayUtils.updateMoneyTimeout((long)model.getLastActiveVin(), (int)30);
                                if (updateTimeOut) {
                                    model.setLastActiveVin(new Date().getTime());
                                }
                            } else {
                                updateTimeOut = model.isBot() ? VinPlayUtils.updateMoneyTimeout((long)model.getLastActiveXu(), (int)30) : VinPlayUtils.updateMoneyTimeout((long)model.getLastActiveXu(), (int)10);
                                if (updateTimeOut) {
                                    model.setLastActiveXu(new Date().getTime());
                                }
                            }
                            model.setLastActive(new Date().getTime());
                            model.setLastMessageId(Long.parseLong(message.getId()));
                        }
                        model.setUpdateMySQL(updateTimeOut);
                        userMap.put((Object)nickname, (Object)model);
                    }
                    catch (Exception e) {
                        logger.error((Object)e);
                        break block20;
                    }
                    finally {
                        // Always release the Hazelcast distributed lock — was
                        // leaked on early returns + on exception break.
                        if (locked) {
                            try { userMap.unlock((Object)nickname); } catch (Exception ignored) {}
                        }
                    }
                }
            }
            int type = 0;
            if (message.getActionName().equals("Bot")) {
                type = 3;
            } else if (message.getActionName().equals("RechargeByCard") || message.getActionName().equals("RechargeByVinCard") || message.getActionName().equals("RechargeByMegaCard") || message.getActionName().equals("RechargeByBank") || message.getActionName().equals("RechargeByIAP") || message.getActionName().equals("RechargeBySMS") || message.getActionName().equals("TransferMoney") && message.getMoneyVP() == -1) {
                type = 1;
                UserMakertingUtil.userNapVin((String)message.getNickname(), (long)message.getMoneyExchange());
            } else if (message.getMoneyVP() > 0 || message.getVp() != 0) {
                type = 2;
            }
            if (!updateTimeOut) {
                // SUN-796 perf: was logger.info — with queue_payment_minigame
                // handling millions of msg/hour and most hitting this skip
                // path (bots + within-10s repeats), that info-level log was
                // a major disk/write contention source. INFO→DEBUG so default
                // log level (INFO) filters them out.
                if (logger.isDebugEnabled()) {
                    logger.debug("update no timeout nickname: " + message.getNickname()
                            + " money: " + message.getMoneyExchange()
                            + " current: " + message.getAfterMoney()
                            + " moneyType: " + message.getMoneyType());
                }
                return true;
            }
            UserDaoImpl userDao = new UserDaoImpl();
            userDao.updateMoney(message, type);
            return true;
        }
        catch (Exception e2) {
            e2.printStackTrace();
            return false;
        }
    }
}

