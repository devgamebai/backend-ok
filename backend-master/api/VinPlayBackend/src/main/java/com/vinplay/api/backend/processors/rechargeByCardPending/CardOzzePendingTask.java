/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.dichvuthe.dao.impl.RechargeDaoImpl
 *  com.vinplay.lucky79.Lucky79Client
 *  com.vinplay.lucky79.ReCheckLucky79Response
 *  com.vinplay.usercore.logger.MoneyLogger
 *  com.vinplay.usercore.service.impl.MailBoxServiceImpl
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.LogMoneyUserMessage
 *  com.vinplay.vbee.common.messages.MoneyMessageInMinigame
 *  com.vinplay.vbee.common.messages.dvt.RechargeByCardMessage
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors.rechargeByCardPending;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dichvuthe.dao.impl.RechargeDaoImpl;
import com.vinplay.lucky79.Lucky79Client;
import com.vinplay.lucky79.ReCheckLucky79Response;
import com.vinplay.usercore.logger.MoneyLogger;
import com.vinplay.usercore.service.impl.MailBoxServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.messages.MoneyMessageInMinigame;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.dvt.RechargeByCardMessage;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.rmq.RMQApi;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;
import org.apache.log4j.Logger;

public class CardOzzePendingTask
extends TimerTask {
    private static final Logger logger = Logger.getLogger((String)"report");

    @Override
    public void run() {
        try {
            Calendar cal = Calendar.getInstance();
            int hour = cal.get(11);
            int minute = cal.get(12);
            String date = "";
            date = hour == 0 && minute == 30 ? VinPlayUtils.getYesterday() : VinPlayUtils.getCurrentDate();
            logger.info((Object)("Recheck  by card " + date + ". Start time: " + new Date()));
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            RechargeDaoImpl dao = new RechargeDaoImpl();
            List<RechargeByCardMessage> listPending = dao.getListCardPending();
            Lucky79Client maxpay = new Lucky79Client(GameCommon.getValueStr("THE_CAO_MERCHANT_ID"), GameCommon.getValueStr("THE_CAO_SECRET_KEY"));
            String actor = "system";
            for (RechargeByCardMessage message : listPending) {
                message.setError((String)null);
                ReCheckLucky79Response response = null;
                if (Integer.parseInt(message.getMessage()) <= 0) continue;
                try {
                    long currentMoney;
                    UserCacheModel user2;
                    String description;
                    IMap userMap;
                    int code;
                    long rechargeMoney;
                    response = maxpay.doReCheck(message.getMessage());
                    if (response == null || response.getStatus() == null || response.getStatus() == "" || (code = this.mapLucky79ToVinplayCode(response.getStatus())) == 30) continue;
                    long money = 0L;
                    if (code == 0) {
                        if (client == null) {
                            MoneyLogger.log((String)message.getNickname(), (String)"RechargeByCard", (long)0L, (long)0L, (String)"vin", (String)"Nap Win qua the", (String)"1030", (String)"can not connect hazelcast");
                            continue;
                        }
                        userMap = client.getMap("users");
                        if (userMap.containsKey((Object)message.getNickname())) {
                            block15 : {
                                money = Math.round(response.getCard_amount() * GameCommon.getValueDouble((String)"RATIO_RECHARGE_CARD"));
                                try {
                                    userMap.lock((Object)message.getNickname());
                                    user2 = (UserCacheModel)userMap.get((Object)message.getNickname());
                                    long moneyUser = user2.getVin();
                                    currentMoney = user2.getVinTotal();
                                    rechargeMoney = user2.getRechargeMoney();
                                    user2.setVin(moneyUser += money);
                                    user2.setVinTotal(currentMoney += money);
                                    user2.setRechargeMoney(rechargeMoney += money);
                                    description = "K\u1ebft qu\u1ea3: Th\u00e0nh c\u00f4ng, M\u00e3 GD: " + message.getReferenceId() + ", Th\u1ebb: " + message.getProvider() + ", M\u1ec7nh gi\u00e1: " + response.getCard_amount() + ", Serial: " + message.getSerial() + ", Pin: " + message.getPin();
                                    MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user2.getId(), message.getNickname(), "RechargeByCard", moneyUser, currentMoney, money, "vin", 0L, 0, 0);
                                    LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user2.getId(), message.getNickname(), "RechargeByCard", "N\u1ea1p Win qua th\u1ebb", currentMoney, money, "vin", description, 0L, false, user2.isBot());
                                    MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageMoney, (int)16);
                                    MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage) messageLog, 601);
                                    userMap.put((Object)message.getNickname(), (Object)user2);
                                }
                                catch (Exception e) {
                                    logger.debug((Object)e);
                                    code = 1;
                                    message.setError("1037");
                                    break block15;
                                }
                                try {
                                    userMap.unlock((Object)message.getNickname());
                                }
                                catch (Exception e) {
                                    // empty catch block
                                }
                            }
                            if (code != 0) continue;
                            ArrayList<String> nicknames = new ArrayList<String>();
                            nicknames.add(message.getNickname());
                            MailBoxServiceImpl mailSer = new MailBoxServiceImpl();
                            String content = "N\u1ea1p win th\u00e0nh c\u00f4ng. B\u1ea1n \u0111\u00e3 nh\u1eadn \u0111\u01b0\u1ee3c " + money + " win. M\u00e3 GD: " + message.getReferenceId() + ", Th\u1ebb: " + message.getProvider() + ", M\u1ec7nh gi\u00e1: " + response.getCard_amount() + ", Serial: " + message.getSerial() + ", Pin: " + message.getPin();
                            mailSer.sendMailBoxFromByNickName(nicknames, "Th\u00f4ng b\u00e1o n\u1ea1p win th\u00e0nh c\u00f4ng", content);
                            dao.insertLogUpdateCardPending(message.getReferenceId(), message.getNickname(), message.getProvider(), message.getSerial(), message.getPin(), String.valueOf(response.getCard_amount()), String.valueOf(response.getStatus()), response.getId(), String.valueOf(code), message.getTimeLog(), String.valueOf(money), "system");
                            dao.updateCard(message.getReferenceId(), (int)response.getCard_amount(), this.mapLucky79ToVinplayCode(response.getStatus()), response.getId(), code);
                            message.setAmount((int)response.getCard_amount());
                            message.setStatus(this.mapLucky79ToVinplayCode(response.getStatus()));
                            message.setMessage(response.getId());
                            message.setCode(code);
                            continue;
                        }
                        message.setError("1038");
                        continue;
                    }
                    if (code == 35 && (userMap = client.getMap("users")).containsKey((Object)message.getNickname())) {
                        money = Math.round(response.getCard_amount() * GameCommon.getValueDouble((String)"RATIO_RECHARGE_CARD"));
                        try {
                            user2 = (UserCacheModel)userMap.get((Object)message.getNickname());
                            long moneyUser = user2.getVin();
                            currentMoney = user2.getVinTotal();
                            rechargeMoney = user2.getRechargeMoney();
                            description = "K\u1ebft qu\u1ea3: th\u1ebb n\u1ea1p sai, M\u00e3 GD: " + message.getReferenceId() + ", Th\u1ebb: " + message.getProvider() + ", Serial: " + message.getSerial() + ", Pin: " + message.getPin();
                            LogMoneyUserMessage messageLog2 = new LogMoneyUserMessage(user2.getId(), message.getNickname(), "RechargeByCard", "N\u1ea1p Win qua th\u1ebb", currentMoney, money, "vin", description, 0L, false, user2.isBot());
                            MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage) messageLog2, 601);
                        }
                        catch (Exception e) {
                            // empty catch block
                        }
                    }
                    dao.updateCard(message.getReferenceId(), (int)response.getCard_amount(), this.mapLucky79ToVinplayCode(response.getStatus()), response.getId(), code);
                    dao.insertLogUpdateCardPending(message.getReferenceId(), message.getNickname(), message.getProvider(), message.getSerial(), message.getPin(), String.valueOf(response.getCard_amount()), String.valueOf(response.getStatus()), response.getId(), String.valueOf(code), message.getTimeLog(), String.valueOf(money), "system");
                    message.setAmount((int)response.getCard_amount());
                    message.setStatus(this.mapLucky79ToVinplayCode(response.getStatus()));
                    message.setMessage(response.getId());
                    message.setCode(code);
                }
                catch (Exception e2) {
                    e2.printStackTrace();
                    logger.debug((Object)e2);
                }
            }
            logger.info((Object)("Recheck  by card " + date + " success at " + new Date()));
        }
        catch (Exception e3) {
            logger.debug((Object)e3);
            e3.printStackTrace();
        }
    }

    private String luckyProviderMaxpay(String vinplayProvider) {
        switch (vinplayProvider) {
            case "Vinaphone": {
                return "vn";
            }
            case "Mobifone": {
                return "mb";
            }
            case "gate": {
                return "FPT";
            }
            case "Viettel": {
                return "vt";
            }
        }
        return vinplayProvider;
    }

    private String mapVinplayLucky79Message(String lucky79Code) {
        if (lucky79Code != null && !lucky79Code.equals("")) {
            String trim;
            switch (trim = lucky79Code.trim()) {
                case "success": {
                    return "N\u1ea1p th\u1ebb th\u00e0nh c\u00f4ng";
                }
                case "card_fail": {
                    return "N\u1ea1p Kh\u00f4ng th\u1ebb th\u00e0nh c\u00f4ng";
                }
                case "processing": {
                    return "Th\u1ebb \u0111ang \u0111\u01b0\u1ee3c n\u1ea1p tr\u00ean thi\u1ebft b\u1ecb";
                }
                case "waiting": {
                    return "Th\u1ebb \u0111ang ch\u1edd \u0111\u1ec3 n\u1ea1p";
                }
            }
            return "Tr\u1ea1ng th\u00e1i kh\u00f4ng x\u00e1c \u0111\u1ecbnh";
        }
        return "";
    }

    private int mapLucky79ToVinplayCode(String lucky79Code) {
        String trim;
        switch (trim = lucky79Code.trim()) {
            case "success": {
                return 0;
            }
            case "card_fail": {
                return 35;
            }
            case "processing": {
                return 30;
            }
            case "waiting": {
                return 30;
            }
        }
        return 30;
    }
}

