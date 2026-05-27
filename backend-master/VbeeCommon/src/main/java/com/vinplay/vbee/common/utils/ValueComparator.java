/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.utils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class ValueComparator
implements Comparator<String> {
    HashMap<String, Long> map = new HashMap();

    public ValueComparator(HashMap<String, Long> map) {
        this.map.putAll(map);
    }

    @Override
    public int compare(String s1, String s2) {
        if (this.map.get(s1) >= this.map.get(s2)) {
            return -1;
        }
        return 1;
    }
}

