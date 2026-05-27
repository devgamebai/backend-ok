/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 */
package com.vinplay.safebox.core.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.safebox.core.SafeBoxService;
import com.vinplay.safebox.dao.impl.SafeBoxDaoImpl;
import com.vinplay.safebox.response.SafeBoxResponse;
import com.vinplay.usercore.service.impl.OtpServiceImpl;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.messages.MoneyMessageInMinigame;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.rmq.RMQApi;
import com.vinplay.vbee.common.utils.VinPlayUtils;

public class SafeBoxImpl
implements SafeBoxService {
    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public SafeBoxResponse depositSafeBox(String userName, double amount) {
        SafeBoxResponse safeBoxResponse = new SafeBoxResponse(1, "");
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        if (client == null) {
            safeBoxResponse.message = "Kh\u00f4ng k\u1ebft n\u1ed1i \u0111\u01b0\u1ee3c";
            return safeBoxResponse;
        }
        IMap userMap = client.getMap("users");
        if (!userMap.containsKey(userName)) {
            return safeBoxResponse;
        }
        try {
            userMap.lock(userName);
            UserCacheModel user = (UserCacheModel)userMap.get(userName);
            long moneyUser = user.getVin();
            long currentMoney = user.getVinTotal();
            long rechargeMoney = user.getRechargeMoney();
            if ((double)currentMoney < amount) {
                safeBoxResponse.message = "S\u1ed1 ti\u1ec1n kh\u00f4ng \u0111\u1ee7";
                SafeBoxResponse safeBoxResponse2 = safeBoxResponse;
                return safeBoxResponse2;
            }
            moneyUser = (long)((double)moneyUser - amount);
            user.setVin(moneyUser);
            currentMoney = (long)((double)currentMoney - amount);
            user.setVinTotal(currentMoney);
            rechargeMoney = (long)((double)rechargeMoney - amount);
            user.setRechargeMoney(rechargeMoney);
            String desc = "N\u1ea1p ti\u1ec1n k\u00e9t s\u1eaft";
            MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), userName, "RechargeBySafeBox", moneyUser, currentMoney, (long)amount, "vin", 0L, 0, 0);
            LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), userName, "RechargeBySafeBox", "SAFE BOX", currentMoney, (long)amount, "vin", desc, 0L, false, user.isBot());
            messageLog.setReferralCode(user.getReferralCode());
            MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", messageMoney, 16);
            MessageBusFactory.get("queue_log_money").publish("queue_log_money", messageLog, 601);
            SafeBoxDaoImpl safeBoxDao = new SafeBoxDaoImpl();
            safeBoxDao.depositSafeBox(userName, amount);
            userMap.put(userName, user);
            safeBoxResponse.status = 0;
            safeBoxResponse.message = "N\u1ea1p ti\u1ec1n v\u00e0o k\u00e9t s\u1eaft th\u00e0nh c\u00f4ng";
            safeBoxResponse.amount = safeBoxDao.getSafeBox(userName);
            safeBoxResponse.currentMoney = currentMoney;
        }
        catch (Exception e2) {
            e2.printStackTrace();
        }
        finally {
            userMap.unlock(userName);
        }
        return safeBoxResponse;
    }

    @Override
    public SafeBoxResponse getSafeBox(String userName) {
        SafeBoxResponse safeBoxResponse = new SafeBoxResponse(0, "");
        SafeBoxDaoImpl safeBoxDao = new SafeBoxDaoImpl();
        safeBoxResponse.amount = safeBoxDao.getSafeBox(userName);
        return safeBoxResponse;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public SafeBoxResponse withDraw(String userName, double amount, String otp) {
        SafeBoxResponse safeBoxResponse;
        block11: {
            safeBoxResponse = new SafeBoxResponse(1, "");
            SafeBoxDaoImpl safeBoxDao = new SafeBoxDaoImpl();
            double amountDB = safeBoxDao.getSafeBox(userName);
            if (amountDB < amount) {
                safeBoxResponse.message = "S\u1ed1 ti\u1ec1n r\u00fat l\u1edbn h\u01a1n";
                return safeBoxResponse;
            }
            OtpServiceImpl otpService = new OtpServiceImpl();
            try {
                boolean code = false;
                if (!code) {
                    HazelcastInstance client = HazelcastClientFactory.getInstance();
                    if (client == null) {
                        safeBoxResponse.message = "Kh\u00f4ng k\u1ebft n\u1ed1i \u0111\u01b0\u1ee3c";
                        return safeBoxResponse;
                    }
                    IMap userMap = client.getMap("users");
                    if (!userMap.containsKey(userName)) {
                        return safeBoxResponse;
                    }
                    try {
                        userMap.lock(userName);
                        UserCacheModel user = (UserCacheModel)userMap.get(userName);
                        long moneyUser = user.getVin();
                        long currentMoney = user.getVinTotal();
                        long rechargeMoney = user.getRechargeMoney();
                        moneyUser = (long)((double)moneyUser + amount);
                        user.setVin(moneyUser);
                        currentMoney = (long)((double)currentMoney + amount);
                        user.setVinTotal(currentMoney);
                        rechargeMoney = (long)((double)rechargeMoney + amount);
                        user.setRechargeMoney(rechargeMoney);
                        String desc = "N\u1ea1p ti\u1ec1n k\u00e9t s\u1eaft";
                        MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), userName, "RechargeBySafeBox", moneyUser, currentMoney, (long)amount, "vin", 0L, 0, 0);
                        LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), userName, "RechargeBySafeBox", "SAFE BOX", currentMoney, (long)amount, "vin", desc, 0L, false, user.isBot());
                        messageLog.setReferralCode(user.getReferralCode());
                        MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", messageMoney, 16);
                        MessageBusFactory.get("queue_log_money").publish("queue_log_money", messageLog, 601);
                        safeBoxDao.withDraw(userName, amount);
                        userMap.put(userName, user);
                        safeBoxResponse.status = 0;
                        safeBoxResponse.message = "R\u00fat ti\u1ec1n k\u00e9t s\u1eaft th\u00e0nh c\u00f4ng";
                        safeBoxResponse.currentMoney = currentMoney;
                        safeBoxResponse.amount = safeBoxDao.getSafeBox(userName);
                        break block11;
                    }
                    catch (Exception e2) {
                        e2.printStackTrace();
                        break block11;
                    }
                    finally {
                        userMap.unlock(userName);
                    }
                }
                safeBoxResponse.message = "C\u00f3 l\u1ed7i OTP";
                return safeBoxResponse;
            }
            catch (Exception e) {
                safeBoxResponse.message = "C\u00f3 l\u1ed7i OTP";
                return safeBoxResponse;
            }
        }
        return safeBoxResponse;
    }
}

