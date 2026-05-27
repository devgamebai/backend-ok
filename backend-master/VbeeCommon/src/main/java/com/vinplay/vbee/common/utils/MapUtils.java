/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.IMap
 */
package com.vinplay.vbee.common.utils;

import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.utils.IMapValueComparator;
import com.vinplay.vbee.common.utils.ValueComparator;
import com.vinplay.vbee.common.utils.ValueComparatorAsc;
import com.vinplay.vbee.common.utils.ValueDoubleComparator;
import com.vinplay.vbee.common.utils.ValueIntComparator;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class MapUtils {
    public static TreeMap<String, Long> sortMapByValue(HashMap<String, Long> map) {
        ValueComparator comparator = new ValueComparator(map);
        TreeMap<String, Long> result = new TreeMap<String, Long>(comparator);
        result.putAll(map);
        return result;
    }

    public static Map<String, Long> sortMapByValue2(HashMap<String, Long> map) {
        ValueComparator comparator = new ValueComparator(map);
        TreeMap<String, Long> result = new TreeMap<String, Long>(comparator);
        result.putAll(map);
        return result;
    }

    public static TreeMap<String, Long> sortMapByValueAsc(HashMap<String, Long> map) {
        ValueComparatorAsc comparator = new ValueComparatorAsc(map);
        TreeMap<String, Long> result = new TreeMap<String, Long>(comparator);
        result.putAll(map);
        return result;
    }

    public static TreeMap<String, Long> sortIMapByValue(IMap<String, Long> map) {
        IMapValueComparator comparator = new IMapValueComparator(map);
        TreeMap<String, Long> result = new TreeMap<String, Long>(comparator);
        result.putAll((Map<String, Long>)map);
        return result;
    }

    public static Map<Integer, Integer> sortMapIntByValue(Map<Integer, Integer> map) {
        ValueIntComparator comparator = new ValueIntComparator(map);
        TreeMap<Integer, Integer> result = new TreeMap<Integer, Integer>(comparator);
        result.putAll(map);
        return result;
    }

    public static Map<Long, Double> sortMapDoubleByValue(Map<Long, Double> map) {
        ValueDoubleComparator comparator = new ValueDoubleComparator(map);
        TreeMap<Long, Double> result = new TreeMap<Long, Double>(comparator);
        result.putAll(map);
        return result;
    }
}

