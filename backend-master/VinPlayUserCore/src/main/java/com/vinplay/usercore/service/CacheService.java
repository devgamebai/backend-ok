/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 */
package com.vinplay.usercore.service;

import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import java.util.Map;
import java.util.Set;

public interface CacheService {
    public void setValue(String var1, String var2);

    public void setValue(String var1, int var2);

    public String getValueStr(String var1) throws KeyNotFoundException;

    public int getValueInt(String var1) throws KeyNotFoundException, NumberFormatException;

    public boolean removeKey(String var1) throws KeyNotFoundException;

    public void setObject(String var1, Object var2);

    public void setObject(String var1, int var2, Object var3);

    public Object getObject(String var1) throws KeyNotFoundException;

    public Object removeObject(String var1) throws KeyNotFoundException;

    public Map<String, Object> getBulk(Set<String> var1);
}

