/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.utils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class ValueDoubleComparator
implements Comparator<Long> {
    Map<Long, Double> map = new HashMap<Long, Double>();

    public ValueDoubleComparator(Map<Long, Double> map) {
        this.map.putAll(map);
    }

    @Override
    public int compare(Long s1, Long s2) {
        if (this.map.get(s1) >= this.map.get(s2)) {
            return -1;
        }
        return 1;
    }
}

