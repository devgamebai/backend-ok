/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.hazelcast.HazelcastUtils
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.LogMoneyUserMessage
 *  com.vinplay.vbee.common.messages.MoneyMessageInMinigame
 *  com.vinplay.vbee.common.messages.sms.LogRechargeSMSMessage
 *  com.vinplay.vbee.common.models.cache.UserMoneyModel
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.json.JSONObject
 */
package com.vinplay.dichvuthe.utils;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dichvuthe.client.HttpURLClient;
import com.vinplay.dichvuthe.dao.impl.RechargeDaoImpl;
import com.vinplay.dichvuthe.utils.DvtUtils;
import com.vinplay.dichvuthe.utils.SmsPlusTask;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.hazelcast.HazelcastUtils;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.messages.MoneyMessageInMinigame;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.sms.LogRechargeSMSMessage;
import com.vinplay.vbee.common.models.cache.UserMoneyModel;
import com.vinplay.vbee.common.rmq.RMQApi;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import org.json.JSONObject;

public class SMSUtils {
    private static List<String> smsIds = new LinkedList<String>();
    private static List<String> smsPlusIds = new LinkedList<String>();
    public static final int SMS8x = 1;
    public static final int SMSPlus = 2;
    public static final int MAX_SIZE = 1000;

    public static void init() {
        Timer timer = new Timer();
        timer.schedule((TimerTask)new SmsPlusTask(), 60000L, 300000L);
    }

    public static void loadMessage() {
        RechargeDaoImpl dao = new RechargeDaoImpl();
        List<String> sms = dao.getListSmsIdNearly();
        for (int i = sms.size(); i > 0; --i) {
            smsIds.add(sms.get(i));
        }
        List<String> smsPlus = dao.getListSmsPlusIdNearly();
        for (int j = smsPlus.size(); j > 0; --j) {
            smsPlusIds.add(smsPlus.get(j));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static boolean checkRequestId(String id, int smsType) {
        switch (smsType) {
            case 1: {
                List<String> list;
                List<String> list2 = list = smsIds;
                synchronized (list2) {
                    if (!smsIds.contains(id)) {
                        if (smsIds.size() >= 1000) {
                            smsIds.remove(0);
                        }
                        smsIds.add(id);
                        return false;
                    }
                    break;
                }
            }
            case 2: {
                List<String> list;
                List<String> list3 = list = smsPlusIds;
                synchronized (list3) {
                    if (!smsPlusIds.contains(id)) {
                        if (smsPlusIds.size() >= 1000) {
                            smsPlusIds.remove(0);
                        }
                        smsPlusIds.add(id);
                        return false;
                    }
                    break;
                }
            }
        }
        return true;
    }

    public static boolean queryTransaction(String requestId, int smsType) {
        try {
            JSONObject sms;
            String chargingType = "";
            if (smsType == 1) {
                chargingType = "sms";
            } else if (smsType == 2) {
                chargingType = "iac";
            }
            HashMap<String, String> params = new HashMap<String, String>();
            params.put("access_key", GameCommon.getValueStr("SMS_PLUS_ACCESS_KEY"));
            params.put("request_id", requestId);
            params.put("charging_type", chargingType);
            String signature = VinPlayUtils.hmacDigest((String)DvtUtils.getSMSPlusHMAC(params), (String)GameCommon.getValueStr("SMS_PLUS_SECRET_KEY"), (String)"HmacSHA256");
            params.put("signature", signature);
            String url = GameCommon.getValueStr("SMS_PLUS_URL") + "?" + HttpURLClient.buildParams(params);
            String response = HttpURLClient.sendGET(url);
            JSONObject obj = new JSONObject(response);
            if (obj.has(chargingType) && (sms = new JSONObject(obj.getString(chargingType))).has("billing_status")) {
                return sms.getString("billing_status").equals("1");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void queryTransactionPending() {
        RechargeDaoImpl rcdao = new RechargeDaoImpl();
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, LogRechargeSMSMessage> msgMap = client.getMap("cacheSmsPlusPending");
        for (Map.Entry entry : msgMap.entrySet()) {
            try {
                IMap userMap;
                LogRechargeSMSMessage message = (LogRechargeSMSMessage)entry.getValue();
                boolean billing = SMSUtils.queryTransaction(message.requestId, message.smsType);
                if (!billing || !(userMap = HazelcastUtils.getMoneyMap((String)message.nickname)).containsKey(message.nickname)) continue;
                try {
                    userMap.lock(message.nickname);
                    UserMoneyModel user = (UserMoneyModel)userMap.get(message.nickname);
                    long moneyUser = user.getVin();
                    long currentMoney = user.getVinTotal();
                    long rechargeMoney = user.getRechargeMoney();
                    user.setVin(moneyUser += (long)message.money);
                    user.setVinTotal(currentMoney += (long)message.money);
                    user.setRechargeMoney(rechargeMoney += (long)message.money);
                    String description = "SDT: " + message.mobile + " .N\u1ed9i dung tin nh\u1eafn: " + message.moMessage + ". \u0110\u1ea7u s\u1ed1: " + message.shortCode;
                    MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), message.nickname, "RechargeBySMS", moneyUser, currentMoney, (long)message.money, "vin", 0L, 0, 0);
                    LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), message.nickname, "RechargeBySMS", "N\u1ea1p Win qua SMS", currentMoney, (long)message.money, "vin", description, 0L, false, user.isBot());
                    MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageMoney, (int)16);
                    MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                     userMap.put(message.nickname, user);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                     userMap.unlock(message.nickname);
                }
                rcdao.updateSMS(message.requestId, 0, "Th\u00e0nh c\u00f4ng", message.smsType);
                msgMap.remove(entry.getKey());
            }
            catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }
}

