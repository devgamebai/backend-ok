/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.usercore.service;

import com.vinplay.usercore.entities.UserLevel;
import java.util.Map;

public interface UserLevelService {
    public String create(String var1, String var2);

    public String create(UserLevel var1);

    public String update(String var1, String var2);

    public UserLevel getByNickName(String var1, String var2);

    public UserLevel getByNickName(String var1);

    public Map<String, Object> findChilds(String var1, String var2, String var3, int var4, int var5);
}

