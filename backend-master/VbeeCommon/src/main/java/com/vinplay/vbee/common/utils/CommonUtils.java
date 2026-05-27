/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.utils;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CommonUtils {
    public static final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
    public static final String FORMAT_DATE_01 = "yyyy-MM-dd HH:mm:ss";

    public static String convertTimestampToString(Date date, String format) {
        if (format != null && !format.isEmpty())
            return new SimpleDateFormat(format).format(date);
        return dateFormat.format(date);
    }

    public static String convertTimestampToString(Date date) {
        return dateFormat.format(date);
    }

    public static String arrayLongToString(long[] arr) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < arr.length; ++i) {
            builder.append(arr[i]);
            builder.append(",");
        }
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    public static String arrayByteToString(byte[] arr) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < arr.length; ++i) {
            builder.append(arr[i]);
            builder.append(",");
        }
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    public static long[] stringToLongArr(String input) {
        String[] arr = input.split(",");
        long[] result = new long[arr.length];
        for (int i = 0; i < arr.length; ++i) {
            result[i] = Long.parseLong(arr[i]);
        }
        return result;
    }

    public static String getRatioTime(long inputTime) {
        if (inputTime < 10L) {
            return "XX-FAST";
        }
        if (inputTime < 100L) {
            return "X-FAST";
        }
        if (inputTime < 300L) {
            return "FAST";
        }
        if (inputTime < 1000L) {
            return "SLOW";
        }
        if (inputTime < 2000L) {
            return "X-SLOW";
        }
        if (inputTime < 3000L) {
            return "XX-SLOW";
        }
        return "XXX-SLOW";
    }

    /**
     * get base path of class
     * @param cls
     * @return
     */
    public static String getBasePath(Class cls) {
        String basePath = cls.getResource(cls.getSimpleName() + ".class").getPath();
        basePath = basePath.replace("file:", "");
        int index = basePath.indexOf("build");
        if (index >= 0) {
            basePath = basePath.substring(0, index);
        } else {
            // Docker: JAR in /app/libs/app/, use working dir as basePath
            basePath = System.getProperty("user.dir", "") + "/";
        }
        return basePath;
    }

    /**
     *
     * @param name
     */
    public static void clearHazelcastMap(String name, List<String> objects) {
        ArrayList<String> address = new ArrayList<String>();
        address.add("127.0.0.1:5701");
        HazelcastClientFactory.init(address, "dev", "52ZqZJ278l3uH391Z");
        HazelcastInstance instance = HazelcastClientFactory.getInstance();
        IMap map = instance.getMap(name);
        for (String object : objects) {
            if (object != null && map.containsKey(object)) {
                map.remove(object);
            } else {
                map.clear();
            }
        }
        System.out.println("Cleared " + map.getName());
        instance.shutdown();
    }
}

