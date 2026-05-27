/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.dao;

import com.vinplay.dal.entities.giftcode.BundleUsedGiftCodeModel;
import com.vinplay.dal.entities.giftcode.GiftCodeBundleModel;
import java.sql.SQLException;
import java.util.List;

public interface GiftCodeBundleDAO {
    public List<GiftCodeBundleModel> showListGiftCodeBundle(String var1, int var2, int var3) throws SQLException;

    public GiftCodeBundleModel showGiftCodeBundle(String var1, String var2) throws SQLException;

    public Long countGiftCodeBundle(String var1) throws SQLException;

    public Long countValueGiftCode(String var1, String var2, String var3, Integer var4, String var5, String var6) throws SQLException;

    public List<BundleUsedGiftCodeModel> showUsedGiftCodeInBundle(int var1) throws SQLException;
}

