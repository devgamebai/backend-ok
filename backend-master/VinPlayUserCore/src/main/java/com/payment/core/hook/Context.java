/*
 * Decompiled with CFR 0.152.
 */
package com.payment.core.hook;

import java.util.HashMap;
import java.util.Map;

public class Context {
    private Map<String, Object> contextMap = new HashMap<String, Object>();

    public Map<String, Object> getContextMap() {
        return this.contextMap;
    }

    public void setContextMap(Map<String, Object> contextMap) {
        this.contextMap = contextMap;
    }

    public <T> T get(String key) {
        return (T)this.contextMap.get(key);
    }

    public void set(String key, Object value) {
        this.contextMap.put(key, value);
    }

    public String toString() {
        return "Context{contextMap=" + this.contextMap + '}';
    }
}

