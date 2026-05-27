/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.usercore.service;

import com.vinplay.vbee.common.models.UserBonusModel;
import java.sql.SQLException;
import java.util.List;

public interface UserBonusService {
    public void insertBonus(UserBonusModel var1);

    public boolean isReceivedBonus(String var1, int var2);

    public boolean isSameIP(String var1, int var2);

    public List<UserBonusModel> search(String var1, int var2, int var3, int var4, String var5, String var6) throws SQLException;

    public List<UserBonusModel> search(String var1, String var2, Integer var3, String var4, String var5, int var6, int var7) throws SQLException;

    public Long count(String var1, String var2, Integer var3, String var4, String var5) throws SQLException;

    public double sumAmount(String var1, String var2, Integer var3, String var4, String var5) throws SQLException;

    public boolean checkConditionsByCurrentTime(String var1);

    public boolean checkExit(String var1, int var2);
}

