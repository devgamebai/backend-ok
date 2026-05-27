/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.Block
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.response.LogVipPointEventResponse
 *  com.vinplay.vbee.common.response.UserVipPointEventResponse
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dal.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.dao.VipPointEventDao;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.response.LogVipPointEventResponse;
import com.vinplay.vbee.common.response.UserVipPointEventResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

public class VipPointEventDaoImpl
implements VipPointEventDao {
    @Override
    public List<LogVipPointEventResponse> listLogVipPointEvent(String nickName, String value, String type, String timeStart, String timeEnd, int page, String bot) {
        final ArrayList<LogVipPointEventResponse> results = new ArrayList<LogVipPointEventResponse>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        FindIterable iterable = null;
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        int numStart = (page - 1) * 50;
        int numEnd = 50;
        objsort.put("_id", -1);
        if (nickName != null && !nickName.equals("")) {
            conditions.put("nick_name", nickName);
        }
        if (value != null && !value.equals("")) {
            conditions.put("value", Integer.parseInt(value));
        }
        if (type != null && !type.equals("")) {
            conditions.put("type", Integer.parseInt(type));
        }
        if (bot != null && !bot.equals("")) {
            conditions.put("is_bot", Integer.parseInt(bot));
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("time_log", obj);
        }
        iterable = db.getCollection("log_vippoint_event").find((Bson)new Document(conditions)).sort((Bson)objsort).skip(numStart).limit(50);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                LogVipPointEventResponse trans = new LogVipPointEventResponse();
                trans.nickName = document.getString("nick_name");
                trans.value = document.getInteger("value");
                trans.type = document.getInteger("type");
                trans.timeLog = document.getString("time_log");
                trans.is_bot = document.getInteger("is_bot");
                results.add(trans);
            }
        });
        return results;
    }

    @Override
    public long countLogVipPointEvent(String nickName, String value, String type, String timeStart, String timeEnd, String bot) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        BasicDBObject obj = new BasicDBObject();
        if (nickName != null && !nickName.equals("")) {
            conditions.put("nick_name", nickName);
        }
        if (value != null && !value.equals("")) {
            conditions.put("value", Integer.parseInt(value));
        }
        if (type != null && !type.equals("")) {
            conditions.put("type", Integer.parseInt(type));
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("time_log", obj);
        }
        if (bot != null && !bot.equals("")) {
            conditions.put("is_bot", Integer.parseInt(bot));
        }
        long record = db.getCollection("log_vippoint_event").count((Bson)new Document(conditions));
        return record;
    }

    @Override
    public long totalVipPointEvent(String nickName, String value, String type, String timeStart, String timeEnd, String bot) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        FindIterable iterable = null;
        BasicDBObject obj = new BasicDBObject();
        if (nickName != null && !nickName.equals("")) {
            conditions.put("nick_name", nickName);
        }
        if (value != null && !value.equals("")) {
            conditions.put("value", Integer.parseInt(value));
        }
        if (type != null && !type.equals("")) {
            conditions.put("type", Integer.parseInt(type));
        }
        if (bot != null && !bot.equals("")) {
            conditions.put("is_bot", Integer.parseInt(bot));
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("time_log", obj);
        }
        final ArrayList<LogVipPointEventResponse> results = new ArrayList();
