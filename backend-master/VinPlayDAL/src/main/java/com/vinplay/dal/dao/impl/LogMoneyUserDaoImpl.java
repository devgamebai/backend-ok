/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.mongodb.BasicDBObject
 *  com.mongodb.Block
 *  com.mongodb.client.AggregateIterable
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.mongodb.client.result.UpdateResult
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.gamebai.LogNoHuGameBaiMessage
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.response.LogMoneyUserResponse
 *  com.vinplay.vbee.common.response.LogUserMoneyResponse
 *  com.vinplay.vbee.common.statics.Consts
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 *  com.vinplay.vbee.common.utils.UserUtil
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dal.dao.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.UpdateResult;
import com.vinplay.dal.dao.LogMoneyUserDao;
import com.vinplay.dal.dao.impl.UserDaoImpl;
import com.vinplay.dal.entities.gamebai.TopGameBaiModel;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.gamebai.LogNoHuGameBaiMessage;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.response.LogMoneyUserResponse;
import com.vinplay.vbee.common.response.LogUserMoneyResponse;
import com.vinplay.vbee.common.statics.Consts;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import com.vinplay.vbee.common.utils.UserUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.mongodb.client.model.Projections;
import com.vinplay.dal.entities.log.LogMoneyUserNapTieuVinModel;
import com.vinplay.dal.model.LogMoneyUserVin4Report;
import com.vinplay.vbee.common.balancehistory.BalanceHistoryCategory;
import com.vinplay.vbee.common.balancehistory.BalanceHistoryCategoryMapper;
import com.vinplay.vbee.common.balancehistory.BalanceHistoryRow;
import com.vinplay.vbee.common.balancehistory.BalanceHistorySummary;
import java.util.Set;
import org.bson.Document;
import org.bson.conversions.Bson;

