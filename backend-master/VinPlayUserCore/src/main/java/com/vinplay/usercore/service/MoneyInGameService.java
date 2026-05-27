/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.enums.FreezeInGame
 *  com.vinplay.vbee.common.models.FreezeModel
 *  com.vinplay.vbee.common.response.FreezeMoneyResponse
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.statics.TransType
 */
package com.vinplay.usercore.service;

import com.vinplay.vbee.common.enums.FreezeInGame;
import com.vinplay.vbee.common.models.FreezeModel;
import com.vinplay.vbee.common.response.FreezeMoneyResponse;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;

public interface MoneyInGameService {
    public FreezeModel getFreeze(String var1) throws SQLException;

    public boolean pushFreezeToCache(FreezeModel var1);

    public FreezeMoneyResponse freezeMoneyInGame(String var1, String var2, String var3, long var4, String var6);

    public FreezeMoneyResponse restoreMoneyInGame(String var1, String var2, String var3, String var4, String var5);

    public MoneyResponse addingMoneyInGame(String var1, String var2, String var3, String var4, long var5, String var7, long var8, String var10, long var11);

    public MoneyResponse subtractMoneyInGame(String var1, String var2, String var3, String var4, long var5, String var7, String var8);

    public MoneyResponse subtractMoneyInGameExactly(String var1, String var2, String var3, String var4, long var5, String var7, String var8);

    public MoneyResponse updateMoneyInGameByFreeze(String var1, String var2, String var3, String var4, long var5, String var7, String var8, long var9);

    public List<FreezeModel> getListFreeze(String var1, String var2, String var3, String var4, String var5, int var6) throws ParseException;

    public boolean restoreFreeze(String var1, String var2, String var3, String var4) throws ParseException;

    public boolean restoreFreeze(String var1);

    public void addVippoint(String var1, long var2, String var4);

    public MoneyResponse addFreezeMoneyInGame(String var1, String var2, String var3, String var4, String var5, long var6, String var8, FreezeInGame var9);

    public FreezeMoneyResponse freezeMoneyTranferAgent(String var1, String var2, String var3, long var4, String var6, String var7);

    public List<FreezeModel> getListFreezeMoneyAgentTranfer(String var1, String var2, String var3, String var4, String var5, int var6, String var7) throws SQLException;

    public boolean restoreFreezeTranferAgent(String var1) throws SQLException;

    public FreezeMoneyResponse restoreMoneyTranferAgent(String var1, String var2, String var3, String var4);

    public MoneyResponse updateMoneyUser(String var1, long var2, String var4, String var5, String var6, String var7, long var8, Long var10, TransType var11, boolean var12);

    public MoneyResponse subtractMoneyInGameXocDia(String var1, String var2, String var3, String var4, long var5, String var7, String var8, boolean var9);

    public MoneyResponse updateMoneyGame3rdUser(String var1, long var2, String var4, String var5, String var6, String var7, long var8, boolean var10);
}