//        final ArrayList results = new ArrayList();
        iterable = db.getCollection("log_vippoint_event").find((Bson)new Document(conditions));
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                LogVipPointEventResponse trans = new LogVipPointEventResponse();
                trans.value = document.getInteger("value");
                results.add(trans);
            }
        });
        long record = 0L;
        for (LogVipPointEventResponse log : results) {
            record += (long)log.value;
        }
        return record;
    }

    @Override
    public List<UserVipPointEventResponse> listuserVipPoint(String nickName, String sort, String filed, String timeStart, String timeEnd, int page, String bot) throws SQLException {
        ArrayList<UserVipPointEventResponse> results = new ArrayList<UserVipPointEventResponse>();
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        int numStart = (page - 1) * 50;
        int numEnd = 50;
        String order = "";
        try {
            String sql = "SELECT nick_name,vp_real,vp_event,vp_add,num_add,vp_sub,num_sub,place,place_max,update_time,is_bot from users_vp_event WHERE 1=1 ";
            int index = 1;
            if (nickName != null && !nickName.equals("")) {
                sql = sql + " AND nick_name=?";
            }
            String[] keys = filed.split(",");
            for (int i = 0; i < keys.length; ++i) {
                if (keys[i].equals("1")) {
                    if (i == keys.length - 1) {
                        if (sort.equals("1")) {
                            order = order + "vp_real asc";
                            continue;
                        }
                        if (!sort.equals("2")) continue;
                        order = order + "vp_real desc";
                        continue;
                    }
                    if (sort.equals("1")) {
                        order = order + "vp_real asc, ";
                        continue;
                    }
                    if (!sort.equals("2")) continue;
                    order = order + "vp_real desc, ";
                    continue;
                }
                if (keys[i].equals("2")) {
                    if (i == keys.length - 1) {
                        if (sort.equals("1")) {
                            order = order + "vp_event asc";
                            continue;
                        }
                        if (!sort.equals("2")) continue;
                        order = order + "vp_event desc";
                        continue;
                    }
                    if (sort.equals("1")) {
                        order = order + "vp_event asc, ";
                        continue;
                    }
                    if (!sort.equals("2")) continue;
                    order = order + "vp_event desc, ";
                    continue;
                }
                if (keys[i].equals("3")) {
                    if (i == keys.length - 1) {
                        if (sort.equals("1")) {
                            order = order + "vp_add asc";
                            continue;
                        }
                        if (!sort.equals("2")) continue;
                        order = order + "vp_add desc";
                        continue;
                    }
                    if (sort.equals("1")) {
                        order = order + "vp_add asc ,";
                        continue;
                    }
                    if (!sort.equals("2")) continue;
                    order = order + "vp_add desc ,";
                    continue;
                }
                if (keys[i].equals("4")) {
                    if (i == keys.length - 1) {
                        if (sort.equals("1")) {
                            order = order + "num_add asc";
                            continue;
                        }
                        if (!sort.equals("2")) continue;
                        order = order + "num_add desc";
                        continue;
                    }
                    if (sort.equals("1")) {
                        order = order + "num_add asc , ";
                        continue;
                    }
                    if (!sort.equals("2")) continue;
                    order = order + "num_add desc , ";
                    continue;
                }
                if (keys[i].equals("5")) {
                    if (i == keys.length - 1) {
                        if (sort.equals("1")) {
                            order = order + "vp_sub asc";
                            continue;
                        }
                        if (!sort.equals("2")) continue;
                        order = order + "vp_sub desc";
                        continue;
                    }
                    if (sort.equals("1")) {
                        order = order + "vp_sub asc , ";
                        continue;
                    }
                    if (!sort.equals("2")) continue;
                    order = order + "vp_sub desc , ";
                    continue;
                }
                if (keys[i].equals("6")) {
                    if (i == keys.length - 1) {
                        if (sort.equals("1")) {
                            order = order + "num_sub asc";
                            continue;
                        }
                        if (!sort.equals("2")) continue;
                        order = order + "num_sub desc";
                        continue;
                    }
                    if (sort.equals("1")) {
                        order = order + "num_sub asc , ";
                        continue;
                    }
                    if (!sort.equals("2")) continue;
                    order = order + "num_sub desc , ";
                    continue;
                }
                if (keys[i].equals("7")) {
                    if (i == keys.length - 1) {
                        if (sort.equals("1")) {
                            order = order + "place asc";
                            continue;
                        }
                        if (!sort.equals("2")) continue;
                        order = order + "place desc";
                        continue;
                    }
                    if (sort.equals("1")) {
                        order = order + "place asc ,";
                        continue;
                    }
                    if (!sort.equals("2")) continue;
                    order = order + "place desc , ";
                    continue;
                }
                if (keys[i].equals("8")) {
                    if (i == keys.length - 1) {
                        if (sort.equals("1")) {
                            order = order + "place_max asc";
                            continue;
                        }
                        if (!sort.equals("2")) continue;
                        order = order + "place_max desc";
                        continue;
                    }
                    if (sort.equals("1")) {
                        order = order + "place_max asc , ";
                        continue;
                    }
                    if (!sort.equals("2")) continue;
                    order = order + "place_max desc , ";
                    continue;
                }
                if (!keys[i].equals("9")) continue;
                if (i == keys.length - 1) {
                    if (sort.equals("1")) {
                        order = order + "is_bot asc";
                        continue;
                    }
                    if (!sort.equals("2")) continue;
                    order = order + "is_bot desc";
                    continue;
                }
                if (sort.equals("1")) {
                    order = order + "is_bot asc , ";
                    continue;
                }
                if (!sort.equals("2")) continue;
                order = order + "is_bot desc , ";
            }
            if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
                sql = sql + " AND update_time BETWEEN ? AND ? ";
            }
            if (bot != null && !bot.equals("")) {
                sql = sql + " AND is_bot=?";
            }
            String limit = "";
            if (page > 0) {
                limit = " limit " + numStart + " , " + 50;
            }
            sql = sort != null && !sort.equals("") && filed != null && !filed.equals("") ? sql + " order by " + order + limit : sql + limit;
            PreparedStatement stm = conn.prepareStatement(sql);
            if (nickName != null && !nickName.equals("")) {
                stm.setString(index, nickName);
                ++index;
            }
            if (bot != null && !bot.equals("")) {
                stm.setInt(index, Integer.parseInt(bot));
                ++index;
            }
            if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
                stm.setString(index, timeStart);
                stm.setString(index + 1, timeEnd);
                ++index;
            }
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                UserVipPointEventResponse trans = new UserVipPointEventResponse();
                trans.nickName = rs.getString("nick_name");
                trans.vp_real = rs.getInt("vp_real");
                trans.vp_event = rs.getInt("vp_event");
                trans.vp_add = rs.getInt("vp_add");
                trans.num_add = rs.getInt("num_add");
                trans.vp_sub = rs.getInt("vp_sub");
                trans.num_sub = rs.getInt("num_sub");
                trans.place = rs.getInt("place");
                trans.placemax = rs.getInt("place_max");
                trans.updateTime = rs.getString("update_time");
                trans.is_bot = rs.getBoolean("is_bot");
                results.add(trans);
            }
            rs.close();
            stm.close();
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            if (conn != null) {
                conn.close();
            }
        }
        return results;
    }

    @Override
    public long countUserVipPoint(String nickName, String sort, String filed, String timeStart, String timeEnd, String bot) throws SQLException {
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        long total = 0L;
        String order = "";
        try {
            ResultSet rs;
            String sql = "SELECT Count(*) cnt from users_vp_event WHERE 1=1 ";
            int index = 1;
            if (nickName != null && !nickName.equals("")) {
                sql = sql + " AND nick_name=?";
            }
            if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
                sql = sql + " AND update_time BETWEEN ? AND ? ";
            }
            String[] keys = filed.split(",");
            for (int i = 0; i < keys.length; ++i) {
                if (keys[i].equals("1")) {
                    if (i == keys.length - 1) {
                        if (sort.equals("1")) {
                            order = order + "vp_real asc";
                            continue;
                        }
                        if (!sort.equals("2")) continue;
                        order = order + "vp_real desc";
                        continue;
                    }
                    if (sort.equals("1")) {
                        order = order + "vp_real asc, ";
                        continue;
                    }
                    if (!sort.equals("2")) continue;
                    order = order + "vp_real desc, ";
                    continue;
                }
                if (keys[i].equals("2")) {
                    if (i == keys.length - 1) {
                        if (sort.equals("1")) {
                            order = order + "vp_event asc";
                            continue;
                        }
                        if (!sort.equals("2")) continue;
                        order = order + "vp_event desc";
                        continue;
                    }
                    if (sort.equals("1")) {
                        order = order + "vp_event asc, ";
                        continue;
                    }
                    if (!sort.equals("2")) continue;
                    order = order + "vp_event desc, ";
                    continue;
                }
                if (keys[i].equals("3")) {
                    if (i == keys.length - 1) {
                        if (sort.equals("1")) {
                            order = order + "vp_add asc";
                            continue;
                        }
                        if (!sort.equals("2")) continue;
                        order = order + "vp_add desc";
                        continue;
                    }
                    if (sort.equals("1")) {
                        order = order + "vp_add asc ,";
                        continue;
                    }
                    if (!sort.equals("2")) continue;
                    order = order + "vp_add desc ,";
                    continue;
                }
                if (keys[i].equals("4")) {
                    if (i == keys.length - 1) {
                        if (sort.equals("1")) {
                            order = order + "num_add asc";
                            continue;
                        }
                        if (!sort.equals("2")) continue;
                        order = order + "num_add desc";
                        continue;
                    }
                    if (sort.equals("1")) {
                        order = order + "num_add asc , ";
                        continue;
                    }
                    if (!sort.equals("2")) continue;
                    order = order + "num_add desc , ";
                    continue;
                }
                if (keys[i].equals("5")) {
                    if (i == keys.length - 1) {
                        if (sort.equals("1")) {
                            order = order + "vp_sub asc";
                            continue;
                        }
                        if (!sort.equals("2")) continue;
                        order = order + "vp_sub desc";
                        continue;
                    }
                    if (sort.equals("1")) {
                        order = order + "vp_sub asc , ";
                        continue;
                    }
                    if (!sort.equals("2")) continue;
                    order = order + "vp_sub desc , ";
                    continue;
                }
                if (keys[i].equals("6")) {
                    if (i == keys.length - 1) {
                        if (sort.equals("1")) {
                            order = order + "num_sub asc";
                            continue;
                        }
                        if (!sort.equals("2")) continue;
                        order = order + "num_sub desc";
                        continue;
                    }
                    if (sort.equals("1")) {
                        order = order + "num_sub asc , ";
                        continue;
                    }
                    if (!sort.equals("2")) continue;
                    order = order + "num_sub desc , ";
                    continue;
                }
                if (keys[i].equals("7")) {
                    if (i == keys.length - 1) {
                        if (sort.equals("1")) {
                            order = order + "place asc";
                            continue;
                        }
                        if (!sort.equals("2")) continue;
                        order = order + "place desc";
                        continue;
                    }
                    if (sort.equals("1")) {
                        order = order + "place asc ,";
                        continue;
                    }
                    if (!sort.equals("2")) continue;
                    order = order + "place desc , ";
                    continue;
                }
                if (keys[i].equals("8")) {
                    if (i == keys.length - 1) {
                        if (sort.equals("1")) {
                            order = order + "place_max asc";
                            continue;
                        }
                        if (!sort.equals("2")) continue;
                        order = order + "place_max desc";
                        continue;
                    }
                    if (sort.equals("1")) {
                        order = order + "place_max asc , ";
                        continue;
                    }
                    if (!sort.equals("2")) continue;
                    order = order + "place_max desc , ";
                    continue;
                }
                if (!keys[i].equals("9")) continue;
                if (i == keys.length - 1) {
                    if (sort.equals("1")) {
                        order = order + "is_bot asc";
                        continue;
                    }
                    if (!sort.equals("2")) continue;
                    order = order + "is_bot desc";
                    continue;
                }
                if (sort.equals("1")) {
                    order = order + "is_bot asc , ";
                    continue;
                }
                if (!sort.equals("2")) continue;
                order = order + "is_bot desc , ";
            }
            if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
                sql = sql + " AND update_time BETWEEN ? AND ? ";
            }
            if (bot != null && !bot.equals("")) {
                sql = sql + " AND is_bot=?";
            }
            if (sort != null && !sort.equals("") && filed != null && !filed.equals("")) {
                sql = sql + " order by " + order;
            }
            PreparedStatement stm = conn.prepareStatement(sql);
            if (nickName != null && !nickName.equals("")) {
                stm.setString(index, nickName);
                ++index;
            }
            if (bot != null && !bot.equals("")) {
                stm.setInt(index, Integer.parseInt(bot));
                ++index;
            }
            if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
                stm.setString(index, timeStart);
                stm.setString(index + 1, timeEnd);
                ++index;
            }
            if ((rs = stm.executeQuery()).next()) {
                total = rs.getLong("cnt");
            }
            rs.close();
            stm.close();
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            if (conn != null) {
                conn.close();
            }
        }
        return total;
    }

}

