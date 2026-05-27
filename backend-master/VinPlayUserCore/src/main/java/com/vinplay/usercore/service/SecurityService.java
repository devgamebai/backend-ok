/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 *  com.vinplay.vbee.common.models.ConfigGame
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  com.vinplay.vbee.common.response.MoneyResponse
 */
package com.vinplay.usercore.service;

import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import com.vinplay.vbee.common.models.ConfigGame;
import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.DoipassResponse;
import com.vinplay.vbee.common.response.MoneyResponse;
import java.util.List;

public interface SecurityService {
    public byte updateEmail(String var1, String var2);

    public byte updateMobile(String var1, String var2);

    public byte updateUserInfo(String var1, String var2, String var3, String var4);

    public int updateUserVipInfo(String var1, String var2, String var3, String var4);

    public MoneyResponse sendMoneyToSafe(String var1, long var2, boolean var4);

    public MoneyResponse takeMoneyInSafe(String var1, long var2, boolean var4);

    public BaseResponseModel updateAvatar(String var1, String var2);

    public DoipassResponse changePassword(String var1, String var2, String var3, boolean var4);

    public byte activeMobile(String var1, boolean var2);

    public byte activeEmail(String var1);

    public String receiveActiveEmail(String var1);

    public byte updateNewMobile(String var1, String var2, boolean var3);

    public boolean checkMobileSecurity(String var1);

    public byte loginWithOTP(String var1, long var2, byte var4);

    public byte configGame(String var1, String var2, boolean check);

    public List<ConfigGame> getListGameBai(int var1) throws KeyNotFoundException;

    public boolean updateStatusUser(String var1, int var2, String var3);

    public boolean changeSecurityUser(String var1, int var2, String var3);

    public boolean saveLoginInfo(int var1, String var2, String var3, String var4, String var5, int var6, String var7);
    public boolean saveLoginInfo(int var1, String var2, String var3, String var4, String var5, int var6, String var7, String deviceName, String location, String loginMethod, long createdAtTs);
}

