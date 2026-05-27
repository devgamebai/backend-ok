/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.dao;

import com.vinplay.dal.entities.giftcode.GiftCodeModel;
import java.sql.SQLException;
import java.util.List;

public interface GiftCodeDAO {
    public List<GiftCodeModel> showListGiftCode(String var1, String var2, String var3, Integer var4, String var5, String var6, int var7, int var8) throws SQLException;

    public Long countGiftCode(String var1, String var2, String var3, Integer var4, String var5, String var6) throws SQLException;

    public Long countValueGiftCode(String var1, String var2, String var3, Integer var4, String var5, String var6) throws SQLException;

    public Long countUsedGiftCode(String var1) throws SQLException;
}

