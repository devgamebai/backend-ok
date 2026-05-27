/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.dal.dao.impl.AgentDAOImpl
 *  com.vinplay.dal.entities.agent.BonusTopDSModel
 *  com.vinplay.dichvuthe.service.impl.AlertServiceImpl
 *  com.vinplay.usercore.dao.impl.GameConfigDaoImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.dvt.RefundFeeAgentMessage
 *  com.vinplay.vbee.common.models.cache.AgentDSModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  com.vinplay.vbee.common.response.TranferAgentResponse
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  com.vinplay.vbee.common.utils.MapUtils
 *  com.vinplay.vbee.common.utils.NumberUtils
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.apache.log4j.Logger
 *  org.json.JSONArray
 *  org.json.JSONException
 *  org.json.JSONObject
 */
package com.vinplay.api.backend.agent.utils;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.dal.entities.agent.BonusTopDSModel;
import com.vinplay.dichvuthe.service.impl.AlertServiceImpl;
import com.vinplay.usercore.dao.impl.GameConfigDaoImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.dvt.RefundFeeAgentMessage;
import com.vinplay.vbee.common.models.cache.AgentDSModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.TranferAgentResponse;
import com.vinplay.vbee.common.utils.MapUtils;
import com.vinplay.vbee.common.utils.NumberUtils;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AgentUtils {
    private static final Logger logger = Logger.getLogger((String)"backend");
    public static long dsMin;
    public static long dsMin1;
    public static List<Long> listBonusFix;
    public static Map<Long, Double> mapBonusMore;
    public static boolean isOpen;
    public static long dsMinDL2;
    public static List<Long> listBonusFixDL2;
    public static Map<Long, Double> mapBonusMoreDL2;
    public static boolean isOpenDL2;

    public static void init() throws JSONException, SQLException {
        AgentUtils.initDL1();
        AgentUtils.initDL2();
    }

    private static void initDL1() throws JSONException, SQLException {
        GameConfigDaoImpl dao = new GameConfigDaoImpl();
        JSONObject obj = new JSONObject(dao.getGameCommon("agent"));
        isOpen = obj.getInt("is_bonus") == 0;
        dsMin = (long)obj.getInt("ds_min") * 1000L;
        dsMin1 = (long)obj.getInt("ds_min_1") * 1000L;
        listBonusFix = new ArrayList<Long>();
        mapBonusMore = new HashMap<Long, Double>();
        JSONArray jArray = obj.getJSONArray("bonus_fix");
        if (jArray != null) {
            for (int i = 0; i < jArray.length(); ++i) {
                listBonusFix.add((long)jArray.getInt(i) * 1000L);
            }
        }
        HashMap<Integer, Integer> mapBonus = new HashMap<Integer, Integer>();
        JSONArray jArrayM = obj.getJSONArray("bonus_more");
        if (jArrayM != null) {
            for (int j = 0; j < jArrayM.length(); ++j) {
                JSONObject jObj = jArrayM.getJSONObject(j);
                Iterator keys = jObj.keys();
                while (keys.hasNext()) {
                    String key = (String)keys.next();
                    mapBonus.put(Integer.parseInt(key), jObj.getInt(key));
                }
            }
        }
        for (Map.Entry entry : mapBonus.entrySet()) {
            mapBonusMore.put((long)((Integer)entry.getKey()).intValue() * 1000L, (double)((Integer)entry.getValue()).intValue() / 100000.0);
        }
        mapBonusMore = MapUtils.sortMapDoubleByValue(mapBonusMore);
    }

    private static void initDL2() throws JSONException, SQLException {
        GameConfigDaoImpl dao = new GameConfigDaoImpl();
        JSONObject obj = new JSONObject(dao.getGameCommon("agent"));
        isOpenDL2 = obj.getInt("is_bonus_dl2") == 0;
        dsMinDL2 = (long)obj.getInt("ds_min_dl2") * 1000L;
        listBonusFixDL2 = new ArrayList<Long>();
        mapBonusMoreDL2 = new HashMap<Long, Double>();
        JSONArray jArray = obj.getJSONArray("bonus_fix_dl2");
        if (jArray != null) {
            for (int i = 0; i < jArray.length(); ++i) {
                listBonusFixDL2.add((long)jArray.getInt(i) * 1000L);
            }
        }
        HashMap<Integer, Integer> mapBonus = new HashMap<Integer, Integer>();
        JSONArray jArrayM = obj.getJSONArray("bonus_more_dl2");
        if (jArrayM != null) {
            for (int j = 0; j < jArrayM.length(); ++j) {
                JSONObject jObj = jArrayM.getJSONObject(j);
                Iterator keys = jObj.keys();
                while (keys.hasNext()) {
                    String key = (String)keys.next();
                    mapBonus.put(Integer.parseInt(key), jObj.getInt(key));
                }
            }
        }
        for (Map.Entry entry : mapBonus.entrySet()) {
            mapBonusMoreDL2.put((long)((Integer)entry.getKey()).intValue() * 1000L, (double)((Integer)entry.getValue()).intValue() / 100000.0);
        }
        mapBonusMoreDL2 = MapUtils.sortMapDoubleByValue(mapBonusMoreDL2);
    }

    public static void refundFeeAgent() throws Exception {
        List<String> lastTime = AgentUtils.getLastTime();
        String startTime = lastTime.get(0);
        String endTime = lastTime.get(1);
        String month = lastTime.get(2);
        AgentDAOImpl dao = new AgentDAOImpl();
        UserServiceImpl service = new UserServiceImpl();
        AlertServiceImpl alertSer = new AlertServiceImpl();
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        Map<String, ArrayList<String>> map = dao.getAllAgent();
        for (Map.Entry entry : map.entrySet()) {
            String agent1 = (String)entry.getKey();
            if (dao.checkRefundFeeAgent(agent1, month)) continue;
            ArrayList temp = (ArrayList)entry.getValue();
            String agent2 = (String)temp.get(0);
            String percent = (String)temp.get(1);
            ArrayList<String> listAgent = new ArrayList<String>();
            listAgent.add(agent1);
            if (!agent2.isEmpty()) {
                String[] arr;
                for (String agent3 : arr = agent2.split(",")) {
                    if (agent3.isEmpty()) continue;
                    listAgent.add(agent3);
                }
            }
            try {
                long feeVinplayCard;
                long feeVinplayCardTemp;
                double ratio1 = GameCommon.getValueDouble((String)"RATIO_REFUND_FEE_1");
                double ratio2 = GameCommon.getValueDouble((String)"RATIO_REFUND_FEE_2");
                double ratio2More = GameCommon.getValueDouble((String)"RATIO_REFUND_FEE_2_MORE");
                long minRefundFee2More = GameCommon.getValueLong((String)"REFUND_FEE_2_MORE");
                TranferAgentResponse tres = dao.searchAgentTranfer(agent1, "", startTime, endTime);
                long fee1 = tres.getTotalFeeBuy1() + tres.getTotalFeeSale1();
                long fee2 = 0L;
                long fee2More = 0L;
                long fee2Rf = 0L;
                for (String agent4 : listAgent) {
                    TranferAgentResponse tares = dao.searchAgentTranfer(agent4, "", startTime, endTime);
                    long fee3 = tares.getTotalFeeBuy2() + tares.getTotalFeeSale2();
                    long ds2 = tares.getTotalBuy2() + tares.getTotalSale2();
                    if (ds2 < minRefundFee2More) {
                        fee2 += fee3;
                        fee2Rf += Math.round((double)fee3 * ratio2);
                        continue;
                    }
                    fee2More += fee3;
                    fee2Rf += Math.round((double)fee3 * ratio2More);
                }
                long feeTotal = Math.round((double)fee1 * ratio1) + fee2Rf;
                long feeVinCash = feeTotal - (feeVinplayCard = AgentUtils.roundVinCard(feeTotal, feeVinplayCardTemp = (long)Math.round(feeTotal * (long)Integer.parseInt(percent) / 100L)));
                if (feeVinCash <= 0L) continue;
                BaseResponseModel mnres = service.updateMoneyFromAdmin(agent1, feeVinCash, "vin", "RefundFee", "Ho\u00e0n tr\u1ea3 ph\u00ed", "Ho\u00e0n tr\u1ea3 ph\u00ed \u0111\u1ea1i l\u00fd th\u00e1ng " + month);
                String description = mnres.isSuccess() ? "Th\u00e0nh c\u00f4ng" : "Th\u1ea5t b\u1ea1i";
                String desc = mnres.isSuccess() ? "Thanh cong" : "That bai";
                int code = Integer.parseInt(mnres.getErrorCode());
                RefundFeeAgentMessage msg = new RefundFeeAgentMessage(agent1, fee1, ratio1, fee2, ratio2, fee2More, ratio2More, feeTotal, month, code, description, feeVinplayCard, feeVinCash, Integer.parseInt(percent));
                MessageBusFactory.get("queue_dvt").publish("queue_dvt", (BaseMessage)msg, (int)306);
                if (mnres.isSuccess()) {
                    try {
                        UserCacheModel user;
                        IMap userMap = client.getMap("users");
                        if (userMap.containsKey((Object)agent1) && (user = (UserCacheModel)userMap.get((Object)agent1)).getMobile() != null && user.isHasMobileSecurity()) {
                            alertSer.sendSMS2One(user.getMobile(), "Tai khoan " + agent1 + " da nhan duoc " + NumberUtils.formatNumber((String)String.valueOf(feeVinCash)) + " vin do VinPlay hoan tra phi dai ly thang " + month, false);
                        }
                    }
                    catch (Exception e) {
                        logger.debug((Object)e);
                    }
                }
                try {
                    List receives = GameCommon.getPhoneAlert();
                    alertSer.sendSMS2List(receives, "Hoan tra phi dai ly thang " + month + ". Tai khoan: " + agent1 + ". Vin: " + NumberUtils.formatNumber((String)String.valueOf(feeVinCash)) + ". Ket qua: " + desc, false);
                }
                catch (Exception e) {
                    logger.debug((Object)e);
                }
            }
            catch (Exception e2) {
                logger.debug((Object)e2);
            }
        }
    }

    private static long roundVinCard(long moneyTotal, long moneyVinCard) {
        if (moneyVinCard < 10500L) {
            return 0L;
        }
        long division = moneyVinCard / 10500L;
        if ((division + 1L) * 10500L < moneyTotal) {
            return (division + 1L) * 10500L;
        }
        return division * 10500L;
    }

    public static void bonusTopDoanhSo() throws Exception {
        long moneyBonusMore;
        String agent5;
        BonusTopDSModel bonusModel;
        if (!isOpen) {
            return;
        }
        List<String> lastTime = AgentUtils.getLastTime();
        String startTime = lastTime.get(0);
        String endTime = lastTime.get(1);
        String month = lastTime.get(2);
        AgentDAOImpl dao = new AgentDAOImpl();
        UserServiceImpl service = new UserServiceImpl();
        AlertServiceImpl alertSer = new AlertServiceImpl();
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        Map<String, ArrayList<String>> map = dao.getAllAgent();
        HashMap<String, Long> hmapTopDS = new HashMap<String, Long>();
        HashMap<String, AgentDSModel> mapAgentDS = new HashMap<String, AgentDSModel>();
        HashMap<String, Long> hmapTopDS2 = new HashMap<String, Long>();
        HashMap<String, AgentDSModel> mapAgentDS2 = new HashMap<String, AgentDSModel>();
        HashMap<String, Long> mapDS1 = new HashMap<String, Long>();
        HashMap<String, BonusTopDSModel> mapBonus = new HashMap<String, BonusTopDSModel>();
        HashMap<String, String> percentMap = new HashMap<String, String>();
        for (Map.Entry entry : map.entrySet()) {
            String agent1 = (String)entry.getKey();
            ArrayList<String> temp = (ArrayList)entry.getValue();
            String agent2 = (String)temp.get(0);
            String percent = (String)temp.get(1);
            percentMap.put(agent1, percent);
            ArrayList<String> listAgent = new ArrayList<String>();
            if (!agent2.isEmpty()) {
                String[] arr;
                for (String agent3 : arr = ((String)((Object)agent2)).split(",")) {
                    if (agent3.isEmpty()) continue;
                    listAgent.add(agent3);
                }
            }
            AgentDSModel ds1 = dao.getDS(agent1, startTime, endTime, true);
            mapDS1.put(agent1, ds1.getDs());
            AgentDSModel dsTotal2 = new AgentDSModel();
            for (String agent4 : listAgent) {
                AgentDSModel ds2 = dao.getDS(agent4, startTime, endTime, false);
                ds1.setDs(ds1.getDs() + ds2.getDs());
                ds1.setDsMua(ds1.getDsMua() + ds2.getDsMua());
                ds1.setDsBan(ds1.getDsBan() + ds2.getDsBan());
                ds1.setGd(ds1.getGd() + ds2.getGd());
                ds1.setGdMua(ds1.getGdMua() + ds2.getGdMua());
                ds1.setGdBan(ds1.getGdBan() + ds2.getGdBan());
                dsTotal2.setDs(dsTotal2.getDs() + ds2.getDs());
                dsTotal2.setDsMua(dsTotal2.getDsMua() + ds2.getDsMua());
                dsTotal2.setDsBan(dsTotal2.getDsBan() + ds2.getDsBan());
                dsTotal2.setGd(dsTotal2.getGd() + ds2.getGd());
                dsTotal2.setGdMua(dsTotal2.getGdMua() + ds2.getGdMua());
                dsTotal2.setGdBan(dsTotal2.getGdBan() + ds2.getGdBan());
            }
            hmapTopDS.put(agent1, ds1.getDs());
            mapAgentDS.put(agent1, ds1);
            hmapTopDS2.put(agent1, dsTotal2.getDs());
            mapAgentDS2.put(agent1, dsTotal2);
        }
        HashMap<String, Long> hmapTopDSNotEnough = new HashMap();
        for (Map.Entry entry2 : hmapTopDS.entrySet()) {
            if ((Long)mapDS1.get(entry2.getKey()) >= dsMin1) continue;
            hmapTopDSNotEnough.put((String)entry2.getKey(), (Long) entry2.getValue());
        }
        for (Map.Entry entry2 : hmapTopDSNotEnough.entrySet()) {
            hmapTopDS.remove(entry2.getKey());
        }
        Map<String, Long> mapTopDS = AgentUtils.sortMapDS(hmapTopDS, mapAgentDS);
        Map<String, Long> mapTopDS2 = AgentUtils.sortMapDS(hmapTopDS2, mapAgentDS2);
        int i = 0;
        for (Map.Entry entry3 : mapTopDS.entrySet()) {
            agent5 = (String)entry3.getKey();
            long ds3 = ((AgentDSModel)mapAgentDS.get(agent5)).getDs();
            if (ds3 < dsMin) continue;
            long moneyBonusFix = 0L;
            moneyBonusMore = 0L;
            if (i < listBonusFix.size()) {
                moneyBonusFix = listBonusFix.get(i);
            }
            for (Map.Entry<Long, Double> et : mapBonusMore.entrySet()) {
                if (ds3 < et.getKey()) continue;
                moneyBonusMore = Math.round((double)ds3 * et.getValue());
                break;
            }
            if (moneyBonusFix > 0L || moneyBonusMore > 0L) {
                bonusModel = null;
                bonusModel = mapBonus.containsKey(agent5) ? (BonusTopDSModel)mapBonus.get(agent5) : new BonusTopDSModel();
                bonusModel.setNickname(agent5);
                bonusModel.setDs(ds3);
                bonusModel.setTop(i + 1);
                bonusModel.setBonusFix(moneyBonusFix);
                bonusModel.setBonusMore(moneyBonusMore);
                mapBonus.put(agent5, bonusModel);
            }
            ++i;
        }
        i = 0;
        for (Map.Entry<String, Long> entry3 : mapTopDS2.entrySet()) {
            agent5 = entry3.getKey();
            long ds3 = ((AgentDSModel)mapAgentDS2.get(agent5)).getDs();
            if (ds3 < dsMinDL2) continue;
            long moneyBonusFix = 0L;
            moneyBonusMore = 0L;
            if (i < listBonusFixDL2.size()) {
                moneyBonusFix = listBonusFixDL2.get(i);
            }
            for (Map.Entry<Long, Double> et : mapBonusMoreDL2.entrySet()) {
                if (ds3 < et.getKey()) continue;
                moneyBonusMore = Math.round((double)ds3 * et.getValue());
                break;
            }
            if (moneyBonusFix > 0L || moneyBonusMore > 0L) {
                bonusModel = null;
                bonusModel = mapBonus.containsKey(agent5) ? (BonusTopDSModel)mapBonus.get(agent5) : new BonusTopDSModel();
                bonusModel.setNickname(agent5);
                bonusModel.setDs2(ds3);
                bonusModel.setTop2(i + 1);
                bonusModel.setBonusFix2(moneyBonusFix);
                bonusModel.setBonusMore2(moneyBonusMore);
                mapBonus.put(agent5, bonusModel);
            }
            ++i;
        }
        for (Map.Entry entry4 : mapBonus.entrySet()) {
            agent5 = (String) entry4.getKey();
            BonusTopDSModel model = (BonusTopDSModel)entry4.getValue();
            if (dao.checkBonusTopDS(agent5, month)) continue;
            String percent2 = (String)percentMap.get(agent5);
            long moneyTotal = model.getBonusFix() + model.getBonusMore() + model.getBonusFix2() + model.getBonusMore2();
            long moneyVinplayCardTemp = Math.round(moneyTotal * (long)Integer.parseInt(percent2) / 100L);
            long moneyVinplayCard = AgentUtils.roundVinCard(moneyTotal, moneyVinplayCardTemp);
            long moneyVinCash = moneyTotal - moneyVinplayCard;
            StringBuilder des = new StringBuilder("Th\u01b0\u1edfng doanh s\u1ed1 \u0111\u1ea1i l\u00fd th\u00e1ng ").append(month).append(". Th\u01b0\u1edfng \u0111\u1ea1t DS: ").append(model.getBonusFix() + model.getBonusMore()).append(" vin");
            des.append(". Th\u01b0\u1edfng \u0111\u1ea1t DS c\u1ea5p 2: ").append(model.getBonusFix2() + model.getBonusMore2()).append(" vin");
            BaseResponseModel mnres = service.updateMoneyFromAdmin(agent5, moneyVinCash, "vin", "BonusTopDS", "Th\u01b0\u1edfng doanh s\u1ed1", des.toString());
            String description = mnres.isSuccess() ? "Th\u00e0nh c\u00f4ng" : "Th\u1ea5t b\u1ea1i";
            String desc = mnres.isSuccess() ? "Thanh cong" : "That bai";
            int code = Integer.parseInt(mnres.getErrorCode());
            model.setBonusTotal(moneyTotal);
            model.setMonth(month);
            model.setCode(code);
            model.setDescription(description);
            model.setTimeLog(VinPlayUtils.getCurrentDateTime());
            model.setBonusVinplayCard(moneyVinplayCard);
            model.setBonusVinCash(moneyVinCash);
            model.setPercent(Integer.parseInt(percent2));
            dao.logBonusTopDS(model);
            if (mnres.isSuccess()) {
                try {
                    UserCacheModel user;
                    IMap userMap = client.getMap("users");
                    if (userMap.containsKey((Object)agent5) && (user = (UserCacheModel)userMap.get((Object)agent5)).getMobile() != null && user.isHasMobileSecurity()) {
                        alertSer.sendSMS2One(user.getMobile(), "Tai khoan " + agent5 + " da nhan duoc " + NumberUtils.formatNumber((String)String.valueOf(moneyVinCash)) + " vin do VinPlay thuong doanh so dai ly thang " + month, false);
                    }
                }
                catch (Exception e) {
                    logger.debug((Object)e);
                }
            }
            try {
                List receives = GameCommon.getPhoneAlert();
                alertSer.sendSMS2List(receives, "Thuong doanh so dai ly thang " + month + ". Tai khoan: " + agent5 + ". Vin: " + NumberUtils.formatNumber((String)String.valueOf(moneyVinCash)) + ". Ket qua: " + desc, false);
            }
            catch (Exception e) {
                logger.debug((Object)e);
            }
        }
    }

    public static Date getFirstDayAfterMonth() {
        Calendar c = Calendar.getInstance();
        c.add(2, 1);
        c.set(5, 1);
        c.set(11, 15);
        c.set(12, 0);
        c.set(13, 0);
        Date date = c.getTime();
        return date;
    }

    public static List<String> getLastTime() {
        ArrayList<String> res = new ArrayList<String>();
        Calendar aCalendar = Calendar.getInstance();
        aCalendar.add(2, -1);
        aCalendar.set(5, 1);
        Date firstDateOfPreviousMonth = aCalendar.getTime();
        aCalendar.set(5, aCalendar.getActualMaximum(5));
        Date lastDateOfPreviousMonth = aCalendar.getTime();
        SimpleDateFormat startFormat = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
        SimpleDateFormat endFormat = new SimpleDateFormat("yyyy-MM-dd 23:59:59");
        String startTime = startFormat.format(firstDateOfPreviousMonth);
        String endTime = endFormat.format(lastDateOfPreviousMonth);
        String month = String.valueOf(aCalendar.get(2) + 1) + "/" + aCalendar.get(1);
        res.add(startTime);
        res.add(endTime);
        res.add(month);
        return res;
    }

    public static Map<String, Long> sortMapDS(HashMap<String, Long> hmapTopDS, Map<String, AgentDSModel> mapAgentDS) {
        for (int i = 0; i <= hmapTopDS.size(); ++i) {
            for (Map.Entry<String, Long> entry : hmapTopDS.entrySet()) {
                for (Map.Entry<String, Long> entry2 : hmapTopDS.entrySet()) {
                    String key1 = entry.getKey();
                    String key2 = entry2.getKey();
                    long value1 = entry.getValue();
                    long value2 = entry2.getValue();
                    if (key1.equals(key2) || value1 != value2) continue;
                    AgentDSModel ds1 = mapAgentDS.get(entry.getKey());
                    AgentDSModel ds2 = mapAgentDS.get(entry2.getKey());
                    if (ds1.getDsBan() > ds2.getDsBan()) {
                        hmapTopDS.put(entry.getKey(), entry.getValue() + 1L);
                        continue;
                    }
                    if (ds1.getDsBan() < ds2.getDsBan()) {
                        hmapTopDS.put(entry2.getKey(), entry2.getValue() + 1L);
                        continue;
                    }
                    if (ds1.getGd() > ds2.getGd()) {
                        hmapTopDS.put(entry.getKey(), entry.getValue() + 1L);
                        continue;
                    }
                    if (ds1.getGd() < ds2.getGd()) {
                        hmapTopDS.put(entry2.getKey(), entry2.getValue() + 1L);
                        continue;
                    }
                    if (ds1.getGdBan() > ds2.getGdBan()) {
                        hmapTopDS.put(entry.getKey(), entry.getValue() + 1L);
                        continue;
                    }
                    if (ds1.getGdBan() >= ds2.getGdBan()) continue;
                    hmapTopDS.put(entry2.getKey(), entry2.getValue() + 1L);
                }
            }
        }
        return MapUtils.sortMapByValue2(hmapTopDS);
    }

    static {
        isOpen = false;
        isOpenDL2 = false;
    }
}

