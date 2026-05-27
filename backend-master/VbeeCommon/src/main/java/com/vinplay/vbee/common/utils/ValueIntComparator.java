/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.utils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class ValueIntComparator
implements Comparator<Integer> {
    Map<Integer, Integer> map = new HashMap<Integer, Integer>();

    public ValueIntComparator(Map<Integer, Integer> map) {
        this.map.putAll(map);
    }

    @Override
    public int compare(Integer s1, Integer s2) {
        if (this.map.get(s1) >= this.map.get(s2)) {
            return -1;
        }
        return 1;
    }
}

