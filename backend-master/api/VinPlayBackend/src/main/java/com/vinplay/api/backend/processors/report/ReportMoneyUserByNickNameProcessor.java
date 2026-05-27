/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.dal.dao.impl.ReportDaoImpl
 *  com.vinplay.dal.entities.report.ReportMoneySystemModel
 *  com.vinplay.dal.entities.report.ReportUserMoneyModel
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.UserModel
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
import com.vinplay.api.backend.response.ResultReportUserMoneyResponse;
import com.vinplay.dal.dao.impl.ReportDaoImpl;
import com.vinplay.dal.entities.report.ReportMoneySystemModel;
import com.vinplay.dal.entities.report.ReportUserMoneyModel;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.ReportModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.statics.Consts;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class ReportMoneyUserByNickNameProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"report");

    public String execute(Param<HttpServletRequest> param) {
        ResultReportUserMoneyResponse res;
        block18 : {
            HttpServletRequest request = (HttpServletRequest)param.get();
            String nickName = request.getParameter("nn");
            String startTime = request.getParameter("ts");
            String endTime = request.getParameter("te");
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            res = new ResultReportUserMoneyResponse(false, "1001");
            ArrayList<ReportUserMoneyModel> transactions = new ArrayList<ReportUserMoneyModel>();
            ArrayList<String> lstNickNameExists = new ArrayList<String>();
            UserServiceImpl service = new UserServiceImpl();
            try {
                if (startTime == null || endTime == null || nickName == null || nickName.equals("") || startTime.equals("") || endTime.equals("")) break block18;
                String[] arrNN = nickName.split(",");
                for (int i = 0; i < arrNN.length; ++i) {
                    String nickname = arrNN[i].trim();
                    ReportDaoImpl dao = new ReportDaoImpl();
                    boolean isBot = false;
                    long rechargeMoney = 0L;
                    IMap userMap = client.getMap("users");
                    if (userMap.containsKey((Object)nickname)) {
                        UserCacheModel user = (UserCacheModel)userMap.get((Object)nickname);
                        rechargeMoney = user.getRechargeMoney();
                    } else {
                        UserModel users = service.getUserByNickName(nickName);
                        if (users != null) {
                            rechargeMoney = users.getRechargeMoney();
                        } else {
                            lstNickNameExists.add(nickname);
                        }
                    }
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
                            for (IMap.Entry entry2 : reportMap.entrySet()) {
                                ReportMoneySystemModel reportMoneySystemModel;
                                if (!((String)entry2.getKey()).contains(today)) continue;
                                String[] arr = ((String)entry2.getKey()).split(",");
                                String nickname2 = arr[0];
                                actionname = arr[1];
                                if (!nickname2.equals(nickname)) continue;
                                model = (ReportModel)entry2.getValue();
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
                            actions = dao.getReportMoneyUser(startTime, endTime, nickname, false);
                        } else if (et.getTime() >= currentDate.getTime()) {
                            String yesterday = VinPlayUtils.getYesterday();
                            actions = dao.getReportMoneyUser(startTime, yesterday, nickname, false);
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
//                    entry2 = actions.entrySet().iterator();
                    for (IMap.Entry entry2: actions.entrySet()) {
                        if (Consts.NO_GAME.contains(entry2.getKey())) {
                            actionOther.put(entry2.getKey(), ((ReportMoneySystemModel)entry2.getValue()).moneyOther);
                            continue;
                        }
                        actionGame.put(entry2.getKey(), entry2.getValue());
                    }
                    ReportUserMoneyModel model2 = new ReportUserMoneyModel();
                    model2.actionGame = actionGame;
                    model2.actionOther = actionOther;
                    model2.nickName = nickname;
                    model2.rechardMoney = rechargeMoney;
                    transactions.add(model2);
                }
                res.setLstExitsNickName(lstNickNameExists);
                res.setTransactions(transactions);
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

