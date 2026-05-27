/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.dal.dao.impl.ReportDaoImpl
 *  com.vinplay.dal.entities.report.ReportMoneySystemModel
 *  com.vinplay.dal.entities.report.ReportTXModel
 *  com.vinplay.dal.entities.report.ReportTotalMoneyModel
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.cache.ReportModel
 *  com.vinplay.vbee.common.statics.Consts
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors.report;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.api.backend.response.DashboardModel;
import com.vinplay.api.backend.response.ReportMoneySystemResponse;
import com.vinplay.api.backend.response.TopUserModel;
import com.vinplay.dal.dao.impl.GetUserIndexDAOImpl;
import com.vinplay.dal.dao.impl.ReportDaoImpl;
import com.vinplay.dal.entities.report.ReportMoneySystemModel;
import com.vinplay.dal.entities.report.ReportTXModel;
import com.vinplay.dal.entities.report.ReportTotalMoneyModel;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.ReportModel;
import com.vinplay.vbee.common.statics.Consts;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class ReportMoneySystemProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"report");

    public String execute(Param<HttpServletRequest> param) {
        ReportMoneySystemResponse res;
        block29 : {
            HttpServletRequest request = (HttpServletRequest)param.get();
            String startTime = request.getParameter("ts");
            String endTime = request.getParameter("te");
            res = new ReportMoneySystemResponse(false, "1001");
            try {
                if (startTime == null || startTime.isEmpty() || endTime == null || endTime.isEmpty()) {
                    startTime = VinPlayUtils.getCurrentDate();
                    endTime = startTime;
                }
                boolean endToday = true;
                Map<String, ReportMoneySystemModel> map = new HashMap();
                Map<String, ReportMoneySystemModel> mapBot = new HashMap();
                String today = VinPlayUtils.getCurrentDate();
                HazelcastInstance client = HazelcastClientFactory.getInstance();
                ReportDaoImpl dao = new ReportDaoImpl();
                SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
                Date st = format.parse(startTime);
                Date et = format.parse(endTime);
                Date currentDate = VinPlayUtils.getCurrentDates();
                if (et.getTime() >= st.getTime()) {
                    if (st.getTime() >= currentDate.getTime()) {
                        IMap<String, ReportModel> reportMap = client.getMap("cacheReports");
                        for (IMap.Entry entry : reportMap.entrySet()) {
                            if (!((String)entry.getKey()).contains(today)) continue;
                            String[] arr = ((String)entry.getKey()).split(",");
                            String actionname = arr[1];
                            ReportModel model = (ReportModel)entry.getValue();
                            ReportMoneySystemModel rModel = new ReportMoneySystemModel();
                            if (!model.isBot) {
                                if (map.containsKey(actionname)) {
                                    rModel = (ReportMoneySystemModel)map.get(actionname);
                                }
                                ReportMoneySystemModel reportMoneySystemModel = rModel;
                                reportMoneySystemModel.moneyWin += model.moneyWin;
                                ReportMoneySystemModel reportMoneySystemModel2 = rModel;
                                reportMoneySystemModel2.moneyLost += model.moneyLost;
                                ReportMoneySystemModel reportMoneySystemModel3 = rModel;
                                reportMoneySystemModel3.moneyOther += model.moneyOther;
                                ReportMoneySystemModel reportMoneySystemModel4 = rModel;
                                reportMoneySystemModel4.fee += model.fee;
                                ReportMoneySystemModel reportMoneySystemModel5 = rModel;
                                reportMoneySystemModel5.revenuePlayGame += model.moneyWin + model.moneyLost;
                                ReportMoneySystemModel reportMoneySystemModel6 = rModel;
                                reportMoneySystemModel6.revenue += model.moneyWin + model.moneyLost + model.moneyOther;
                                map.put(actionname, rModel);
                                continue;
                            }
                            if (mapBot.containsKey(actionname)) {
                                rModel = (ReportMoneySystemModel)mapBot.get(actionname);
                            }
                            ReportMoneySystemModel reportMoneySystemModel7 = rModel;
                            reportMoneySystemModel7.moneyWin += model.moneyWin;
                            ReportMoneySystemModel reportMoneySystemModel8 = rModel;
                            reportMoneySystemModel8.moneyLost += model.moneyLost;
                            ReportMoneySystemModel reportMoneySystemModel9 = rModel;
                            reportMoneySystemModel9.moneyOther += model.moneyOther;
                            ReportMoneySystemModel reportMoneySystemModel10 = rModel;
                            reportMoneySystemModel10.fee += model.fee;
                            ReportMoneySystemModel reportMoneySystemModel11 = rModel;
                            reportMoneySystemModel11.revenuePlayGame += model.moneyWin + model.moneyLost;
                            ReportMoneySystemModel reportMoneySystemModel12 = rModel;
                            reportMoneySystemModel12.revenue += model.moneyWin + model.moneyLost + model.moneyOther;
                            mapBot.put(actionname, rModel);
                        }
                    } else if (et.getTime() < currentDate.getTime()) {
                        endToday = false;
                        map = dao.getReportMoneySystemMySQL(startTime, endTime, false);
                        mapBot = dao.getReportMoneySystemMySQL(startTime, endTime, true);
                    } else if (et.getTime() >= currentDate.getTime()) {
                        String yesterday = VinPlayUtils.getYesterday();
                        map = dao.getReportMoneySystemMySQL(startTime, yesterday, false);
                        mapBot = dao.getReportMoneySystemMySQL(startTime, yesterday, true);
                        IMap<String, ReportModel> reportMap2 = client.getMap("cacheReports");
                        for (Map.Entry entry2 : reportMap2.entrySet()) {
                            if (!((String)entry2.getKey()).contains(today)) continue;
                            String[] arr2 = ((String)entry2.getKey()).split(",");
                            String actionname2 = arr2[1];
                            ReportModel model2 = (ReportModel)entry2.getValue();
                            ReportMoneySystemModel rModel2 = new ReportMoneySystemModel();
                            if (!model2.isBot) {
                                if (map.containsKey(actionname2)) {
                                    rModel2 = (ReportMoneySystemModel)map.get(actionname2);
                                }
                                ReportMoneySystemModel reportMoneySystemModel13 = rModel2;
                                reportMoneySystemModel13.moneyWin += model2.moneyWin;
                                ReportMoneySystemModel reportMoneySystemModel14 = rModel2;
                                reportMoneySystemModel14.moneyLost += model2.moneyLost;
                                ReportMoneySystemModel reportMoneySystemModel15 = rModel2;
                                reportMoneySystemModel15.moneyOther += model2.moneyOther;
                                ReportMoneySystemModel reportMoneySystemModel16 = rModel2;
                                reportMoneySystemModel16.fee += model2.fee;
                                ReportMoneySystemModel reportMoneySystemModel17 = rModel2;
                                reportMoneySystemModel17.revenuePlayGame += model2.moneyWin + model2.moneyLost;
                                ReportMoneySystemModel reportMoneySystemModel18 = rModel2;
                                reportMoneySystemModel18.revenue += model2.moneyWin + model2.moneyLost + model2.moneyOther;
                                map.put(actionname2, rModel2);
                                continue;
                            }
                            if (mapBot.containsKey(actionname2)) {
                                rModel2 = (ReportMoneySystemModel)mapBot.get(actionname2);
                            }
                            ReportMoneySystemModel reportMoneySystemModel19 = rModel2;
                            reportMoneySystemModel19.moneyWin += model2.moneyWin;
                            ReportMoneySystemModel reportMoneySystemModel20 = rModel2;
                            reportMoneySystemModel20.moneyLost += model2.moneyLost;
                            ReportMoneySystemModel reportMoneySystemModel21 = rModel2;
                            reportMoneySystemModel21.moneyOther += model2.moneyOther;
                            ReportMoneySystemModel reportMoneySystemModel22 = rModel2;
                            reportMoneySystemModel22.fee += model2.fee;
                            ReportMoneySystemModel reportMoneySystemModel23 = rModel2;
                            reportMoneySystemModel23.revenuePlayGame += model2.moneyWin + model2.moneyLost;
                            ReportMoneySystemModel reportMoneySystemModel24 = rModel2;
                            reportMoneySystemModel24.revenue += model2.moneyWin + model2.moneyLost + model2.moneyOther;
                            mapBot.put(actionname2, rModel2);
                        }
                    }
                }
                ReportTXModel taiXiu = new ReportTXModel();
                ReportTXModel taiXiuBot = new ReportTXModel();
                HashMap<String, ReportMoneySystemModel> actionGame = new HashMap<String, ReportMoneySystemModel>();
                HashMap<String, Long> vinInUser = new HashMap<String, Long>();
                HashMap<String, Long> vinInEvent = new HashMap<String, Long>();
                long totalInUser = 0L;
                long totalInEvent = 0L;
                long totalIn = 0L;
                HashMap<String, Long> vinOutUser = new HashMap<String, Long>();
                HashMap<String, Long> vinOutAgent = new HashMap<String, Long>();
                long totalOutUser = 0L;
                long totalOutAgent = 0L;
                long totalOut = 0L;
                double ratioCashout = 0.0;
                HashMap<String, Long> vinOther = new HashMap<String, Long>();
                HashMap<String, Long> user = new HashMap<String, Long>();
                HashMap<String, ReportMoneySystemModel> actionGameBot = new HashMap<String, ReportMoneySystemModel>();
                HashMap<String, Long> bot = new HashMap<String, Long>();
                ReportTotalMoneyModel totalModelStart = dao.getReportTotalMoneyAtTime(startTime, true);
                ReportTotalMoneyModel totalModelEnd = new ReportTotalMoneyModel();
                totalModelEnd = endToday ? dao.getTotalMoney(GameCommon.getValueStr((String)"SUPER_AGENT")) : dao.getReportTotalMoneyAtTime(endTime, false);
                vinOutAgent.put("agentStart", totalModelStart.moneyAgent1 + totalModelStart.moneyAgent2 + totalModelStart.moneySuperAgent);
                vinOutAgent.put("agentEnd", totalModelEnd.moneyAgent1 + totalModelEnd.moneyAgent2 + totalModelEnd.moneySuperAgent);
                totalOutAgent = (Long)vinOutAgent.get("agentEnd") - (Long)vinOutAgent.get("agentStart");
                user.put("userStart", totalModelStart.moneyUser);
                user.put("userEnd", totalModelEnd.moneyUser);
                for (Map.Entry entry3 : map.entrySet()) {
                    String actionname3 = (String)entry3.getKey();
                    ReportMoneySystemModel model3 = (ReportMoneySystemModel)entry3.getValue();
                    if (Consts.GAMES.contains(actionname3)) {
                        if (((String)entry3.getKey()).equals("TaiXiu")) {
                            taiXiu.fee = model3.fee;
                            taiXiu.moneyLost = model3.moneyLost;
                            taiXiu.moneyWin = model3.fee * 50L;
                            taiXiu.moneyRefund = model3.moneyWin - taiXiu.moneyWin;
                            taiXiu.moneyOther = model3.moneyOther;
                            taiXiu.revenuePlayGame = taiXiu.moneyRefund + taiXiu.moneyWin + taiXiu.moneyLost;
                            taiXiu.revenue = taiXiu.moneyRefund + taiXiu.moneyWin + taiXiu.moneyLost + taiXiu.moneyOther;
                            continue;
                        }
                        actionGame.put(actionname3, model3);
                        continue;
                    }
                    if (Consts.VIN_IN_USER.contains(actionname3)) {
                        vinInUser.put(actionname3, model3.moneyOther);
                        continue;
                    }
                    if (Consts.VIN_IN_EVENT.contains(actionname3)) {
                        vinInEvent.put(actionname3, model3.moneyOther);
                        continue;
                    }
                    if (Consts.VIN_OUT_USER.contains(actionname3)) {
                        vinOutUser.put(actionname3, Math.abs(model3.moneyOther));
                        continue;
                    }
                    if (!Consts.VIN_OTHER.contains(actionname3)) continue;
                    vinOther.put(actionname3, -model3.moneyOther);
                }
                for (Map.Entry entry4 : vinInUser.entrySet()) {
                    totalInUser += ((Long)entry4.getValue()).longValue();
                }
                for (Map.Entry entry4 : vinInEvent.entrySet()) {
                    totalInEvent += ((Long)entry4.getValue()).longValue();
                }
                totalIn = totalInUser + totalInEvent;
                for (Map.Entry entry4 : vinOutUser.entrySet()) {
                    totalOutUser += ((Long)entry4.getValue()).longValue();
                }
                totalOut = totalOutUser + totalOutAgent;
                if (totalIn > 0L && totalOut > 0L) {
                    ratioCashout = (double)Math.round(10000L * totalOut / totalIn) / 100.0;
                }
                for (Map.Entry entry3 : mapBot.entrySet()) {
                    ReportMoneySystemModel model4 = (ReportMoneySystemModel)entry3.getValue();
                    if (Consts.GAMES.contains(entry3.getKey())) {
                        if (((String)entry3.getKey()).equals("TaiXiu")) {
                            taiXiuBot.fee = model4.fee;
                            taiXiuBot.moneyLost = model4.moneyLost;
                            taiXiuBot.moneyWin = model4.fee * 50L;
                            taiXiuBot.moneyRefund = model4.moneyWin - taiXiuBot.moneyWin;
                            taiXiuBot.moneyOther = model4.moneyOther;
                            taiXiuBot.revenuePlayGame = taiXiuBot.moneyRefund + taiXiuBot.moneyWin + taiXiuBot.moneyLost;
                            taiXiuBot.revenue = taiXiuBot.moneyRefund + taiXiuBot.moneyWin + taiXiuBot.moneyLost + taiXiuBot.moneyOther;
                            continue;
                        }
                        actionGameBot.put((String)entry3.getKey(), (ReportMoneySystemModel)entry3.getValue());
                        continue;
                    }
                    bot.put((String)entry3.getKey(), model4.moneyOther);
                }

                // New Dashboard logic — wrapped in Throwable catch because precompiled DAL
                // may lack source-only methods (NoSuchMethodError is an Error, not Exception)
                int newUsers = 0;
                long totalDeposit = totalInUser;
                long totalWithdraw = totalOutUser;
                long totalComm = 0;
                long profit = 0;
                List<TopUserModel> top10Win = new ArrayList<TopUserModel>();
                List<TopUserModel> top10Loss = new ArrayList<TopUserModel>();
                List<TopUserModel> top10AgencyComm = new ArrayList<TopUserModel>();
                try {
                    GetUserIndexDAOImpl userIndexDAO = new GetUserIndexDAOImpl();
                    String stSQL = VinPlayUtils.getDateTimeStr(VinPlayUtils.getDateTimeFromDate(startTime));
                    String etSQL = VinPlayUtils.getDateTimeStr(VinPlayUtils.getDateTimeFromDate(endTime));
                    java.util.Date etDate = VinPlayUtils.getDateTimeFromDate(endTime);
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.setTime(etDate);
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
                    cal.set(java.util.Calendar.MINUTE, 59);
                    cal.set(java.util.Calendar.SECOND, 59);
                    etSQL = VinPlayUtils.getDateTimeStr(cal.getTime());
                    newUsers = userIndexDAO.getRegister(stSQL, etSQL, null);
                } catch (Throwable e) {
                    logger.error("Dashboard: newUsers error: " + e.getMessage());
                }
                try {
                    totalComm = dao.getTotalCommission(startTime, endTime);
                    Map<String, Long> winners = dao.getTopWinners(startTime, endTime);
                    for (Map.Entry<String, Long> entry : winners.entrySet()) {
                        top10Win.add(new TopUserModel(entry.getKey(), entry.getValue()));
                    }
                    Map<String, Long> losers = dao.getTopLosers(startTime, endTime);
                    for (Map.Entry<String, Long> entry : losers.entrySet()) {
                        top10Loss.add(new TopUserModel(entry.getKey(), entry.getValue()));
                    }
                    Map<String, Long> agencyComm = dao.getTopAgencyCommission(startTime, endTime);
                    for (Map.Entry<String, Long> entry : agencyComm.entrySet()) {
                        top10AgencyComm.add(new TopUserModel(entry.getKey(), entry.getValue()));
                    }
                } catch (Throwable e) {
                    logger.error("Dashboard: metrics error: " + e.getMessage());
                }
                profit = totalDeposit - totalWithdraw - totalModelEnd.moneyUser;
                DashboardModel newDashboardData = new DashboardModel(newUsers, totalDeposit, totalWithdraw, totalComm, profit, top10Win, top10Loss, top10AgencyComm);

                res = new ReportMoneySystemResponse(true, "0", taiXiu, taiXiuBot, actionGame, vinInUser, vinInEvent, totalInUser, totalInEvent, totalIn, vinOutUser, vinOutAgent, totalOutUser, totalOutAgent, totalOut, ratioCashout, vinOther, user, actionGameBot, bot, newDashboardData);
            }
            catch (Exception e) {
                e.printStackTrace();
                logger.debug((Object)e);
            }
        }
        return res.toJson();
    }
}

