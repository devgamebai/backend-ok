/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.usercore.service;

import java.util.Map;

public interface UserWagesService {
    public boolean insertByJob(String var1);

    public String receivedMoney(long var1);

    public String receivedAllMoney(String var1);

    public Map<String, Object> history(String var1, String var2, String var3, int var4, int var5, int var6);
}

