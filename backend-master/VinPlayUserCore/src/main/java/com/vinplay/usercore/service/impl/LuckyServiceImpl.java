/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.enums.Vippoint
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.LogMoneyUserMessage
 *  com.vinplay.vbee.common.messages.LuckyMessage
 *  com.vinplay.vbee.common.messages.MoneyMessageInMinigame
 *  com.vinplay.vbee.common.models.cache.SlotFreeDaily
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.response.LuckyResponse
 *  com.vinplay.vbee.common.response.LuckyVipResponse
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  com.vinplay.vbee.common.statics.TransType
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.usercore.service.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.usercore.dao.impl.LuckyDaoImpl;
import com.vinplay.usercore.entities.vqmm.LuckyHistory;
import com.vinplay.usercore.entities.vqmm.LuckyVipHistory;
import com.vinplay.usercore.logger.MoneyLogger;
import com.vinplay.usercore.service.LuckyService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.usercore.utils.LuckyUtils;
import com.vinplay.usercore.utils.VippointUtils;
import com.vinplay.vbee.common.enums.Vippoint;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.messages.LuckyMessage;
import com.vinplay.vbee.common.messages.MoneyMessageInMinigame;
import com.vinplay.vbee.common.models.cache.SlotFreeDaily;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.LuckyResponse;
import com.vinplay.vbee.common.response.LuckyVipResponse;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.rmq.RMQApi;
import com.vinplay.vbee.common.statics.TransType;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;

