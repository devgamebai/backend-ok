/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.models.TopCaoThu
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.models.cache.UserExtraInfoModel
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.response.NapXuResponse
 *  com.vinplay.vbee.common.response.UserInfoModel
 *  com.vinplay.vbee.common.response.UserResponse
 *  com.vinplay.vbee.common.statics.TransType
 */
package com.vinplay.usercore.service;

import com.hazelcast.core.IMap;
import com.vinplay.usercore.entities.TransferMoneyResponse;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.models.BankSmsModel;
import com.vinplay.vbee.common.models.TopCaoThu;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.models.cache.UserExtraInfoModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.response.NapXuResponse;
import com.vinplay.vbee.common.response.UserInfoModel;
import com.vinplay.vbee.common.response.UserResponse;
import com.vinplay.vbee.common.statics.TransType;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;

public interface UserService {
    public String insertUser(String var1, String var2) throws SQLException;

    public boolean insertUserBySocial(String var1, String var2) throws SQLException;

    public String updateNickname(int var1, String var2) throws SQLException;

    public boolean checkNickname(String var1) throws SQLException;

    public UserModel getUserByUserName(String var1) throws SQLException;

    public UserModel getUserByNickName(String var1) throws SQLException;

    public UserModel getUserBySocialId(String var1, String var2) throws SQLException;

    public UserResponse checkSessionKey(String var1, String var2, Games var3);

    public void logout(String var1);

    public long getMoneyUserCache(String var1, String var2);
    
    public UserCacheModel getMoneyUser(String var1);

    public long getMoneyCashout(String var1) throws ParseException;

    public long getCurrentMoneyUserCache(String var1, String var2);

    public MoneyResponse updateMoney(String var1, long var2, String var4, String var5, String var6, String var7, long var8, Long var10, TransType var11);

    /**
     * SUN-1205/1206 — variant accepting an explicit commission volume.
     * Set {@code validBetAmount > 0} to override {@code abs(money)} in the
     * downstream rebate calculation (e.g. Dream Gaming hedge bets where
     * money deducted ≠ valid bet). Pass {@code 0} for legacy behavior.
     */
    public MoneyResponse updateMoney(String var1, long var2, String var4, String var5, String var6, String var7, long var8, Long var10, TransType var11, long validBetAmount);

    public BaseResponseModel updateMoneyFromAdmin(String var1, long var2, String var4, String var5, String var6, String var7);

    public MoneyResponse updateMoneyFromAdmin(String var1, long var2, String var4, String var5, String var6, String var7, long var8, boolean var10);

    public BaseResponseModel updateMoneyCacheToDB(String var1) throws SQLException;

    public boolean isPhoneUsed(String var1) throws SQLException;

    public boolean isPhoneUsed(String var1, String var2) throws SQLException;

    public boolean updateVerifyMobile(String var1, String var2, boolean var3) throws SQLException;

    public boolean isXacThucSDT(String var1) throws Exception;

    public boolean checkMobile(String var1) throws SQLException;

    public boolean checkMobileDaiLy(String var1) throws SQLException;

    public boolean checkMobileSecurity(String var1) throws SQLException;

    public boolean checkEmailSecurity(String var1) throws SQLException;

    public byte checkUser(String var1);

    public NapXuResponse napXu(String var1, long var2, boolean var4);

    public List<TopCaoThu> getTopCaoThu(String var1, String var2, int var3);

    public byte checkMoney(String var1, long var2, byte var4);

    public TransferMoneyResponse transferMoney(String var1, String var2, long var3, String var5, boolean var6);

    public double getFeeTransfer(int var1);

    public byte calFeeTransfer(int var1, int var2);

    public UserCacheModel getUser(String var1);

    public boolean insertBot(String var1, String var2, String var3, long var4, long var6, int var8) throws SQLException;

    public long getTotalRechargeMoney(String var1);

    public int getVipPointSave(String var1);

    public boolean checkAccesstoken(String var1, String var2);

    public boolean isActiveToken(String var1, String var2);

    public int checkBot(String var1);

    public UserExtraInfoModel getUserExtraInfo(String var1);

    public UserModel getNicknameExactly(String var1, IMap<String, UserCacheModel> var2) throws SQLException;

    public List<UserInfoModel> checkPhoneByUser(String var1) throws SQLException;

    public UserCacheModel checkMoneyNegative(UserCacheModel var1) throws SQLException;

    public long insertBankSms(String content, String sms, long amount, int status) throws SQLException;

    public List<BankSmsModel> getBankSmsLst(String id, String content, String sms, String timeStart, String timeEnd, int status, int page, int pageSize, String from, String to) throws SQLException;

    public int countBankSmsLst(String id, String content, String sms, String timeStart, String timeEnd, int status, String from, String to) throws SQLException;

    public boolean updateBankSmsStatus(int status, long id) throws SQLException;

    public boolean updateBankSmsStatus(String content, int status, long id) throws SQLException;

    public long getUserValue(String username);

    default boolean isUserBigWin(String username) { return false; }

    default boolean isUserJackpot(String username) { return false; }
}

