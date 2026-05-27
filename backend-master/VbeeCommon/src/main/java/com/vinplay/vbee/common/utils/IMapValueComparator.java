/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.IMap
 */
package com.vinplay.vbee.common.utils;

import com.hazelcast.core.IMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class IMapValueComparator
implements Comparator<String> {
    HashMap<String, Long> map = new HashMap();

    public IMapValueComparator(IMap<String, Long> map) {
        this.map.putAll((Map<String, Long>)map);
    }

    @Override
    public int compare(String s1, String s2) {
        if (this.map.get(s1) >= this.map.get(s2)) {
            return -1;
        }
        return 1;
    }
}