public class LogMoneyUserDaoImpl
implements LogMoneyUserDao {

    /**
     * H-1 fix (review round 2 follow-up): cap on {@code page} input for the
     * balance-history endpoint so a caller passing {@code page=1e9} cannot
     * make Mongo {@code skip(2_000_000_000)} which would scan the entire
     * index. Combined with admin auth + 200-row limit, this bounds the
     * worst-case query cost.
     */
    public static final int MAX_BALANCE_HISTORY_PAGE = 10_000;

    @Override
    public List<LogUserMoneyResponse> searchLogMoneyUser(String nickName, String userName, String moneyType, String serviceName, String actionName, String timeStart, String timeEnd, int page, int like, int totalRecord) {
        final ArrayList<LogUserMoneyResponse> results = new ArrayList<LogUserMoneyResponse>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        FindIterable iterable = null;
        BasicDBObject obj = new BasicDBObject();
        int numStart = (page - 1) * totalRecord;
        if (nickName != null && !nickName.equals("")) {
            conditions.put("nick_name", nickName);
        }
        if (userName != null && !userName.equals("")) {
            conditions.put("user_name", userName);
        }
        if (actionName != null && !actionName.equals("")) {
            conditions.put("action_name", actionName);
        }
        if (serviceName != null && !serviceName.equals("")) {
            if (serviceName.startsWith("desc:")) {
                conditions.put("description", new BasicDBObject("$regex", ".*" + serviceName.substring(5) + ".*").append("$options", "i"));
            } else {
                conditions.put("service_name", serviceName);
            }
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("trans_time", obj);
        }

        BasicDBObject objsort = new BasicDBObject();
        objsort.put("_id", -1);
        if (moneyType.equals("vin")) {
            iterable = db.getCollection("log_money_user_vin").find((Bson)new Document(conditions)).sort((Bson)objsort).skip(numStart).limit(totalRecord);
        } else if (moneyType.equals("xu")) {
            iterable = db.getCollection("log_money_user_xu").find((Bson)new Document(conditions)).sort((Bson)objsort).skip(numStart).limit(totalRecord);
        }
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                LogUserMoneyResponse tranlogmoney = new LogUserMoneyResponse();
                tranlogmoney.nickName = document.getString("nick_name");
                tranlogmoney.serviceName = document.getString("service_name");
                tranlogmoney.currentMoney = document.getLong("current_money");
                tranlogmoney.moneyExchange = document.getLong("money_exchange");
                tranlogmoney.description = document.getString("description");
                tranlogmoney.transactionTime = document.getString("trans_time");
                tranlogmoney.actionName = document.getString("action_name");
                tranlogmoney.fee = document.getLong("fee");
                results.add(tranlogmoney);
            }
        });
        return results;
    }

    @Override
    public int countsearchLogMoneyUser(String nickName, String moneyType, String serviceName, String actionName, String timeStart, String timeEnd, int like) {
        int countRecord = 0;
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        objsort.put("_id", -1);
        if (nickName != null && !nickName.equals("")) {
            if (like == 0) {
                conditions.put("nick_name", nickName);
            } else {
                conditions.put("nick_name", nickName);
            }
        }
        if (actionName != null && !actionName.equals("")) {
            conditions.put("action_name", actionName);
        }
        if (serviceName != null && !serviceName.equals("")) {
            if (serviceName.startsWith("desc:")) {
                conditions.put("description", new BasicDBObject("$regex", ".*" + serviceName.substring(5) + ".*").append("$options", "i"));
            } else {
                conditions.put("service_name", serviceName);
            }
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("trans_time", obj);
        }
        if (moneyType.equals("vin")) {
            countRecord = (int)db.getCollection("log_money_user_vin").count((Bson)new Document(conditions));
        } else if (moneyType.equals("xu")) {
            countRecord = (int)db.getCollection("log_money_user_xu").count((Bson)new Document(conditions));
        }
        return countRecord;
    }

    @Override
    public List<LogMoneyUserResponse> getHistoryTransactionLogMoney(String nickName, int moneyType, int page) {
        int numStart = (page - 1) * 13;
        int numEnd = 13;
        List<LogMoneyUserResponse> result = this.getTransactionList(nickName, moneyType, numStart, 13);
        return result;
    }

    private Map<String, Object> buildConditionNapVin(String nickName) {
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        conditions.put("nick_name", nickName);
        return conditions;
    }

    private Map<String, Object> buildConditionTieuVin(String nickName) {
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        conditions.put("nick_name", nickName);
        return conditions;
    }

    private Map<String, Object> buildConditionTransaction(String nickName, int moneyType) {
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        conditions.put("nick_name", nickName);
        ArrayList<BasicDBObject> lstServerName = new ArrayList<BasicDBObject>();
        switch (moneyType) {
            case 4: {
                for (String action : Consts.NAP_XU) {
                    BasicDBObject query = new BasicDBObject("action_name", action);
                    lstServerName.add(query);
                }
                conditions.put("$or", lstServerName);
                conditions.put("money_exchange", new BasicDBObject("$gt", 0));
            }
        }
        return conditions;
    }

    @Override
    public int countHistoryTransactionLogMoney(String nickName, int queryType) {
        int countRecord = 0;
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        Map<String, Object> conditions = this.buildConditionTransaction(nickName, queryType);
        Map<String, Object> conditionNapVin = this.buildConditionNapVin(nickName);
        Map<String, Object> conditionTieuVin = this.buildConditionTieuVin(nickName);
        switch (queryType) {
            case 1: {
                countRecord = (int)db.getCollection("log_money_user_vin").count((Bson)new Document(conditions));
                break;
            }
            case 3: {
                countRecord = (int)db.getCollection("log_money_user_nap_vin").count((Bson)new Document(conditionNapVin));
                break;
            }
            case 5: {
                countRecord = (int)db.getCollection("log_money_user_tieu_vin").count((Bson)new Document(conditionTieuVin));
                break;
            }
            default: {
                countRecord = (int)db.getCollection("log_money_user_xu").count((Bson)new Document(conditions));
            }
        }
        return countRecord;
    }

    @Override
    public List<LogMoneyUserResponse> getTransactionList(String nickName, int queryType, int start, int end) {
        final ArrayList<LogMoneyUserResponse> results = new ArrayList<LogMoneyUserResponse>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        Map<String, Object> conditions = this.buildConditionTransaction(nickName, queryType);
        Map<String, Object> conditionNapVin = this.buildConditionNapVin(nickName);
        Map<String, Object> conditionTieuVin = this.buildConditionTieuVin(nickName);
        FindIterable iterable = null;
        BasicDBObject objsort = new BasicDBObject();
        objsort.put("_id", -1);
        switch (queryType) {
            case 1: {
                iterable = db.getCollection("log_money_user_vin").find((Bson)new Document(conditions)).sort((Bson)objsort).skip(start).limit(end);
                break;
            }
            case 3: {
                iterable = db.getCollection("log_money_user_nap_vin").find((Bson)new Document(conditionNapVin)).sort((Bson)objsort).skip(start).limit(end);
                break;
            }
            case 5: {
                iterable = db.getCollection("log_money_user_tieu_vin").find((Bson)new Document(conditionTieuVin)).sort((Bson)objsort).skip(start).limit(end);
                break;
            }
            default: {
                iterable = db.getCollection("log_money_user_xu").find((Bson)new Document(conditions)).sort((Bson)objsort).skip(start).limit(end);
            }
        }
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                LogMoneyUserResponse tranlogmoney = new LogMoneyUserResponse();
                tranlogmoney.transId = document.getLong("trans_id");
                tranlogmoney.serviceName = document.getString("service_name");
                tranlogmoney.currentMoney = document.getLong("current_money");
                tranlogmoney.moneyExchange = document.getLong("money_exchange");
                tranlogmoney.description = document.getString("description");
                tranlogmoney.transactionTime = document.getString("trans_time");
                results.add(tranlogmoney);
            }
        });
        return results;
    }

    @Override
    public Map<String, TopGameBaiModel> getTopGameBai(String gameName) {
        TopGameBaiModel model;
        HashMap<String, TopGameBaiModel> result = new HashMap<String, TopGameBaiModel>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection collection = db.getCollection("log_money_user_xu");
        Document conditionWin = new Document();
        conditionWin.put("action_name", gameName);
        conditionWin.put("money_exchange", new BasicDBObject("$gt", 0));
        Document conditionWinWeek = new Document();
        conditionWinWeek.put("action_name", gameName);
        conditionWinWeek.put("money_exchange", new BasicDBObject("$gt", 0));
        String startTime = "2016-10-17 00;00;00";
        BasicDBObject obj = new BasicDBObject();
        obj.put("$gte", "2016-10-17 00;00;00");
        conditionWinWeek.put("trans_time", obj);
        AggregateIterable iterableWin = collection.aggregate(Arrays.asList(new Document[]{new Document("$match", conditionWin), new Document("$group", new Document("_id", "$nick_name").append("money", new Document("$sum", "$money_exchange")).append("count", new Document("$sum", 1)))}));
        final ArrayList<TopGameBaiModel> resultWin = new ArrayList();
        iterableWin.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                TopGameBaiModel top = new TopGameBaiModel();
                top.setNickName(document.getString("_id"));
                top.setWinCount(document.getInteger("count"));
                top.setMoneyWin(document.getLong("money"));
                resultWin.add(top);
            }
        });
        AggregateIterable iterableWinWeek = collection.aggregate(Arrays.asList(new Document[]{new Document("$match", conditionWinWeek), new Document("$group", new Document("_id", "$nick_name").append("money", new Document("$sum", "$money_exchange")).append("count", new Document("$sum", 1)))}));
        final ArrayList<TopGameBaiModel> resultWinWeek = new ArrayList();
        iterableWinWeek.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                TopGameBaiModel top = new TopGameBaiModel();
                top.setNickName(document.getString("_id"));
                top.setWinCountThisWeek(document.getInteger("count"));
                top.setMoneyWinThisWeek(document.getLong("money"));
                resultWinWeek.add(top);
            }
        });
        Document conditionLost = new Document();
        conditionLost.put("action_name", gameName);
        conditionLost.put("money_exchange", new BasicDBObject("$lt", 0));
        Document conditionLostWeek = new Document();
        conditionLostWeek.put("action_name", gameName);
        conditionLostWeek.put("money_exchange", new BasicDBObject("$lt", 0));
        conditionLostWeek.put("trans_time", obj);
        AggregateIterable iterableLost = collection.aggregate(Arrays.asList(new Document[]{new Document("$match", conditionLost), new Document("$group", new Document("_id", "$nick_name").append("money", new Document("$sum", "$money_exchange")).append("count", new Document("$sum", 1)))}));
        final ArrayList<TopGameBaiModel> resultLost = new ArrayList();
        iterableLost.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                TopGameBaiModel top = new TopGameBaiModel();
                top.setNickName(document.getString("_id"));
                top.setLostCount(document.getInteger("count"));
                top.setMoneyLost(document.getLong("money"));
                resultLost.add(top);
            }
        });
        AggregateIterable iterablLostWeek = collection.aggregate(Arrays.asList(new Document[]{new Document("$match", conditionLostWeek), new Document("$group", new Document("_id", "$nick_name").append("money", new Document("$sum", "$money_exchange")).append("count", new Document("$sum", 1)))}));
        final ArrayList<TopGameBaiModel> resultLostWeek = new ArrayList();
        iterablLostWeek.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                TopGameBaiModel top = new TopGameBaiModel();
                top.setNickName(document.getString("_id"));
                top.setLostCountThisWeek(document.getInteger("count"));
                top.setMoneyLostThisWeek(document.getLong("money"));
                resultLostWeek.add(top);
            }
        });
        for (TopGameBaiModel top : resultWin) {
            top.setWinCountThisMonth(top.getWinCount());
            top.setWinCountThisYear(top.getWinCount());
            top.setMoneyWinThisMonth(top.getMoneyWin());
            top.setMoneyWinThisYear(top.getMoneyWin());
            result.put(top.getNickName(), top);
        }
        for (TopGameBaiModel top : resultWinWeek) {
            if (!result.containsKey(top.getNickName())) continue;
            model = (TopGameBaiModel)result.get(top.getNickName());
            model.setWinCountThisWeek(top.getWinCountThisWeek());
            model.setMoneyWinThisWeek(top.getMoneyWinThisWeek());
            result.put(top.getNickName(), model);
        }
        for (TopGameBaiModel top : resultLost) {
            if (result.containsKey(top.getNickName())) {
                model = (TopGameBaiModel)result.get(top.getNickName());
                model.setLostCount(top.getLostCount());
                model.setLostCountThisMonth(top.getLostCount());
                model.setLostCountThisYear(top.getLostCount());
                model.setMoneyLost(top.getMoneyLost());
                model.setMoneyLostThisMonth(top.getMoneyLost());
                model.setMoneyLostThisYear(top.getMoneyLost());
                result.put(top.getNickName(), model);
                continue;
            }
            top.setLostCountThisMonth(top.getLostCount());
            top.setLostCountThisYear(top.getLostCount());
            top.setMoneyLostThisMonth(top.getMoneyLost());
            top.setMoneyLostThisYear(top.getMoneyLost());
            result.put(top.getNickName(), top);
        }
        for (TopGameBaiModel top : resultLostWeek) {
            if (!result.containsKey(top.getNickName())) continue;
            model = (TopGameBaiModel)result.get(top.getNickName());
            model.setLostCountThisWeek(top.getLostCountThisWeek());
            model.setMoneyLostThisWeek(top.getMoneyLostThisWeek());
            result.put(top.getNickName(), model);
        }
        return result;
    }

    @Override
    public List<LogNoHuGameBaiMessage> getNoHuGameBaiHistory(int pageNumber, String gameName) {
        int pageSize = 10;
        int skipNumber = (pageNumber - 1) * 10;
        final ArrayList<LogNoHuGameBaiMessage> results = new ArrayList<LogNoHuGameBaiMessage>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        FindIterable iterable = null;
        Document conditions = new Document();
        if (gameName != null && !gameName.isEmpty()) {
            conditions.put("game_name", gameName);
        } else {
            conditions.put("game_name", new Document("$ne", "PokerTour"));
        }
        BasicDBObject sortCondtions = new BasicDBObject();
        sortCondtions.put("_id", -1);
        iterable = db.getCollection("log_no_hu_game_bai").find((Bson)conditions).sort((Bson)sortCondtions).skip(skipNumber).limit(10);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                LogNoHuGameBaiMessage entry = new LogNoHuGameBaiMessage(document.getString("nick_name"), document.getInteger("room").intValue(), document.getLong("pot_value").longValue(), document.getLong("money_win").longValue(), document.getString("game_name"), document.getString("description"), document.getString("tour_id"));
                entry.setCreateTime(document.getString("trans_time"));
                results.add(entry);
            }
        });
        return results;
    }

    @Override
    public int countNoHuGameBaiHistory() {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        Document conditions = new Document();
        long totalRows = db.getCollection("log_no_hu_game_bai").count((Bson)conditions);
        return (int)totalRows;
    }

    @Override
    public UserModel getUserByNickName(String nickname) throws SQLException {
        UserModel user = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT * FROM users WHERE nick_name=?";
            PreparedStatement stm = conn.prepareStatement("SELECT * FROM users WHERE nick_name=?");
            stm.setString(1, nickname);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                user = UserUtil.parseResultSetToUserModel((ResultSet)rs);
            }
            rs.close();
            stm.close();
        }
        return user;
    }

    @Override
    public UserModel getUserById(long id) throws SQLException {
        UserModel user = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement("SELECT * FROM users WHERE id=?")) {
            stm.setLong(1, id);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    user = UserUtil.parseResultSetToUserModel(rs);
                }
            }
        }
        return user;
    }

    @Override
    public List<LogUserMoneyResponse> searchLogMoneyTranferUser(String nickName, String timeStart, String timeEnd, String type, int page) {
        final ArrayList<LogUserMoneyResponse> results = new ArrayList<LogUserMoneyResponse>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        FindIterable iterable = null;
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        objsort.put("_id", -1);
        int numStart = (page - 1) * 100;
        int numEnd = 100;
        conditions.put("action_name", "TransferMoney");
        if (!nickName.isEmpty()) {
            conditions.put("nick_name", nickName);
        }
        if (!timeStart.isEmpty() && !timeEnd.isEmpty()) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("trans_time", obj);
        }
        if (type.equals("1")) {
            iterable = db.getCollection("log_money_user_tieu_vin").find((Bson)new Document(conditions)).sort((Bson)objsort).skip(numStart).limit(100);
        } else if (type.equals("2")) {
            iterable = db.getCollection("log_money_user_nap_vin").find((Bson)new Document(conditions)).sort((Bson)objsort).skip(numStart).limit(100);
        }
        iterable.forEach((Block)new Block<Document>(){

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            public void apply(Document document) {
                LogUserMoneyResponse tranlogmoney = new LogUserMoneyResponse();
                tranlogmoney.nickName = document.getString("nick_name");
                HazelcastInstance client = HazelcastClientFactory.getInstance();
                IMap<String, UserModel> userMap = client.getMap("users");
                if (userMap.containsKey(tranlogmoney.nickName)) {
                    try {
                         userMap.lock(tranlogmoney.nickName);
                        UserCacheModel user = (UserCacheModel)userMap.get(tranlogmoney.nickName);
                        tranlogmoney.userName = user.getUsername();
                    }
                    catch (Exception user) {
                    }
                    finally {
                         userMap.unlock(tranlogmoney.nickName);
                    }
                } else {
                    UserDaoImpl dao = new UserDaoImpl();
                    try {
                        UserModel user2 = dao.getUserByNickName(tranlogmoney.nickName);
                        tranlogmoney.userName = user2.getUsername();
                    }
                    catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                tranlogmoney.serviceName = document.getString("service_name");
                tranlogmoney.currentMoney = document.getLong("current_money");
                tranlogmoney.moneyExchange = document.getLong("money_exchange");
                tranlogmoney.description = document.getString("description");
                tranlogmoney.transactionTime = document.getString("trans_time");
                tranlogmoney.actionName = document.getString("action_name");
                tranlogmoney.fee = document.getLong("fee");
                results.add(tranlogmoney);
            }
        });
        return results;
    }

    @Override
    public boolean UpdateProcessLogChuyenTienDaiLy(String nickNameSend, String nickNameReceive, String timeLog, String Status) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("log_chuyen_tien_dai_ly");
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        conditions.put("trans_time", timeLog);
        conditions.put("nick_name_send", nickNameSend);
        conditions.put("nick_name_receive", nickNameReceive);
        col.updateOne((Bson)new Document(conditions), (Bson)new Document("$set", new Document("process", Integer.parseInt(Status))));
        return true;
    }

    @Override
    public boolean UpdateProcessLogChuyenTienDaiLyMySQL(String nickNameSend, String nickNameReceive, String timeLog, String Status) throws SQLException {
        String sql = " UPDATE vinplay.log_tranfer_agent  SET process = ?,      update_time = ?  WHERE trans_time = ?        AND nick_name_send = ?        AND nick_name_receive = ? ";
        PreparedStatement stmt = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            stmt = conn.prepareStatement(" UPDATE vinplay.log_tranfer_agent  SET process = ?,      update_time = ?  WHERE trans_time = ?        AND nick_name_send = ?        AND nick_name_receive = ? ");
            stmt.setInt(1, Integer.parseInt(Status));
            stmt.setString(2, DateTimeUtils.getCurrentTime((String)"yyyy-MM-dd HH:mm:ss"));
            stmt.setString(3, timeLog);
            stmt.setString(4, nickNameSend);
            stmt.setString(5, nickNameReceive);
            stmt.executeUpdate();
            stmt.close();
        }
        return true;
    }

    @Override
    public LogMoneyUserVin4Report getLogMoneyUserVin4ReportWithListAction(String nick_name, String fromTime, String endTime, List<String> actions, int page, int maxItem) {
        LogMoneyUserVin4Report result = new LogMoneyUserVin4Report();
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection<Document> collection = db.getCollection("log_money_user_vin");
            page = page - 1 < 0 ? 0 : page - 1;
            int numStart = page * maxItem;
            BasicDBObject objsort = new BasicDBObject();
            objsort.put("_id", -1);
            BasicDBObject conditions = new BasicDBObject();
            if (nick_name != null && !nick_name.isEmpty()) {
                conditions.put("nick_name", nick_name);
            }
            if (fromTime != null && !fromTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
                BasicDBObject obj = new BasicDBObject();
                obj.put("$gte", fromTime.trim());
                obj.put("$lte", endTime.trim());
                conditions.put("trans_time", obj);
            }
            if (actions != null) {
                BasicDBObject obj = new BasicDBObject();
                obj.put("$in", actions);
                conditions.put("action_name", obj);
            }
            ArrayList<LogMoneyUserNapTieuVinModel> list = new ArrayList<>();
            Long totalRecords = 0L;
            try {
                list = (ArrayList<LogMoneyUserNapTieuVinModel>) collection.find(new Document(conditions)).projection(Projections.exclude("_id")).skip(numStart).limit(maxItem).sort(objsort).map(document -> {
                    LogMoneyUserNapTieuVinModel model = new LogMoneyUserNapTieuVinModel(document.getLong("trans_id"), document.getInteger("user_id"), document.getString("nick_name"), document.getString("service_name"), document.getLong("current_money"), document.getLong("money_exchange"), document.getString("description"), document.getString("trans_time"), document.getString("action_name"), document.getLong("fee"), document.getDate("create_time"));
                    return model;
                }).into(new ArrayList<>());
                totalRecords = collection.countDocuments(new Document(conditions));
            }
            catch (Exception exception) {
                // empty catch block
            }
            result.setTotalData(totalRecords);
            result.setList(list);
            return result;
        }
        catch (Exception e) {
            e.printStackTrace();
            return result;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Balance history (c=9972) — VIN only.
    // Filter is built from category set; summary is computed in-process so it
    // shares the exact classifier with the list rows.
    // ─────────────────────────────────────────────────────────────────────

    private static long longOrZero(Document doc, String key) {
        Object v = doc.get(key);
        return v instanceof Number ? ((Number) v).longValue() : 0L;
    }

    private BasicDBObject buildBalanceHistoryFilter(String nickName,
                                                    String fromTime,
                                                    String endTime,
                                                    Set<BalanceHistoryCategory> categories) {
        // Defense-in-depth: caller (processor) already validates these. A null/empty
        // here would silently degrade to a full-collection scan, so fail loudly.
        if (nickName == null || nickName.isEmpty()
                || fromTime == null || fromTime.isEmpty()
                || endTime == null || endTime.isEmpty()) {
            throw new IllegalArgumentException("balance-history: nickName/fromTime/endTime required");
        }
        BasicDBObject cond = new BasicDBObject();
        cond.put("nick_name", nickName);
        BasicDBObject t = new BasicDBObject();
        t.put("$gte", fromTime.trim());
        t.put("$lte", endTime.trim());
        cond.put("trans_time", t);
        // Categories: union of regex(action_name) OR service_name $in OR (signed bucket).
        // We skip the OR filter if every category is selected (no narrowing).
        if (categories != null
                && !categories.isEmpty()
                && categories.size() < BalanceHistoryCategory.values().length) {
            List<BasicDBObject> ors = new ArrayList<>();
            Set<String> services = new java.util.HashSet<>();
            for (BalanceHistoryCategory c : categories) {
                services.addAll(BalanceHistoryCategoryMapper.serviceNamesFor(c));
                for (String rx : BalanceHistoryCategoryMapper.actionNameRegexFor(c)) {
                    ors.add(new BasicDBObject("action_name",
                            new BasicDBObject("$regex", rx).append("$options", "i")));
                }
            }
            if (!services.isEmpty()) {
                ors.add(new BasicDBObject("service_name",
                        new BasicDBObject("$in", new ArrayList<>(services))));
            }
            // OTHER is a negative match — easier to enforce in Java post-fetch. We do not
            // narrow Mongo for OTHER; if the only requested category is OTHER, we fetch
            // wide and let the Java classifier drop non-OTHER rows. For mixed sets that
            // include OTHER, we likewise widen.
            if (!categories.contains(BalanceHistoryCategory.OTHER) && !ors.isEmpty()) {
                cond.put("$or", ors);
            }
        }
        return cond;
    }

    @Override
    public List<BalanceHistoryRow> queryBalanceHistory(String nickName,
                                                       String fromTime,
                                                       String endTime,
                                                       Set<BalanceHistoryCategory> categories,
                                                       int page,
                                                       int limit) {
        MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
        MongoCollection<Document> coll = db.getCollection("log_money_user_vin");
        BasicDBObject filter = buildBalanceHistoryFilter(nickName, fromTime, endTime, categories);
        BasicDBObject sort = new BasicDBObject("_id", -1);
        // H-1 fix: cap page to prevent multi-billion Mongo skip (DoS guard).
        int cappedPage = Math.min(Math.max(1, page), MAX_BALANCE_HISTORY_PAGE);
        int skip = (cappedPage - 1) * limit;

        List<BalanceHistoryRow> out = new ArrayList<>();
        FindIterable<Document> it = coll.find((Bson) filter).sort((Bson) sort).skip(skip).limit(limit);
        for (Document doc : it) {
            BalanceHistoryRow row = new BalanceHistoryRow();
            row.transId      = longOrZero(doc, "trans_id");
            row.transTime    = doc.getString("trans_time");
            row.nickname     = doc.getString("nick_name");
            row.actionLabel  = doc.getString("action_name");
            row.serviceName  = doc.getString("service_name");
            row.amount       = longOrZero(doc, "money_exchange");
            row.balanceAfter = longOrZero(doc, "current_money");
            row.fee          = longOrZero(doc, "fee");
            // H-5 fix: include fee in balance_before reconstruction.
            // Convention: wallet writers do `current_money = previous + money_exchange - fee`
            // → `balance_before = balance_after - money_exchange + fee`.
            // For non-fee rows (NAP/game settle), fee = 0 → no change in behavior.
            row.balanceBefore = row.balanceAfter - row.amount + row.fee;
            row.description  = doc.getString("description");
            BalanceHistoryCategory cat = BalanceHistoryCategoryMapper.classify(row.actionLabel, row.serviceName, row.amount);
            row.category = cat.wire();
            // Drop rows that don't match the requested category set (handles OTHER + post-classification mismatches).
            if (categories == null || categories.contains(cat)) {
                out.add(row);
            }
        }
        return out;
    }

    @Override
    public long countBalanceHistory(String nickName,
                                    String fromTime,
                                    String endTime,
                                    Set<BalanceHistoryCategory> categories) {
        // Always re-classify when there is a category filter — Mongo's $regex on
        // action_name does NOT share the Java classifier's \b word boundaries, so
        // a plain countDocuments would over-count vs what queryBalanceHistory
        // actually returns (pagination.total != sum-of-pages otherwise).
        MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
        MongoCollection<Document> coll = db.getCollection("log_money_user_vin");
        BasicDBObject filter = buildBalanceHistoryFilter(nickName, fromTime, endTime, categories);
        boolean noNarrowing = categories == null
                || categories.size() == BalanceHistoryCategory.values().length;
        if (noNarrowing) {
            return coll.countDocuments((Bson) filter);
        }
        long cnt = 0L;
        FindIterable<Document> it = coll.find((Bson) filter)
                .projection(new Document("action_name", 1).append("service_name", 1).append("money_exchange", 1));
        for (Document doc : it) {
            String an = doc.getString("action_name");
            String sn = doc.getString("service_name");
            long me = longOrZero(doc, "money_exchange");
            BalanceHistoryCategory c = BalanceHistoryCategoryMapper.classify(an, sn, me);
            if (categories.contains(c)) cnt++;
        }
        return cnt;
    }

    @Override
    public BalanceHistorySummary summarizeBalanceHistory(String nickName,
                                                         String fromTime,
                                                         String endTime) {
        if (nickName == null || nickName.isEmpty()
                || fromTime == null || fromTime.isEmpty()
                || endTime == null || endTime.isEmpty()) {
            throw new IllegalArgumentException("balance-history: nickName/fromTime/endTime required");
        }
        MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
        MongoCollection<Document> coll = db.getCollection("log_money_user_vin");
        BasicDBObject filter = new BasicDBObject();
        filter.put("nick_name", nickName);
        BasicDBObject t = new BasicDBObject();
        t.put("$gte", fromTime.trim());
        t.put("$lte", endTime.trim());
        filter.put("trans_time", t);
        BalanceHistorySummary sum = new BalanceHistorySummary();
        FindIterable<Document> it = coll.find((Bson) filter)
                .projection(new Document("action_name", 1).append("service_name", 1).append("money_exchange", 1));
        for (Document doc : it) {
            String an = doc.getString("action_name");
            String sn = doc.getString("service_name");
            long me = longOrZero(doc, "money_exchange");
            BalanceHistoryCategory c = BalanceHistoryCategoryMapper.classify(an, sn, me);
            sum.addTo(c, me);
        }
        return sum;
    }

}

