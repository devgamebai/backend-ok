/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.GiftCodeGameResponse
 */
package com.vinplay.usercore.dao;

import com.vinplay.vbee.common.response.GiftCodeGameResponse;
import java.util.List;

public interface GiftCodeGameDao {
    public boolean exportGiftCodeStore(GiftCodeGameResponse var1);

    public boolean exportGiftCode(GiftCodeGameResponse var1);

    public List<GiftCodeGameResponse> searchAllGiftCode(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8, String var9, int var10, int var11);

    public List<GiftCodeGameResponse> searchAllGiftCodeAdmin(String var1, String var2, String var3, String var4, String var5, int var6, int var7);

    public long countSearchAllGiftCode(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8, String var9);

    public long countSearchAllGiftCodeAdmin(String var1, String var2, String var3, String var4, String var5);

    public boolean blockGiftCode(String var1, String var2);
}

