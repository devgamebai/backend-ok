/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.Block
 *  com.mongodb.client.AggregateIterable
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.mongodb.client.model.FindOneAndUpdateOptions
 *  com.vinplay.vbee.common.models.cache.ReportModel
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dal.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.vinplay.dal.dao.ReportDAO;
import com.vinplay.dal.entities.report.ReportMoneySystemModel;
import com.vinplay.dal.entities.report.ReportTotalMoneyModel;
import com.vinplay.vbee.common.models.cache.ReportModel;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.io.PrintStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.Document;
import org.bson.conversions.Bson;

public class ReportDaoImpl
implements ReportDAO {
    @Override
    public List<String> getAllBot() throws SQLException {
        ArrayList<String> res = new ArrayList<String>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT nick_name FROM users WHERE is_bot=1";
            PreparedStatement stm = conn.prepareStatement("SELECT nick_name FROM users WHERE is_bot=1");
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                res.add(rs.getString("nick_name"));
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public Map<String, ReportMoneySystemModel> getReportMoneySystemMySQL(String startTime, String endTime, boolean isBot) throws Exception {
        HashMap<String, ReportMoneySystemModel> results = new HashMap<String, ReportMoneySystemModel>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             CallableStatement call = conn.prepareCall("CALL report_money_system(?,?)")) {
            int param = 1;
            call.setString(param++, VinPlayUtils.getDateTimeStr((java.util.Date)VinPlayUtils.getDateTimeFromDate((String)startTime)));
            call.setString(param++, VinPlayUtils.getDateTimeStr((java.util.Date)VinPlayUtils.getDateTimeFromDate((String)endTime)));
            try (ResultSet rs = call.executeQuery()) {
                while (rs.next()) {
                    ReportMoneySystemModel model = new ReportMoneySystemModel();
                    model.moneyWin = rs.getLong("money_win");
                    model.moneyLost = rs.getLong("money_lost");
                    model.moneyOther = rs.getLong("money_other");
                    model.fee = rs.getLong("fee");
                    model.revenuePlayGame = model.moneyWin + model.moneyLost;
                    model.revenue = model.revenuePlayGame + model.moneyOther;
                    String actionName = rs.getString("action_name");
                    results.put(actionName, model);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return results;
    }

    @Override
    public Map<String, ReportMoneySystemModel> getReportMoneySystem(String startTime, String endTime, boolean isBot) throws Exception {
        final HashMap<String, ReportMoneySystemModel> results = new HashMap<String, ReportMoneySystemModel>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        Document conditions = new Document();
        if (!startTime.isEmpty() && !endTime.isEmpty()) {
            BasicDBObject obj = new BasicDBObject();
            obj.put("$gte", VinPlayUtils.getDateTimeStr((java.util.Date)VinPlayUtils.getDateTimeFromDate((String)startTime)));
            obj.put("$lte", VinPlayUtils.getDateTimeStr((java.util.Date)VinPlayUtils.getDateTimeFromDate((String)endTime)));
            conditions.put("time_log", obj);
        }
        MongoCollection col = null;
        if (!isBot) {
            col = db.getCollection("report_money_vin");
            AggregateIterable iterable = col.aggregate(Arrays.asList(new Document[]{new Document("$match", conditions), new Document("$group", new Document("_id", "$action_name").append("money_win", new Document("$sum", "$money_win")).append("money_lost", new Document("$sum", "$money_lost")).append("money_other", new Document("$sum", "$money_other")).append("fee", new Document("$sum", "$fee")))}));
            iterable.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    ReportMoneySystemModel model = new ReportMoneySystemModel();
                    model.moneyWin = document.getLong("money_win");
                    model.moneyLost = document.getLong("money_lost");
                    model.moneyOther = document.getLong("money_other");
                    model.fee = document.getLong("fee");
                    model.revenuePlayGame = model.moneyWin + model.moneyLost;
                    model.revenue = model.revenuePlayGame + model.moneyOther;
                    String actionName = document.getString("_id");
                    results.put(actionName, model);
                }
            });
            return results;
        }
        return results;
    }

    @Override
    public Map<String, ReportMoneySystemModel> getReportMoneyUser(String startTime, String endTime, String nickname, boolean isBot) throws Exception {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        Document conditions = new Document();
        if (!startTime.isEmpty() && !endTime.isEmpty()) {
            BasicDBObject obj = new BasicDBObject();
            obj.put("$gte", VinPlayUtils.getDateTimeStr((java.util.Date)VinPlayUtils.getDateTimeFromDate((String)startTime)));
            obj.put("$lte", VinPlayUtils.getDateTimeStr((java.util.Date)VinPlayUtils.getDateTimeFromDate((String)endTime)));
            conditions.put("time_log", obj);
        }
        conditions.put("nick_name", nickname);
        MongoCollection col = null;
        col = !isBot ? db.getCollection("report_money_vin") : db.getCollection("report_money_vin_bot");
        AggregateIterable iterable = col.aggregate(Arrays.asList(new Document[]{new Document("$match", conditions), new Document("$group", new Document("_id", "$action_name").append("money_win", new Document("$sum", "$money_win")).append("money_lost", new Document("$sum", "$money_lost")).append("money_other", new Document("$sum", "$money_other")).append("fee", new Document("$sum", "$fee")))}));
        final HashMap<String, ReportMoneySystemModel> results = new HashMap<String, ReportMoneySystemModel>();
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                ReportMoneySystemModel model = new ReportMoneySystemModel();
                String actionName = document.getString("_id");
                model.moneyWin = document.getLong("money_win");
                model.moneyLost = document.getLong("money_lost");
                model.moneyOther = document.getLong("money_other");
                model.fee = document.getLong("fee");
                model.revenuePlayGame = model.moneyWin + model.moneyLost;
                model.revenue = model.revenuePlayGame + model.moneyOther;
                results.put(actionName, model);
            }
        });
        return results;
    }

    @Override
    public ReportTotalMoneyModel getTotalMoney(String superAgent) throws SQLException {
        long moneyBot = 0L;
        long moneyUser = 0L;
        long moneyAgent1 = 0L;
        long moneyAgent2 = 0L;
        long moneySuperAgent = 0L;
        String sqlSuperAgent = "SELECT (SUM(vin)) as sum FROM users WHERE nick_name = '" + superAgent + "'";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            try (PreparedStatement stmBot = conn.prepareStatement("SELECT (SUM(vin)) as sum FROM users WHERE is_bot=1");
                 ResultSet rsBot = stmBot.executeQuery()) {
                if (rsBot.next()) moneyBot = rsBot.getLong("sum");
            }
            try (PreparedStatement stmUser = conn.prepareStatement("SELECT (SUM(vin)) as sum FROM users WHERE is_bot=0 AND dai_ly <> 1 AND dai_ly <> 2");
                 ResultSet rsUser = stmUser.executeQuery()) {
                if (rsUser.next()) moneyUser = rsUser.getLong("sum");
            }
            try (PreparedStatement stmAgent1 = conn.prepareStatement("SELECT (SUM(vin)) as sum FROM users WHERE is_bot=0 AND dai_ly = 1");
                 ResultSet rsAgent1 = stmAgent1.executeQuery()) {
                if (rsAgent1.next()) moneyAgent1 = rsAgent1.getLong("sum");
            }
            try (PreparedStatement stmAgent2 = conn.prepareStatement("SELECT (SUM(vin)) as sum FROM users WHERE is_bot=0 AND dai_ly = 2");
                 ResultSet rsAgent2 = stmAgent2.executeQuery()) {
                if (rsAgent2.next()) moneyAgent2 = rsAgent2.getLong("sum");
            }
            try (PreparedStatement stmSuperAgent = conn.prepareStatement(sqlSuperAgent);
                 ResultSet rsSuperAgent = stmSuperAgent.executeQuery()) {
                if (rsSuperAgent.next()) moneySuperAgent = rsSuperAgent.getLong("sum");
            }
        }
        catch (SQLException e) {
            throw e;
        }
        long total = moneyBot + moneyUser + (moneyAgent1 -= moneySuperAgent) + moneyAgent2 + moneySuperAgent;
        ReportTotalMoneyModel model = new ReportTotalMoneyModel(moneyBot, moneyUser, moneyAgent1, moneyAgent2, moneySuperAgent, total, null);
        return model;
    }

    @Override
    public long getTotalPnl(String nickname) throws SQLException {
        long res = 0L;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stm = conn.prepareStatement("SELECT 0 AS vin_total FROM users WHERE nick_name=?"); // SUN-13xx: vin_total dropped, kept alias for response parity
            stm.setString(1, nickname);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                res = 0L;
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public long getSafeMoney(String nickname) throws SQLException {
        long res = 0L;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT 0 AS safe FROM users WHERE nick_name=?";
            PreparedStatement stm = conn.prepareStatement("SELECT 0 AS safe FROM users WHERE nick_name=?");
            stm.setString(1, nickname);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                res = 0L;
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public boolean checkBot(String nickname) throws SQLException {
        boolean res = false;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT is_bot FROM users WHERE nick_name=?";
            PreparedStatement stm = conn.prepareStatement("SELECT is_bot FROM users WHERE nick_name=?");
            stm.setString(1, nickname);
            ResultSet rs = stm.executeQuery();
            if (rs.next() && rs.getInt("is_bot") == 1) {
                res = true;
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public boolean saveLogTotalMoney(ReportTotalMoneyModel model) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("report_total_money");
        Document doc = new Document();
        doc.append("money_bot", model.moneyBot);
        doc.append("money_user", model.moneyUser);
        doc.append("money_agent_1", model.moneyAgent1);
        doc.append("money_agent_2", model.moneyAgent2);
        doc.append("money_super_agent", model.moneySuperAgent);
        doc.append("time_log", VinPlayUtils.getCurrentDateTime());
        col.insertOne(doc);
        return true;
    }

    @Override
    public List<ReportTotalMoneyModel> getReportTotalMoney(int pageNumber, String startTime, String endTime) {
        final ArrayList<ReportTotalMoneyModel> res = new ArrayList<ReportTotalMoneyModel>();
        int pageSize = 50;
        int skipNumber = (pageNumber - 1) * 50;
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        Document conditions = new Document();
        if (!startTime.isEmpty() && !endTime.isEmpty()) {
            BasicDBObject obj = new BasicDBObject();
            obj.put("$gte", startTime);
            obj.put("$lte", endTime);
            conditions.put("time_log", obj);
        }
        BasicDBObject sortCondtions = new BasicDBObject();
        sortCondtions.put("_id", -1);
        FindIterable iterable = db.getCollection("report_total_money").find((Bson)conditions).sort((Bson)sortCondtions).skip(skipNumber).limit(50);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                ReportTotalMoneyModel model = new ReportTotalMoneyModel();
                model.moneyBot = document.getLong("money_bot");
                model.moneyUser = document.getLong("money_user");
                model.moneyAgent1 = document.getLong("money_agent_1");
                model.moneyAgent2 = document.getLong("money_agent_2");
                model.moneySuperAgent = document.getLong("money_super_agent");
                model.total = model.moneyBot + model.moneyUser + model.moneyAgent1 + model.moneyAgent2 + model.moneySuperAgent;
                model.timeLog = document.getString("time_log");
                res.add(model);
            }
        });
        return res;
    }

    @Override
    public ReportTotalMoneyModel getReportTotalMoneyAtTime(String date, boolean bStart) throws ParseException {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        Document conditions = new Document();
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject sortCondtions = new BasicDBObject();
        java.util.Date dateTime = VinPlayUtils.getDateTimeFromDate((String)date);
        if (bStart) {
            obj.put("$gte", VinPlayUtils.getDateTimeStr((java.util.Date)dateTime));
            sortCondtions.put("_id", 1);
        } else {
            Calendar cal = Calendar.getInstance();
            cal.setTime(dateTime);
            cal.add(5, 1);
            obj.put("$lte", VinPlayUtils.getDateTimeStr((java.util.Date)cal.getTime()));
            sortCondtions.put("_id", -1);
        }
        conditions.put("time_log", obj);
        Document document = (Document)db.getCollection("report_total_money").find((Bson)conditions).sort((Bson)sortCondtions).first();
        ReportTotalMoneyModel model = new ReportTotalMoneyModel();
        if (document != null) {
            model.moneyBot = document.getLong("money_bot");
            model.moneyUser = document.getLong("money_user");
            model.moneyAgent1 = document.getLong("money_agent_1");
            model.moneyAgent2 = document.getLong("money_agent_2");
            model.moneySuperAgent = document.getLong("money_super_agent");
            model.total = model.moneyBot + model.moneyUser + model.moneyAgent1 + model.moneyAgent2 + model.moneySuperAgent;
            model.timeLog = document.getString("time_log");
        }
        return model;
    }

    @Override
    public boolean saveLogMoneyForReport(String nickname, String actionname, String date, ReportModel model) throws ParseException {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = null;
        col = !model.isBot ? db.getCollection("report_money_vin") : db.getCollection("report_money_vin_bot");
        BasicDBObject updateFields = new BasicDBObject();
        updateFields.append("money_win", model.moneyWin);
        updateFields.append("money_lost", model.moneyLost);
        updateFields.append("money_other", model.moneyOther);
        updateFields.append("fee", model.fee);
        updateFields.append("time_log", VinPlayUtils.getDateTimeStr((java.util.Date)VinPlayUtils.getDateTimeFromDate((String)date)));
        updateFields.append("create_time", VinPlayUtils.getDateTimeFromDate((String)date));
        BasicDBObject conditions = new BasicDBObject();
        conditions.append("nick_name", nickname);
        conditions.append("action_name", actionname);
        conditions.append("date", date);
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
        options.upsert(true);
        col.findOneAndUpdate((Bson)conditions, (Bson)new Document("$set", updateFields), options);
        return true;
    }

    @Override
    public boolean saveTopCaoThu(String nickname, String date, long moneyWin) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("top_user_play_game_vin");
        BasicDBObject updateFields = new BasicDBObject();
        updateFields.append("money_win", moneyWin);
        BasicDBObject conditions = new BasicDBObject();
        conditions.append("nick_name", nickname);
        conditions.append("date", date);
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
        options.upsert(true);
        col.findOneAndUpdate((Bson)conditions, (Bson)new Document("$set", updateFields), options);
        return true;
    }

    @Override
    public HashMap<String, Long> getReportTopGame(String startTime, String endTime, String actionName, boolean isBot) throws Exception {
        final HashMap<String, Long> results = new HashMap<String, Long>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        Document conditions = new Document();
        if (!startTime.isEmpty() && !endTime.isEmpty()) {
            BasicDBObject obj = new BasicDBObject();
            obj.put("$gte", VinPlayUtils.getDateTimeStr((java.util.Date)VinPlayUtils.getDateTimeFromDate((String)startTime)));
            obj.put("$lte", VinPlayUtils.getDateTimeStr((java.util.Date)VinPlayUtils.getDateTimeFromDate((String)endTime)));
            conditions.put("time_log", obj);
        }
        conditions.put("action_name", actionName);
        MongoCollection col = null;
        if (!isBot) {
            col = db.getCollection("report_money_vin");
            AggregateIterable iterable = col.aggregate(Arrays.asList(new Document[]{new Document("$match", conditions), new Document("$group", new Document("_id", "$nick_name").append("money_win", new Document("$sum", "$money_win")).append("money_lost", new Document("$sum", "$money_lost")).append("money_other", new Document("$sum", "$money_other")))}));
            iterable.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    String nickName = document.getString("_id");
                    long money = document.getLong("money_win") + document.getLong("money_lost") + document.getLong("money_other");
                    results.put(nickName, money);
                }
            });
            return results;
        }
        return results;
    }

    @Override
    public Map<String, ReportModel> getListReportModelByDay(final String date, final boolean isBot) throws Exception {
        final HashMap<String, ReportModel> results = new HashMap<String, ReportModel>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        Document conditions = new Document();
        conditions.put("time_log", VinPlayUtils.getDateTimeStr((java.util.Date)VinPlayUtils.getDateTimeFromDate((String)date)));
        MongoCollection col = null;
        col = !isBot ? db.getCollection("report_money_vin") : db.getCollection("report_money_vin_bot");
        FindIterable iterable = col.find((Bson)conditions);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                ReportModel model = new ReportModel();
                String nickname = document.getString("nick_name");
                String actionname = document.getString("action_name");
                model.moneyWin = document.getLong("money_win");
                model.moneyLost = document.getLong("money_lost");
                model.moneyOther = document.getLong("money_other");
                model.fee = document.getLong("fee");
                model.isBot = isBot;
                String key = nickname + "," + actionname + "," + date;
                results.put(key, model);
            }
        });
        return results;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void saveReportMoneyVin(Map<String, ReportModel> input) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO report_money_daily(action_name, money_win, money_lost, money_other, fee, date) VALUES(?, ?, ?, ?, ?, ?)")) {
            Calendar cal = Calendar.getInstance();
            cal.add(5, -1);
            Date yesterday = new Date(cal.getTimeInMillis());
            for (Map.Entry<String, ReportModel> entry : input.entrySet()) {
                if (entry.getValue().isBot) continue;
                stmt.setString(1, entry.getKey());
                stmt.setLong(2, entry.getValue().moneyWin);
                stmt.setLong(3, entry.getValue().moneyLost);
                stmt.setLong(4, entry.getValue().moneyOther);
                stmt.setLong(5, entry.getValue().fee);
                stmt.setDate(6, yesterday);
                stmt.execute();
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            System.out.println(e);
        }
    }

    @Override
    public Map<String, Long> getTopWinners(String startTime, String endTime) throws Exception {
        final Map<String, Long> results = new HashMap<String, Long>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        Document conditions = new Document();
        if (!startTime.isEmpty() && !endTime.isEmpty()) {
            BasicDBObject obj = new BasicDBObject();
            obj.put("$gte", VinPlayUtils.getDateTimeStr((java.util.Date)VinPlayUtils.getDateTimeFromDate((String)startTime)));
            obj.put("$lte", VinPlayUtils.getDateTimeStr((java.util.Date)VinPlayUtils.getDateTimeFromDate((String)endTime)));
            conditions.put("time_log", obj);
        }
        MongoCollection col = db.getCollection("report_money_vin");
        AggregateIterable<Document> iterable = col.aggregate(Arrays.asList(new Document[]{
            new Document("$match", conditions),
            new Document("$group", new Document("_id", "$nick_name")
                .append("total", new Document("$sum", new Document("$add", Arrays.asList(new Object[]{"$money_win", "$money_lost"}))))),
            new Document("$match", new Document("total", new Document("$gt", 0))),
            new Document("$sort", new Document("total", -1)),
            new Document("$limit", 10)
        }));
        iterable.forEach((Block)new Block<Document>(){
            public void apply(Document document) {
                results.put(document.getString("_id"), document.getLong("total"));
            }
        });
        return results;
    }

    @Override
    public Map<String, Long> getTopLosers(String startTime, String endTime) throws Exception {
        final Map<String, Long> results = new HashMap<String, Long>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        Document conditions = new Document();
        if (!startTime.isEmpty() && !endTime.isEmpty()) {
            BasicDBObject obj = new BasicDBObject();
            obj.put("$gte", VinPlayUtils.getDateTimeStr((java.util.Date)VinPlayUtils.getDateTimeFromDate((String)startTime)));
            obj.put("$lte", VinPlayUtils.getDateTimeStr((java.util.Date)VinPlayUtils.getDateTimeFromDate((String)endTime)));
            conditions.put("time_log", obj);
        }
        MongoCollection col = db.getCollection("report_money_vin");
        AggregateIterable<Document> iterable = col.aggregate(Arrays.asList(new Document[]{
            new Document("$match", conditions),
            new Document("$group", new Document("_id", "$nick_name")
                .append("total", new Document("$sum", new Document("$add", Arrays.asList(new Object[]{"$money_win", "$money_lost"}))))),
            new Document("$match", new Document("total", new Document("$lt", 0))),
            new Document("$sort", new Document("total", 1)),
            new Document("$limit", 10)
        }));
        iterable.forEach((Block)new Block<Document>(){
            public void apply(Document document) {
                results.put(document.getString("_id"), document.getLong("total"));
            }
        });
        return results;
    }

    @Override
    public Map<String, Long> getTopAgencyCommission(String startTime, String endTime) throws Exception {
        final Map<String, Long> results = new HashMap<String, Long>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        Document conditions = new Document();
        if (!startTime.isEmpty() && !endTime.isEmpty()) {
            BasicDBObject obj = new BasicDBObject();
            obj.put("$gte", VinPlayUtils.getDateTimeStr((java.util.Date)VinPlayUtils.getDateTimeFromDate((String)startTime)));
            obj.put("$lte", VinPlayUtils.getDateTimeStr((java.util.Date)VinPlayUtils.getDateTimeFromDate((String)endTime)));
            conditions.put("time_log", obj);
        }
        MongoCollection col = db.getCollection("log_refund_fee_agent");
        AggregateIterable<Document> iterable = col.aggregate(Arrays.asList(new Document[]{
            new Document("$match", conditions),
            new Document("$group", new Document("_id", "$nick_name")
                .append("total", new Document("$sum", "$fee"))),
            new Document("$sort", new Document("total", -1)),
            new Document("$limit", 10)
        }));
        iterable.forEach((Block)new Block<Document>(){
            public void apply(Document document) {
                results.put(document.getString("_id"), document.getLong("total"));
            }
        });
        return results;
    }

    @Override
    public long getTotalCommission(String startTime, String endTime) throws Exception {
        final long[] result = new long[]{0L};
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        Document conditions = new Document();
        if (!startTime.isEmpty() && !endTime.isEmpty()) {
            BasicDBObject obj = new BasicDBObject();
            obj.put("$gte", VinPlayUtils.getDateTimeStr((java.util.Date)VinPlayUtils.getDateTimeFromDate((String)startTime)));
            obj.put("$lte", VinPlayUtils.getDateTimeStr((java.util.Date)VinPlayUtils.getDateTimeFromDate((String)endTime)));
            conditions.put("time_log", obj);
        }
        conditions.put("action_name", "RefundFee");
        MongoCollection col = db.getCollection("report_money_vin");
        AggregateIterable<Document> iterable = col.aggregate(Arrays.asList(new Document[]{
            new Document("$match", conditions),
            new Document("$group", new Document("_id", null)
                .append("total", new Document("$sum", "$money_other")))
        }));
        iterable.forEach((Block)new Block<Document>(){
            public void apply(Document document) {
                result[0] = Math.abs(document.getLong("total"));
            }
        });
        return result[0];
    }
}

