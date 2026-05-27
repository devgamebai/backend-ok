/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mongodb.util.JSON
 *  org.json.JSONArray
 *  org.json.JSONObject
 */
package com.vinplay.dal.utils;

import com.mongodb.util.JSON;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.LinkedList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class DataUtils {
    public static JSONArray convertToJSONArray(ResultSet resultSet) throws Exception {
        JSONArray jsonArray = new JSONArray();
        int total_rows = resultSet.getMetaData().getColumnCount();
        while (resultSet.next()) {
            JSONObject obj = new JSONObject();
            for (int i = 0; i < total_rows; ++i) {
                obj.put(resultSet.getMetaData().getColumnLabel(i + 1).toLowerCase(), resultSet.getObject(i + 1));
            }
            jsonArray.put(obj);
        }
        return jsonArray;
    }

    public static List<Object> convertToObjectArray(ResultSet resultSet) throws Exception {
        LinkedList<Object> objectArray = new LinkedList<Object>();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int total_rows = resultSet.getMetaData().getColumnCount();
        while (resultSet.next()) {
            JSONObject obj = new JSONObject();
            for (int i = 0; i < total_rows; ++i) {
                String type = metaData.getColumnTypeName(i + 1);
                obj.put(metaData.getColumnLabel(i + 1).toLowerCase(), resultSet.getObject(i + 1) == null ? (type == "INT" ? Integer.valueOf(0) : resultSet.getObject(i + 1)) : resultSet.getObject(i + 1));
            }
            objectArray.add(JSON.parse((String)obj.toString()));
        }
        return objectArray;
    }
}

