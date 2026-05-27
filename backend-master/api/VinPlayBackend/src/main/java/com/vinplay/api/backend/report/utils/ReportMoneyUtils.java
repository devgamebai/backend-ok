/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.mongodb.BasicDBObject
 *  com.mongodb.Block
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.dal.dao.impl.ReportDaoImpl
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.cache.ReportModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.statics.Consts
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.apache.log4j.Logger
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.api.backend.report.utils;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.api.backend.agent.utils.AgentTask;
import com.vinplay.api.backend.agent.utils.AgentUtils;
import com.vinplay.api.backend.report.utils.ReportMoneyTask;
import com.vinplay.dal.dao.impl.ReportDaoImpl;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.ReportModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.statics.Consts;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;

public class ReportMoneyUtils {
    private static final Logger logger = Logger.getLogger((String)"report");

    public static void init() {
        try {
            boolean push = true;
            String today = VinPlayUtils.getCurrentDate();
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            IMap<String, ReportModel> reportMap = client.getMap("cacheReports");
            for (Map.Entry entry : reportMap.entrySet()) {
                ReportModel model = (ReportModel)entry.getValue();
                if (model.isBot || !((String)entry.getKey()).contains(today)) continue;
                push = false;
                break;
            }
            if (push) {
                ReportDaoImpl dao = new ReportDaoImpl();
                for (int i = 0; i <= 1; ++i) {
                    boolean isBot = false;
                    if (i == 0) {
                        isBot = true;
                    }
                    Map<String, ReportModel>  maps = dao.getListReportModelByDay(today, isBot);
                    for (Map.Entry entry2 : maps.entrySet()) {
                        reportMap.put(entry2.getKey().toString(), (ReportModel)entry2.getValue());
                    }
                }
            }
            int period = 15;
            Calendar cal = Calendar.getInstance();
            int minute = cal.get(12);
            while (minute % 15 != 0) {
                if (minute >= 59) {
                    minute = 0;
                    cal.add(11, 1);
                    break;
                }
                ++minute;
            }
            cal.set(12, minute);
            cal.set(13, 0);
            Timer timer = new Timer();
            timer.schedule((TimerTask)new ReportMoneyTask(), cal.getTime(), 900000L);
            timer.schedule((TimerTask)new AgentTask(), AgentUtils.getFirstDayAfterMonth());
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.debug((Object)e);
        }
    }

