/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.dal.dao.impl.ReportDaoImpl
 *  com.vinplay.dal.entities.report.ReportMoneySystemModel
 *  com.vinplay.dal.entities.report.ReportMoneyUserModel
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.cache.ReportModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.statics.Consts
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors.report;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.api.backend.response.ReportMoneyUserResponse;
import com.vinplay.dal.dao.impl.ReportDaoImpl;
import com.vinplay.dal.entities.report.ReportMoneySystemModel;
import com.vinplay.dal.entities.report.ReportMoneyUserModel;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.ReportModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.statics.Consts;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class ReportMoneyUserProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"report");

    public String execute(Param<HttpServletRequest> param) {
        ReportMoneyUserResponse res;
        block15 : {
            HttpServletRequest request = (HttpServletRequest)param.get();
            String nickname = request.getParameter("nn");
            String startTime = request.getParameter("ts");
            String endTime = request.getParameter("te");
            res = new ReportMoneyUserResponse(false, "1001");
            try {
                if (startTime == null || endTime == null || nickname == null || nickname.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) break block15;
                HazelcastInstance client = HazelcastClientFactory.getInstance();
                ReportDaoImpl dao = new ReportDaoImpl();
                boolean isBot = false;
                long currentMoney = 0L;
                long safeMoney = 0L;
                long totalMoney = 0L;
                IMap userMap = client.getMap("users");
                if (userMap.containsKey((Object)nickname)) {
                    UserCacheModel user = (UserCacheModel)userMap.get((Object)nickname);
                    isBot = user.isBot();
                    currentMoney = user.getVinTotal();
                    safeMoney = user.getSafe();
                } else {
                    isBot = dao.checkBot(nickname);
                    currentMoney = dao.getTotalPnl(nickname);
                    safeMoney = dao.getSafeMoney(nickname);
                }
                totalMoney = currentMoney + safeMoney;
                Map<String, ReportMoneySystemModel> actions = new HashMap();
                String today = VinPlayUtils.getCurrentDate();
                SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
                Date st = format.parse(startTime);
                Date et = format.parse(endTime);
                Date currentDate = VinPlayUtils.getCurrentDates();
                if (et.getTime() >= st.getTime()) {
                    ReportModel model;
                    String actionname;
                    if (st.getTime() >= currentDate.getTime()) {
                        IMap<String, ReportModel> reportMap = client.getMap("cacheReports");
                        for (IMap.Entry entry : reportMap.entrySet()) {
                            ReportMoneySystemModel reportMoneySystemModel;
                            if (!((String)entry.getKey()).contains(today)) continue;
                            String[] arr = ((String)entry.getKey()).split(",");
                            String nickname2 = arr[0];
                            actionname = arr[1];
                            if (!nickname2.equals(nickname)) continue;
                            model = (ReportModel)entry.getValue();
                            ReportMoneySystemModel rModel = reportMoneySystemModel = new ReportMoneySystemModel();
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
                            actions.put(actionname, rModel);
                        }
                    } else if (et.getTime() < currentDate.getTime()) {
                        actions = dao.getReportMoneyUser(startTime, endTime, nickname, isBot);
                    } else if (et.getTime() >= currentDate.getTime()) {
                        String yesterday = VinPlayUtils.getYesterday();
                        actions = dao.getReportMoneyUser(startTime, yesterday, nickname, isBot);

                        IMap<String, ReportModel> reportMap2 = client.getMap("cacheReports");
                        for (Map.Entry entry2 : reportMap2.entrySet()) {
                            if (!((String)entry2.getKey()).contains(nickname) || !((String)entry2.getKey()).contains(today)) continue;
                            String[] arr2 = ((String)entry2.getKey()).split(",");
                            actionname = arr2[1];
                            model = (ReportModel)entry2.getValue();
                            ReportMoneySystemModel rModel = new ReportMoneySystemModel();
                            if (actions.containsKey(actionname)) {
                                rModel = (ReportMoneySystemModel)actions.get(actionname);
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
                            actions.put(actionname, rModel);
                        }
                    }
                }
                HashMap actionGame = new HashMap();
                HashMap actionOther = new HashMap();
                for (Map.Entry entry3 : actions.entrySet()) {
                    if (Consts.NO_GAME.contains(entry3.getKey())) {
                        actionOther.put(entry3.getKey(), ((ReportMoneySystemModel)entry3.getValue()).moneyOther);
                        continue;
                    }
                    actionGame.put(entry3.getKey(), entry3.getValue());
                }
                ReportMoneyUserModel model2 = new ReportMoneyUserModel();
                model2.actionGame = actionGame;
                model2.actionOther = actionOther;
                model2.nickname = nickname;
                model2.currentMoney = currentMoney;
                model2.safeMoney = safeMoney;
                model2.totalMoney = totalMoney;
                model2.isBot = isBot;
                res.users = model2;
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

