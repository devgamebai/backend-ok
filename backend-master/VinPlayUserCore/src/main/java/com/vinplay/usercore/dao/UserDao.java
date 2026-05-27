/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.TopCaoThu
 *  com.vinplay.vbee.common.models.UserAdminInfo
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.models.userMission.MissionObj
 *  com.vinplay.vbee.common.models.userMission.UserMissionCacheModel
 *  com.vinplay.vbee.common.response.UserInfoModel
 */
package com.vinplay.usercore.dao;

import com.vinplay.vbee.common.models.BankSmsModel;
import com.vinplay.vbee.common.models.TopCaoThu;
import com.vinplay.vbee.common.models.UserAdminInfo;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.UserSunReal;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.models.userMission.MissionObj;
import com.vinplay.vbee.common.models.userMission.UserMissionCacheModel;
import com.vinplay.vbee.common.response.UserInfoModel;
import java.sql.SQLException;
import java.util.List;

public interface UserDao {
    public boolean checkUsername(String var1) throws SQLException;

    public boolean checkNickname(String var1) throws SQLException;

    public int checkAgent(String var1) throws SQLException;

    public boolean checkNicknameExist(String var1) throws SQLException;

    public boolean updateMoney(int var1, long var2, String var4) throws SQLException;

    public boolean updateRechargeMoney(int var1, long var2) throws SQLException;

    public UserModel getUserByUserName(String var1) throws SQLException;

    public UserModel getUserByNickName(String var1) throws SQLException;

    public UserModel getUserByFBId(String var1) throws SQLException;

    public UserModel getUserByGGId(String var1) throws SQLException;

    public long getMoneyUser(String var1, String var2) throws SQLException;

    /** Returns vin_total/xu_total (cumulative P&L counter), NOT current balance. Renamed from getCurrentMoney. */
    public long getTotalPnl(String var1, String var2) throws SQLException;

    public int getIdByNickname(String var1) throws SQLException;

    public int getIdByUsername(String var1) throws SQLException;

    public boolean restoreMoneyByAdmin(int var1, long var2, long var4, long var6, String var8) throws SQLException;

    public boolean checkMobile(String var1) throws SQLException;

    public boolean checkMobileDaiLy(String var1) throws SQLException;

    public boolean checkMobileSecurity(String var1) throws SQLException;

    public boolean checkEmailSecurity(String var1) throws SQLException;

    public List<TopCaoThu> getTopCaoThu(String var1, String var2, int var3);

    public UserModel getUserNormalByNickName(String var1) throws SQLException;

    public boolean updateStatusDailyByNickName(String var1, int var2) throws SQLException;

    public List<UserAdminInfo> searchUserAdmin(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8, int var9, int var10, String var11, String var12, String var13) throws SQLException;

    public int countSearchUserAdmin(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8, String var9) throws SQLException;

    public boolean insertBot(String var1, String var2, String var3, long var4, long var6, int var8) throws SQLException;

    public int checkBotByNickname(String var1) throws SQLException;

    public List<UserInfoModel> checkPhoneByUser(String var1) throws SQLException;

    public UserMissionCacheModel getListMissionByNickName(String var1, String var2, int var3) throws SQLException;

    public void insertUserMission(String var1, MissionObj var2, UserModel var3) throws SQLException;

    public void updateUserMission(String var1, String var2, MissionObj var3) throws SQLException;

    public void resetUserMission() throws Exception;

    public UserCacheModel getUserByNickNameCache(String var1) throws SQLException;

    public void insertCommission(int var1, String var2, long var3, String var5) throws SQLException;
    
    public String getReferralCode(String nickName) throws SQLException;

    public long insertBankSms(String var1, String var2, long var3, int var5) throws SQLException;

    public List<BankSmsModel> getBankSmsLst(String id, String content, String sms, String timeStart, String timeEnd, int status, int page, int pageSize, String from, String to) throws SQLException;

    public int countBankSmsLst(String id, String content, String sms, String timeStart, String timeEnd, int status, String from, String to) throws SQLException;

    public boolean updateBankSmsStatus(int status, long id) throws SQLException;

    public boolean updateBankSmsStatus(String content, int status, long id) throws SQLException;

    public boolean verifyMobile(String var1, String var2, boolean var3) throws SQLException;

    public boolean updateTeleIdAndPhoneByNickName(String var1, String var2, String var3) throws SQLException;

    public List<UserCacheModel> GetNickNameFreeze() throws SQLException;

    public List<UserSunReal> getAllUserSunReal() throws SQLException;
}

