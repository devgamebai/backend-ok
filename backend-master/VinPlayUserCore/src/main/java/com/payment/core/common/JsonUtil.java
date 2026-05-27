/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.DeserializationFeature
 *  com.fasterxml.jackson.databind.JavaType
 *  com.fasterxml.jackson.databind.JsonMappingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package com.payment.core.common;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.core.common.StringUtil;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class JsonUtil {
    private static ObjectMapper mapper = new ObjectMapper();

    private JsonUtil() {
    }

    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return mapper.writeValueAsString(obj);
        }
        catch (IOException e) {
            throw new RuntimeException("Json serialization error.", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        if (StringUtil.isEmpty(json)) {
            return null;
        }
        try {
            return (T)mapper.readValue(json, clazz);
        }
        catch (JsonMappingException e) {
            throw new RuntimeException("Json deserialization error.", e);
        }
        catch (IOException e) {
            throw new RuntimeException("Json io error.", e);
        }
    }

    public static <T> List<T> fromJsonToList(String json, Class<T> clazz) {
        if (StringUtil.isEmpty(json)) {
            return null;
        }
        try {
            return (List)mapper.readValue(json, (JavaType)mapper.getTypeFactory().constructCollectionType(List.class, clazz));
        }
        catch (JsonMappingException e) {
            throw new RuntimeException("Json deserialization error.", e);
        }
        catch (IOException e) {
            throw new RuntimeException("Json io error.", e);
        }
    }

    public static String map2Json(Map<String, Object> map) {
        return JsonUtil.toJson(map);
    }

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}

