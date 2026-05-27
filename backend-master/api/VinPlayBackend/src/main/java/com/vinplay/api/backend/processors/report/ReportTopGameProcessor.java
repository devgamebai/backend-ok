/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.dal.dao.impl.ReportDaoImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.TopCaoThu
 *  com.vinplay.vbee.common.models.cache.ReportModel
 *  com.vinplay.vbee.common.statics.Consts
 *  com.vinplay.vbee.common.utils.MapUtils
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors.report;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.api.backend.response.ReportTopGameResponse;
import com.vinplay.dal.dao.impl.ReportDaoImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.TopCaoThu;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.ReportModel;
import com.vinplay.vbee.common.statics.Consts;
import com.vinplay.vbee.common.utils.MapUtils;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class ReportTopGameProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"report");

    public String execute(Param<HttpServletRequest> param) {
        ReportTopGameResponse res;
        block38 : {
            String ALL_GAME = "all";
            HttpServletRequest request = (HttpServletRequest)param.get();
            String action = request.getParameter("ac");
            String numStr = request.getParameter("n");
            String startTime = request.getParameter("ts");
            String endTime = request.getParameter("te");
            res = new ReportTopGameResponse(false, "1001");
            try {
                if (numStr == null || numStr.isEmpty()) numStr = "20";
                int num = Integer.parseInt(numStr);
                if (startTime == null || startTime.isEmpty()) startTime = VinPlayUtils.getCurrentDate();
                if (endTime == null || endTime.isEmpty()) endTime = startTime;
                if (action == null || action.isEmpty()) action = "all";
                TopCaoThu top;
                HashMap map = new HashMap();
                HashMap mapBot = new HashMap();
                HazelcastInstance client = HazelcastClientFactory.getInstance();
                IMap<String, ReportModel> reportMap = client.getMap("cacheReports");
                String today = VinPlayUtils.getCurrentDate();
                ReportDaoImpl dao = new ReportDaoImpl();
                SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
                Date st = format.parse(startTime);
                Date et = format.parse(endTime);
                Date currentDate = VinPlayUtils.getCurrentDates();
                if (et.getTime() >= st.getTime()) {
                    if (st.getTime() >= currentDate.getTime()) {
                        for (Map.Entry entry : reportMap.entrySet()) {
                            if (!((String)entry.getKey()).contains(today)) continue;
                            String[] arr = ((String)entry.getKey()).split(",");
                            String nickname = arr[0];
                            String actionname = arr[1];
                            if (!actionname.equals(action) && (!action.equals("all") || !Consts.GAMES.contains(actionname))) continue;
                            ReportModel model = (ReportModel)entry.getValue();
                            long value = 0L;
                            if (!model.isBot) {
                                if (map.containsKey(nickname)) {
                                    value = (Long)map.get(nickname);
                                }
                                map.put(nickname, value += model.moneyWin + model.moneyLost + model.moneyOther);
                                continue;
                            }
                            if (mapBot.containsKey(nickname)) {
                                value = (Long)mapBot.get(nickname);
                            }
                            mapBot.put(nickname, value += model.moneyWin + model.moneyLost + model.moneyOther);
                        }
                    } else if (et.getTime() < currentDate.getTime()) {
                        if (!action.equals("all")) {
                            map = dao.getReportTopGame(startTime, endTime, action, false);
                            mapBot = dao.getReportTopGame(startTime, endTime, action, true);
                        } else {
                            for (String actionGame : Consts.GAMES) {
                                HashMap<String, Long> mapG = dao.getReportTopGame(startTime, endTime, actionGame, false);
                                HashMap<String, Long> mapBotG = dao.getReportTopGame(startTime, endTime, actionGame, true);
                                for (Map.Entry entry2 : mapG.entrySet()) {
                                    if (map.containsKey(entry2.getKey())) {
                                        map.put(entry2.getKey(), (Long)map.get(entry2.getKey()) + (Long)entry2.getValue());
                                        continue;
                                    }
                                    map.put(entry2.getKey(), entry2.getValue());
                                }
                                for (Map.Entry entry2 : mapBotG.entrySet()) {
                                    if (mapBot.containsKey(entry2.getKey())) {
                                        mapBot.put(entry2.getKey(), (Long)mapBot.get(entry2.getKey()) + (Long)entry2.getValue());
                                        continue;
                                    }
                                    mapBot.put(entry2.getKey(), entry2.getValue());
                                }
                            }
                        }
                    } else if (et.getTime() >= currentDate.getTime()) {
                        String yesterday = VinPlayUtils.getYesterday();
                        if (!action.equals("all")) {
                            map = dao.getReportTopGame(startTime, yesterday, action, false);
                            mapBot = dao.getReportTopGame(startTime, yesterday, action, true);
                        } else {
                            for (String actionGame2 : Consts.GAMES) {
                                HashMap<String, Long> mapG2 = dao.getReportTopGame(startTime, endTime, actionGame2, false);
                                HashMap<String, Long> mapBotG2 = dao.getReportTopGame(startTime, endTime, actionGame2, true);
                                for (Map.Entry entry3 : mapG2.entrySet()) {
                                    if (map.containsKey(entry3.getKey())) {
                                        map.put(entry3.getKey(), (Long)map.get(entry3.getKey()) + (Long)entry3.getValue());
                                        continue;
                                    }
                                    map.put(entry3.getKey(), entry3.getValue());
                                }
                                for (Map.Entry entry3 : mapBotG2.entrySet()) {
                                    if (mapBot.containsKey(entry3.getKey())) {
                                        mapBot.put(entry3.getKey(), (Long)mapBot.get(entry3.getKey()) + (Long)entry3.getValue());
                                        continue;
                                    }
                                    mapBot.put(entry3.getKey(), entry3.getValue());
                                }
                            }
                        }
                        for (Map.Entry entry4 : reportMap.entrySet()) {
                            if (!((String)entry4.getKey()).contains(today)) continue;
                            String[] arr2 = ((String)entry4.getKey()).split(",");
                            String nickname2 = arr2[0];
                            String actionname2 = arr2[1];
                            if (!actionname2.equals(action) && (!action.equals("all") || !Consts.GAMES.contains(actionname2))) continue;
                            ReportModel model2 = (ReportModel)entry4.getValue();
                            long value2 = 0L;
                            if (!model2.isBot) {
                                if (map.containsKey(nickname2)) {
                                    value2 = (Long)map.get(nickname2);
                                }
                                map.put(nickname2, value2 += model2.moneyWin + model2.moneyLost + model2.moneyOther);
                                continue;
                            }
                            if (mapBot.containsKey(nickname2)) {
                                value2 = (Long)mapBot.get(nickname2);
                            }
                            mapBot.put(nickname2, value2 += model2.moneyWin + model2.moneyLost + model2.moneyOther);
                        }
                    }
                }
                TreeMap<String, UserModel> sortUserWin = MapUtils.sortMapByValue(map);
                TreeMap<String, UserModel>  sortUserLost = MapUtils.sortMapByValueAsc(map);
                TreeMap<String, UserModel>  sortBotWin = MapUtils.sortMapByValue(mapBot);
                TreeMap<String, UserModel>  sortBotLost = MapUtils.sortMapByValueAsc(mapBot);
                ArrayList<TopCaoThu> topUserWin = new ArrayList<TopCaoThu>();
                ArrayList<TopCaoThu> topUserLost = new ArrayList<TopCaoThu>();
                ArrayList<TopCaoThu> topBotWin = new ArrayList<TopCaoThu>();
                ArrayList<TopCaoThu> topBotLost = new ArrayList<TopCaoThu>();
                int n = 0;
                for (Map.Entry entry5 : sortUserWin.entrySet()) {
                    if ((Long)entry5.getValue() >= 0L) {
                        top = new TopCaoThu((String)entry5.getKey(), ((Long)entry5.getValue()).longValue());
                        topUserWin.add(top);
                    }
                    if (++n < num) continue;
                    break;
                }
                n = 0;
                for (Map.Entry entry5 : sortUserLost.entrySet()) {
                    if ((Long)entry5.getValue() <= 0L) {
                        top = new TopCaoThu((String)entry5.getKey(), ((Long)entry5.getValue()).longValue());
                        topUserLost.add(top);
                    }
                    if (++n < num) continue;
                    break;
                }
                n = 0;
                for (Map.Entry entry5 : sortBotWin.entrySet()) {
                    if ((Long)entry5.getValue() >= 0L) {
                        top = new TopCaoThu((String)entry5.getKey(), ((Long)entry5.getValue()).longValue());
                        topBotWin.add(top);
                    }
                    if (++n < num) continue;
                    break;
                }
                n = 0;
                for (Map.Entry entry5 : sortBotLost.entrySet()) {
                    if ((Long)entry5.getValue() <= 0L) {
                        top = new TopCaoThu((String)entry5.getKey(), ((Long)entry5.getValue()).longValue());
                        topBotLost.add(top);
                    }
                    if (++n < num) continue;
                    break;
                }
                res.topUserWin = topUserWin;
                res.topUserLost = topUserLost;
                res.topBotWin = topBotWin;
                res.topBotLost = topBotLost;
                res.setErrorCode("0");
                res.setSuccess(true);
            }
            catch (Exception e) {
                logger.debug((Object)e);
            }
        }
        return res.toJson();
    }
}

