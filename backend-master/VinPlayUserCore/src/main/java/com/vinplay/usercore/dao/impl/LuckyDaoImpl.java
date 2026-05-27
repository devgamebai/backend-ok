/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.Block
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.models.cache.SlotFreeDaily
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.usercore.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.usercore.dao.LuckyDao;
import com.vinplay.usercore.entities.vqmm.LuckyHistory;
import com.vinplay.usercore.entities.vqmm.LuckyVipHistory;
import com.vinplay.vbee.common.models.cache.SlotFreeDaily;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.io.UnsupportedEncodingException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

public class LuckyDaoImpl
implements LuckyDao {
    @Override
    public int receiveRotateDaily(int userId, String nickname) throws SQLException {
        Integer rotateCount = -1;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
        CallableStatement call = null;
        try {
            call = conn.prepareCall("CALL lucky_receive_rotate_daily(?,?,?)");
            int param = 1;
            call.setInt(param++, userId);
            call.setString(param++, nickname);
            call.registerOutParameter(param, 4);
            call.executeUpdate();
            rotateCount = call.getInt(param);
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            if (call != null) {
                call.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
        return rotateCount;
    }

    @Override
    public List<Integer> getRotateCount(int userId, String ipAddress) throws SQLException {
        ArrayList<Integer> result = new ArrayList<Integer>();
        Integer rotateDaily = -1;
        Integer rotateFree = -1;
        Integer rotateInDay = -1;
        Integer rotateByIp = -1;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
        CallableStatement call = null;
        try {
            call = conn.prepareCall("CALL lucky_get_rotate_count(?,?,?,?,?,?)");
            int param = 1;
            call.setInt(param++, userId);
            call.setString(param++, ipAddress);
            call.registerOutParameter(param++, 4);
            call.registerOutParameter(param++, 4);
            call.registerOutParameter(param++, 4);
            call.registerOutParameter(param, 4);
            call.executeUpdate();
            rotateDaily = call.getInt(param - 3);
            rotateFree = call.getInt(param - 2);
            rotateInDay = call.getInt(param - 1);
            rotateByIp = call.getInt(param);
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            if (call != null) {
                call.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
        result.add(rotateDaily);
        result.add(rotateFree);
        result.add(rotateInDay);
        result.add(rotateByIp);
        return result;
    }

    @Override
    public boolean saveResultLucky(int userId, String nickname, String ipAddress, int rotateDaily, int rotateFree, int rotateInDay, int rotateByIp, int slotFree, String slotName, int slotMaxWin, int slotRoom) throws SQLException, UnsupportedEncodingException {
        boolean success = false;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
        CallableStatement call = null;
        try {
            call = conn.prepareCall("CALL save_result_lucky(?,?,?,?,?,?,?,?,?,?,?)");
            int param = 1;
            call.setInt(param++, userId);
            call.setString(param++, nickname);
            call.setString(param++, ipAddress);
            call.setInt(param++, rotateDaily);
            call.setInt(param++, rotateFree);
            call.setInt(param++, rotateInDay);
            call.setInt(param++, rotateByIp);
            call.setInt(param++, slotFree);
            call.setString(param++, slotName);
            call.setInt(param++, slotMaxWin);
            call.setInt(param++, slotRoom);
            call.executeUpdate();
            success = true;
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            if (call != null) {
                call.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
        return success;
    }

    @Override
    public List<LuckyHistory> getLuckyHistory(String nickname, int page) {
        int pageSize = 10;
        int skipNumber = (page - 1) * 10;
        final ArrayList<LuckyHistory> luckyHistory = new ArrayList<LuckyHistory>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
//        HashMap<String, String> conditions = new HashMap<String, String>();
        conditions.put("nick_name", nickname);
        BasicDBObject objsort = new BasicDBObject();
        objsort.put("_id", -1);
        FindIterable iterable = db.getCollection("lucky_new_history").find((Bson)new Document(conditions)).sort((Bson)objsort).skip(skipNumber).limit(10);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                LuckyHistory lucky = new LuckyHistory();
                lucky.setTransId(document.getLong("trans_id"));
                lucky.setNickname(document.getString("nick_name"));
                lucky.setResultVin(document.getString("result_vin"));
                lucky.setResultXu(document.getString("result_xu"));
                lucky.setResultSlot(document.getString("result_slot"));
                lucky.setDescription(document.getString("description"));
                lucky.setTransTime(document.getString("trans_time"));
                luckyHistory.add(lucky);
            }
        });
        return luckyHistory;
    }

    @Override
    public List<LuckyVipHistory> getLuckyVipHistory(String nickname, int page) {
        int pageSize = 10;
        int skipNumber = (page - 1) * 10;
        final ArrayList<LuckyVipHistory> luckyHistory = new ArrayList<LuckyVipHistory>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
//HashMap<String, String> conditions = new HashMap<String, String>();
        conditions.put("nick_name", nickname);
        BasicDBObject objsort = new BasicDBObject();
        objsort.put("_id", -1);
        FindIterable iterable = db.getCollection("lucky_vip_history").find((Bson)new Document(conditions)).sort((Bson)objsort).skip(skipNumber).limit(10);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                LuckyVipHistory lucky = new LuckyVipHistory();
                lucky.setTransId(document.getLong("trans_id"));
                lucky.setNickname(document.getString("nick_name"));
                lucky.setResultVin(document.getInteger("result_vin"));
                lucky.setResultMulti(document.getInteger("result_multi"));
                lucky.setTransTime(document.getString("time_log"));
                luckyHistory.add(lucky);
            }
        });
        return luckyHistory;
    }

    @Override
    public Map<String, SlotFreeDaily> getAllSlotFree() throws SQLException {
        HashMap<String, SlotFreeDaily> res = new HashMap<String, SlotFreeDaily>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");){
            String sql = "SELECT * FROM rotate_slot_free WHERE DATE(update_time) = CURDATE()";
            PreparedStatement stm = conn.prepareStatement("SELECT * FROM rotate_slot_free WHERE DATE(update_time) = CURDATE()");
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                String gameName = rs.getString("game_name");
                String nickname = rs.getString("nick_name");
                int room = rs.getInt("room");
                int rotateFree = rs.getInt("rotate_free");
                long maxWin = rs.getInt("max_winning");
                SlotFreeDaily model = new SlotFreeDaily(rotateFree, maxWin);
                String key = nickname + "-" + gameName + "-" + room;
                res.put(key, model);
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public boolean logLuckyVip(long transId, String nickname, String month, int resultVin, int resultMulti) throws Exception {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("lucky_vip_history");
        Document doc = new Document();
        doc.append("trans_id", transId);
        doc.append("nick_name", nickname);
        doc.append("month", month);
        doc.append("result_vin", resultVin);
        doc.append("result_multi", resultMulti);
        doc.append("time_log", VinPlayUtils.getCurrentDateTime());
        doc.append("create_time", new Date());
        col.insertOne(doc);
        return true;
    }

    @Override
    public int getLuckyVipInMonth(String nickname, String month) throws Exception {
        final ArrayList res = new ArrayList();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
//HashMap<String, String> conditions = new HashMap<String, String>();
        conditions.put("nick_name", nickname);
        conditions.put("month", month);
        FindIterable iterable = db.getCollection("lucky_vip_history").find((Bson)new Document(conditions));
        if (iterable != null) {
            iterable.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    res.add(1);
                }
            });
        }
        return res.size();
    }

    @Override
    public long getLuckyVipLastReferenceId() {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap conditions = new HashMap();
        BasicDBObject objsort = new BasicDBObject();
        objsort.put("_id", -1);
        FindIterable iterable = db.getCollection("lucky_vip_history").find((Bson)new Document(conditions)).sort((Bson)objsort).limit(1);
        Document document = iterable != null ? (Document)iterable.first() : null;
        long transId = document == null ? 0L : document.getLong("trans_id");
        return transId;
    }

}

