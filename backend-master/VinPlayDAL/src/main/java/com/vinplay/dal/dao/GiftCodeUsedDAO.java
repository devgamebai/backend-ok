/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.dao;

import com.vinplay.dal.entities.giftcode.GiftCodeUsedModel;
import java.sql.SQLException;
import java.util.List;

public interface GiftCodeUsedDAO {
    public List<GiftCodeUsedModel> showListGiftCodeUsed(String var1, String var2, Integer var3, Integer var4, int var5, String var6, String var7, int var8, int var9) throws SQLException;

    public long countGiftCodeUsed(String var1, String var2, Integer var3, Integer var4, int var5, String var6, String var7) throws SQLException;

    public long countValueGiftCodeUsed(String var1, String var2, Integer var3, Integer var4, int var5, String var6, String var7) throws SQLException;
}