public class LuckyServiceImpl
implements LuckyService {
    private static final Logger logger = Logger.getLogger((String)"user_core");

    @Override
    public int receiveRotateDaily(int userId, String nickname) throws SQLException {
        LuckyDaoImpl dao = new LuckyDaoImpl();
        return dao.receiveRotateDaily(userId, nickname);
    }

    @Override
    public List<LuckyHistory> getLuckyHistory(String nickname, int page) {
        LuckyDaoImpl dao = new LuckyDaoImpl();
        return dao.getLuckyHistory(nickname, page);
    }

    @Override
    public List<LuckyVipHistory> getLuckyVipHistory(String nickname, int page) {
        LuckyDaoImpl dao = new LuckyDaoImpl();
        return dao.getLuckyVipHistory(nickname, page);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public LuckyResponse getResultLuckyRotation(String nickname, String ipAddress) {
        LuckyResponse response = new LuckyResponse(false, "1001");
        String resultVin = "fail";
        String resultSlot = "fail";
        String resultXu = "fail";
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey(nickname)) {
            try {
                 userMap.lock(nickname);
                UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                long moneyVin = user.getVin();
                long moneyXu = user.getXu();
                long currentMoneyVin = user.getVinTotal();
                long currentMoneyXu = user.getXuTotal();
                long moneyExchangeXu = 0L;
                long moneyExchangeVin = 0L;
                int slotFree = 0;
                String slotName = "";
                LuckyDaoImpl dao = new LuckyDaoImpl();
                List<Integer> rotateLst = dao.getRotateCount(user.getId(), ipAddress);
                int rotateDaily = rotateLst.get(0);
                int rotateFree = rotateLst.get(1);
                int rotateInDay = rotateLst.get(2);
                int rotateByIp = rotateLst.get(3);
                int maxInDay = GameCommon.getValueInt("LUCKY_MAX_IN_DAY");
                int maxByIP = GameCommon.getValueInt("LUCKY_MAX_BY_IP");
                if (rotateDaily + rotateFree > 0) {
                    if (rotateInDay < maxInDay) {
                        int userType = LuckyUtils.getUserType(user.isHasMobileSecurity(), user.getRechargeMoney());
                        resultXu = LuckyUtils.getResultLucky(LuckyUtils.generateResult(), 2, userType);
                        if (rotateByIp < maxByIP) {
                            resultVin = LuckyUtils.getResultLucky(LuckyUtils.generateResult(), 1, userType);
                            resultSlot = LuckyUtils.getResultLucky(LuckyUtils.generateResult(), 3, userType);
                        }
                        if (!(resultVin.equals("more") || resultSlot.equals("more") || resultXu.equals("more"))) {
                            if (rotateDaily > 0) {
                                --rotateDaily;
                            } else {
                                --rotateFree;
                            }
                            ++rotateInDay;
                        }
                        response.setRotateCount(rotateDaily + rotateFree);
                        if (!resultVin.equals("more") && !resultVin.equals("fail")) {
                            moneyExchangeVin = Long.parseLong(resultVin);
                            ++rotateByIp;
                        }
                        if (!resultSlot.equals("more") && !resultSlot.equals("fail")) {
                            slotName = resultSlot.substring(0, resultSlot.length() - 1);
                            slotFree += Integer.parseInt(resultSlot.substring(resultSlot.length() - 1));
                            ++rotateByIp;
                        }
                        if (!resultXu.equals("more") && !resultXu.equals("fail")) {
                            moneyExchangeXu += Long.parseLong(resultXu);
                        }
                        int slotMaxWin = GameCommon.getValueInt("LUCKY_SLOT_MAX_WIN");
                        int slotRoom = GameCommon.getValueInt("LUCKY_SLOT_ROOM");
                        if (dao.saveResultLucky(user.getId(), nickname, ipAddress, rotateDaily, rotateFree, rotateInDay, rotateByIp, slotFree, slotName, slotMaxWin, slotRoom)) {
                            LuckyMessage message = new LuckyMessage(user.getId(), nickname, resultVin, resultXu, resultSlot);
                            MessageBusFactory.get("queue_vqmm").publish("queue_vqmm", (BaseMessage)message, (int)105);
                            if (moneyExchangeVin > 0L || moneyExchangeXu > 0L) {
                                LogMoneyUserMessage messageLog;
                                if (moneyExchangeVin > 0L) {
                                    user.setVin(moneyVin += moneyExchangeVin);
                                    user.setVinTotal(currentMoneyVin += moneyExchangeVin);
                                    MoneyMessageInMinigame messageVin = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, "VQMM", moneyVin, currentMoneyVin, moneyExchangeVin, "vin", 0L, 0, 0);
                                    messageLog = new LogMoneyUserMessage(user.getId(), nickname, "VQMM", "VQMM - Tr\u1ea3 th\u01b0\u1edfng", currentMoneyVin, moneyExchangeVin, "vin", "Ng\u00e0y " + VinPlayUtils.getCurrentDate(), 0L, false, user.isBot());
                                    messageLog.setReferralCode(user.getReferralCode());
                                    MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageVin, (int)16);
                                    MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                                }
                                if (moneyExchangeXu > 0L) {
                                    user.setXu(moneyXu += moneyExchangeXu);
                                    user.setXuTotal(currentMoneyXu += moneyExchangeXu);
                                    MoneyMessageInMinigame messageXu = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, "VQMM", moneyXu, currentMoneyXu, moneyExchangeXu, "xu", 0L, 0, 0);
                                    messageLog = new LogMoneyUserMessage(user.getId(), nickname, "VQMM", "VQMM - Tr\u1ea3 th\u01b0\u1edfng", currentMoneyXu, moneyExchangeXu, "xu", "Ng\u00e0y " + VinPlayUtils.getCurrentDate(), 0L, false, user.isBot());
                                    messageLog.setReferralCode(user.getReferralCode());
                                    MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageXu, (int)16);
                                    MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                                }
                                userMap.put(nickname, user);
                            }
                            if (slotFree > 0) {
                                String key;
                                IMap slotMap = client.getMap("cacheSlotFree");
                                if (slotMap.containsKey((key = nickname + "-" + slotName + "-" + slotRoom))) {
                                    SlotFreeDaily slotModel = (SlotFreeDaily)slotMap.get(key);
                                    slotModel.setRotateFree(slotModel.getRotateFree() + slotFree);
                                    slotMap.put(key, slotModel);
                                } else {
                                    SlotFreeDaily slotModel = new SlotFreeDaily(slotFree, (long)slotMaxWin);
                                    slotMap.put(key, slotModel);
                                }
                            }
                            response.setErrorCode("0");
                            response.setSuccess(true);
                        }
                    } else {
                        response.setErrorCode("3002");
                    }
                } else {
                    response.setErrorCode("3001");
                }
                response.setCurrentMoneyVin(currentMoneyVin);
                response.setCurrentMoneyXu(currentMoneyXu);
            }
            catch (Exception e) {
                logger.debug(e);
                MoneyLogger.log(nickname, "VQMM", 0L, 0L, "vin/xu", "vong quay may man", "1001", "error: " + e.getMessage());
            }
            finally {
                 userMap.unlock(nickname);
            }
        }
        response.setResultVin(resultVin);
        response.setResultXu(resultXu);
        response.setResultSlot(resultSlot);
        return response;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public LuckyVipResponse rotateLuckyVip(String nickname, boolean check) {
        LuckyVipResponse res;
        block17 : {
            res = new LuckyVipResponse(false, "1001");
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            IMap<String, UserModel> userMap = client.getMap("users");
            LuckyDaoImpl dao = new LuckyDaoImpl();
            if (userMap.containsKey(nickname)) {
                try {
                     userMap.lock(nickname);
                    UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                    int level = VippointUtils.getLevel(user.getVippointSave());
                    int userType = LuckyUtils.getUserVipType(level);
                    if (level >= Vippoint.VANG.getId()) {
                        Calendar c = Calendar.getInstance();
                        c.setTime(new Date());
                        String month = String.valueOf(c.get(2) + 1) + "/" + c.get(1);
                        int maxDayOfMonth = c.getActualMaximum(5);
                        int dayInMonth = c.get(5);
                        int rotateInMonth = dao.getLuckyVipInMonth(nickname, month);
                        int numRemain = LuckyUtils.getNumLuckyVip(userType) - rotateInMonth;
                        if (numRemain > 0) {
                            if (user.getBirthday() != null) {
                                c.setTime(VinPlayUtils.getDateTimeFromDate((String)user.getBirthday()));
                                int bdInMonth = c.get(5);
                                if (bdInMonth == dayInMonth || bdInMonth >= maxDayOfMonth && dayInMonth == maxDayOfMonth) {
                                    if (!check) {
                                        IMap map;
                                        HazelcastInstance instance;
                                        int resultVin = LuckyUtils.getResultLuckyVip(LuckyUtils.generateResult(), 1, userType);
                                        int resultMulti = LuckyUtils.getResultLuckyVip(LuckyUtils.generateResult(), 4, userType);
                                        if (resultVin <= 0 || resultMulti <= 0 || !(map = (instance = HazelcastClientFactory.getInstance()).getMap("cacheConfig")).containsKey("LUCKY_VIP_ID")) break block17;
                                        try {
                                            map.lock("LUCKY_VIP_ID");
                                            long transId = Long.parseLong((String)map.get("LUCKY_VIP_ID"));
                                            if (dao.logLuckyVip(++transId, nickname, month, resultVin, resultMulti)) {
                                                map.put("LUCKY_VIP_ID", String.valueOf(transId));
                                                res.setResultVin(resultVin);
                                                res.setResultMulti(resultMulti);
                                                res.setRotateCount(numRemain - 1);
                                                res.setSuccess(true);
                                                res.setErrorCode("0");
                                                UserServiceImpl userSer = new UserServiceImpl();
                                                long money = resultVin * resultMulti;
                                                MoneyResponse mnres = userSer.updateMoney(nickname, money, "vin", "VQVIP", "V\u00f2ng quay VIP", "K\u1ebft qu\u1ea3: " + resultVin + " (Vin) X" + resultMulti, 0L, null, TransType.NO_VIPPOINT);
                                                res.setCurrentMoney(mnres.getCurrentMoney());
                                            }
                                            break block17;
                                        }
                                        catch (Exception e) {
                                            logger.debug(e);
                                            break block17;
                                        }
                                        finally {
                                            map.unlock("LUCKY_VIP_ID");
                                        }
                                    }
                                    res.setRotateCount(numRemain);
                                    res.setSuccess(true);
                                    res.setErrorCode("0");
                                    break block17;
                                }
                                res.setErrorCode("1203");
                                break block17;
                            }
                            res.setErrorCode("1202");
                            break block17;
                        }
                        res.setErrorCode("1204");
                        break block17;
                    }
                    res.setErrorCode("1201");
                }
                catch (Exception e2) {
                    logger.debug(e2);
                    MoneyLogger.log(nickname, "VQVIP", 0L, 0L, "vin", "vong quay vip", "1001", "error: " + e2.getMessage());
                }
                finally {
                     userMap.unlock(nickname);
                }
            }
        }
        return res;
    }
}

