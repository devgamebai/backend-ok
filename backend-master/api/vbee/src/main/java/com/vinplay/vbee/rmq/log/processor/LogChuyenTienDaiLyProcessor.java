/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.dal.dao.impl.AgentDAOImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.LogChuyenTienDaiLyMessage
 *  com.vinplay.vbee.common.models.cache.AgentDSModel
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 */
package com.vinplay.vbee.rmq.log.processor;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.LogChuyenTienDaiLyMessage;
import com.vinplay.vbee.common.models.cache.AgentDSModel;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import com.vinplay.vbee.dao.impl.LogMoneyUserDaoImpl;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class LogChuyenTienDaiLyProcessor
implements BaseProcessor<byte[], Boolean> {
    public Boolean execute(Param<byte[]> param) {
        String startTime;
        AgentDAOImpl agentDao;
        AgentDSModel dsAgent2;
        List lastTime;
        LogChuyenTienDaiLyMessage message;
        String endTime;
        long feeMua;
        String keyReceive;
        IMap dsMap;
        Calendar aCalendar;
        int gdMua;
        AgentDSModel dsAgent;
        boolean bAgent1;
        long dsMua;
        block34 : {
            message = (LogChuyenTienDaiLyMessage)BaseMessage.fromBytes((byte[])((byte[])param.get()));
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            String month = "";
            try {
                month = DateTimeUtils.getFormatTime((String)"MM/yyyy", (Date)VinPlayUtils.getDateTime((String)message.getTransTime()));
            }
            catch (ParseException e3) {
                month = DateTimeUtils.getCurrentTime((String)"MM/yyyy");
            }
            LogMoneyUserDaoImpl dao = new LogMoneyUserDaoImpl();
            dao.logChuyenTienDaiLy(message);
            try {
                dao.logChuyenTienDaiLyMySQL(message);
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
            dsMap = client.getMap("cacheDsAgent");
            String keySend = null;
            keyReceive = null;
            dsMua = 0L;
            long dsBan = 0L;
            feeMua = 0L;
            long feeBan = 0L;
            gdMua = 0;
            int gdBan = 0;
            bAgent1 = false;
            switch (message.getStatus()) {
                case 1: {
                    keyReceive = message.getNicknameReceive() + "," + month;
                    gdMua = 1;
                    dsMua = message.getMoneySend();
                    feeMua = message.getFee();
                    bAgent1 = true;
                    break;
                }
                case 2: {
                    keyReceive = message.getNicknameReceive() + "," + month;
                    gdMua = 1;
                    dsMua = message.getMoneySend();
                    feeMua = message.getFee();
                    bAgent1 = false;
                    break;
                }
                case 3: {
                    keySend = message.getNicknameSend() + "," + month;
                    gdBan = 1;
                    dsBan = message.getMoneySend();
                    feeBan = message.getFee();
                    bAgent1 = true;
                    break;
                }
                case 4: {
                    keySend = message.getNicknameSend() + "," + month;
                    feeBan = message.getFee();
                    bAgent1 = true;
                    break;
                }
                case 5: {
                    keySend = message.getNicknameSend() + "," + month;
                    feeBan = message.getFee();
                    bAgent1 = true;
                    break;
                }
                case 6: {
                    keySend = message.getNicknameSend() + "," + month;
                    gdBan = 1;
                    dsBan = message.getMoneySend();
                    feeBan = message.getFee();
                    bAgent1 = false;
                    break;
                }
                case 7: {
                    keyReceive = message.getNicknameReceive() + "," + month;
                    feeMua = message.getFee();
                    bAgent1 = true;
                    break;
                }
                case 8: {
                    keySend = message.getNicknameSend() + "," + month;
                    feeBan = message.getFee();
                    bAgent1 = false;
                }
            }
            agentDao = new AgentDAOImpl();
            if (keySend != null) {
                if (dsMap.containsKey(keySend)) {
                    if (dsBan > 0L || gdBan > 0 || feeBan > 0L) {
                        try {
                            dsMap.lock((Object)keySend);
                            dsAgent = (AgentDSModel)dsMap.get((Object)keySend);
                            dsAgent.setDsBan(dsAgent.getDsBan() + dsBan);
                            dsAgent.setGdBan(dsAgent.getGdBan() + gdBan);
                            dsAgent.setFeeBan(dsAgent.getFeeBan() + feeBan);
                            dsAgent.setDs(dsAgent.getDs() + dsBan);
                            dsAgent.setGd(dsAgent.getGd() + gdBan);
                            dsAgent.setFee(dsAgent.getFee() + feeBan);
                            dsMap.put((Object)keySend, (Object)dsAgent);
                        }
                        catch (Exception e2) {
                            e2.printStackTrace();
                            break block34;
                        }
                        try {
                            dsMap.unlock((Object)keySend);
                        }
                        catch (Exception e2) {}
                    }
                } else {
                    try {
                        aCalendar = Calendar.getInstance();
                        aCalendar.setTime(VinPlayUtils.getDateTime((String)message.getTransTime()));
                        lastTime = DateTimeUtils.getLastTime((Calendar)aCalendar);
                        startTime = (String)lastTime.get(0);
                        endTime = (String)lastTime.get(1);
                        dsAgent2 = agentDao.getDS(message.getNicknameSend(), startTime, endTime, bAgent1);
                        dsAgent2.setDsBan(dsAgent2.getDsBan() + dsBan);
                        dsAgent2.setGdBan(dsAgent2.getGdBan() + gdBan);
                        dsAgent2.setFeeBan(dsAgent2.getFeeBan() + feeBan);
                        dsAgent2.setDs(dsAgent2.getDs() + dsBan);
                        dsAgent2.setGd(dsAgent2.getGd() + gdBan);
                        dsAgent2.setFee(dsAgent2.getFee() + feeBan);
                        dsMap.put((Object)keySend, (Object)dsAgent2);
                    }
                    catch (Exception e2) {
                        e2.printStackTrace();
                    }
                }
            }
        }
        if (keyReceive != null) {
            if (dsMap.containsKey(keyReceive)) {
                if (dsMua <= 0L && gdMua <= 0 && feeMua <= 0L) {
                    return true;
                }
                try {
                    dsMap.lock((Object)keyReceive);
                    dsAgent = (AgentDSModel)dsMap.get((Object)keyReceive);
                    dsAgent.setDsMua(dsAgent.getDsMua() + dsMua);
                    dsAgent.setGdMua(dsAgent.getGdMua() + gdMua);
                    dsAgent.setFeeMua(dsAgent.getFeeMua() + feeMua);
                    dsAgent.setDs(dsAgent.getDs() + dsMua);
                    dsAgent.setGd(dsAgent.getGd() + gdMua);
                    dsAgent.setFee(dsAgent.getFee() + feeMua);
                    dsMap.put((Object)keyReceive, (Object)dsAgent);
                }
                catch (Exception e2) {
                    e2.printStackTrace();
                    return true;
                }
                try {
                    dsMap.unlock((Object)keyReceive);
                }
                catch (Exception e2) {}
            } else {
                try {
                    aCalendar = Calendar.getInstance();
                    aCalendar.setTime(VinPlayUtils.getDateTime((String)message.getTransTime()));
                    lastTime = DateTimeUtils.getLastTime((Calendar)aCalendar);
                    startTime = (String)lastTime.get(0);
                    endTime = (String)lastTime.get(1);
                    dsAgent2 = agentDao.getDS(message.getNicknameReceive(), startTime, endTime, bAgent1);
                    dsAgent2.setDsMua(dsAgent2.getDsMua() + dsMua);
                    dsAgent2.setGdMua(dsAgent2.getGdMua() + gdMua);
                    dsAgent2.setFeeMua(dsAgent2.getFeeMua() + feeMua);
                    dsAgent2.setDs(dsAgent2.getDs() + dsMua);
                    dsAgent2.setGd(dsAgent2.getGd() + gdMua);
                    dsAgent2.setFee(dsAgent2.getFee() + feeMua);
                    dsMap.put((Object)keyReceive, (Object)dsAgent2);
                }
                catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
        return true;
    }
}