    public static void fixYesterdayData(String today, final String yesterday) throws ParseException {
        logger.info((Object)("fixYesterdayData start at: " + new Date()));
        String timeLog = VinPlayUtils.getDateTimeStr((Date)VinPlayUtils.getDateTimeFromDate((String)yesterday));
        final HashMap<String, ReportModel> reportMap = new HashMap();
        HashMap<String, ReportModel> reportActionMap = new HashMap<String, ReportModel>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        Document conditions = new Document();
        String startTime = timeLog;
        String endTime = VinPlayUtils.getDateTimeStr((Date)VinPlayUtils.getDateTimeFromDate((String)today));
        BasicDBObject obj = new BasicDBObject();
        obj.put("$gte", (Object)startTime);
        obj.put("$lt", (Object)endTime);
        conditions.put("trans_time", (Object)obj);
        conditions.put("is_bot", (Object)false);
        FindIterable iterable = db.getCollection("log_money_user_vin").find((Bson)conditions);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                String serviceName = document.getString((Object)"service_name");
                // Todo:
                if (serviceName != null && !serviceName.equals("T\u00e0i x\u1ec9u - T\u00e1n l\u1ed9c") && !serviceName.equals("T\u00e0i x\u1ec9u - R\u00fat l\u1ed9c")) {
                    String key;
                    String nickname = document.getString((Object)"nick_name");
                    String actionname = document.getString((Object)"action_name");
                    long money = document.getLong((Object)"money_exchange");
                    long fee = document.getLong((Object)"fee");
                    Boolean playGame = document.getBoolean((Object)"play_game");
                    if (playGame == null) {
                        playGame = ReportMoneyUtils.checkPlayGame(actionname);
                    }
                    if (reportMap.containsKey(key = nickname + "," + actionname + "," + yesterday)) {
                        ReportModel reportModel = (ReportModel)reportMap.get(key);
                        if (playGame.booleanValue()) {
                            if (money > 0L) {
                                ReportModel reportModel2 = reportModel;
                                reportModel2.moneyWin += money;
                            } else {
                                ReportModel reportModel3 = reportModel;
                                reportModel3.moneyLost += money;
                            }
                        } else {
                            ReportModel reportModel4 = reportModel;
                            reportModel4.moneyOther += money;
                        }
                        ReportModel reportModel5 = reportModel;
                        reportModel5.fee += fee;
                        reportMap.put(key, reportModel);
                    } else {
                        ReportModel reportModel = new ReportModel();
                        reportModel.isBot = false;
                        if (playGame.booleanValue()) {
                            if (money > 0L) {
                                reportModel.moneyWin = money;
                            } else {
                                reportModel.moneyLost = money;
                            }
                        } else {
                            reportModel.moneyOther = money;
                        }
                        reportModel.fee = fee;
                        reportMap.put(key, reportModel);
                    }
                }
            }
        });
        ReportDaoImpl dao = new ReportDaoImpl();
        for (Map.Entry entry : reportMap.entrySet()) {
            if (((ReportModel)entry.getValue()).isBot) continue;
            String[] arr = ((String)entry.getKey()).split(",");
            String nickname = arr[0];
            String actionname = arr[1];
            dao.saveLogMoneyForReport(nickname, actionname, yesterday, (ReportModel)entry.getValue());
            ReportModel sumModel = reportActionMap.containsKey(actionname) ? (ReportModel)reportActionMap.get(actionname) : new ReportModel(0L, 0L, 0L, 0L, false);
            ReportModel reportModel = sumModel;
            reportModel.moneyWin += ((ReportModel)entry.getValue()).moneyWin;
            ReportModel reportModel2 = sumModel;
            reportModel2.moneyLost += ((ReportModel)entry.getValue()).moneyLost;
            ReportModel reportModel3 = sumModel;
            reportModel3.moneyOther += ((ReportModel)entry.getValue()).moneyOther;
            ReportModel reportModel4 = sumModel;
            reportModel4.fee += ((ReportModel)entry.getValue()).fee;
            reportActionMap.put(actionname, sumModel);
        }
        dao.saveReportMoneyVin(reportActionMap);
        logger.info((Object)("fixYesterdayData success at: " + new Date()));
    }

    public static boolean checkPlayGame(String actionname) {
        boolean res = true;
        if (Consts.NO_GAME.contains(actionname)) {
            res = false;
        }
        return res;
    }

    public static boolean checkBot(IMap<String, UserCacheModel> userMap, String nickname) {
        boolean isBot = false;
        if (userMap.containsKey((Object)nickname)) {
            isBot = ((UserCacheModel)userMap.get((Object)nickname)).isBot();
        } else {
            try {
                isBot = ReportMoneyUtils.checkBotDB(nickname);
            }
            catch (Exception e) {
                logger.debug((Object)e);
            }
        }
        return isBot;
    }

    public static boolean checkBotDB(String nickname) throws SQLException {
        boolean res = false;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        String sql = "SELECT is_bot FROM users WHERE nick_name=?";
        PreparedStatement stm = conn.prepareStatement("SELECT is_bot FROM users WHERE nick_name=?");
        stm.setString(1, nickname);
        ResultSet rs = stm.executeQuery();
        if (rs.next() && rs.getInt("is_bot") == 1) {
            res = true;
        }
        rs.close();
        stm.close();
        if (conn != null) {
            conn.close();
        }
        return res;
    }

}

