/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.dvt.RefundFeeAgentMessage
 *  com.vinplay.vbee.common.models.AgentModel
 *  com.vinplay.vbee.common.models.cache.AgentDSModel
 *  com.vinplay.vbee.common.response.AgentResponse
 *  com.vinplay.vbee.common.response.LogAgentTranferMoneyResponse
 *  com.vinplay.vbee.common.response.TranferAgentResponse
 */
package com.vinplay.dal.dao;

import com.vinplay.dal.entities.agent.*;
import com.vinplay.vbee.common.messages.dvt.RefundFeeAgentMessage;
import com.vinplay.vbee.common.models.AgentModel;
import com.vinplay.vbee.common.models.cache.AgentDSModel;
import com.vinplay.vbee.common.response.AgentResponse;
import com.vinplay.vbee.common.response.LogAgentTranferMoneyResponse;
import com.vinplay.vbee.common.response.TranferAgentResponse;
import org.json.JSONArray;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

public interface AgentDAO {

    public Map<String, Object> loginSystemAgent(String username, String password) throws SQLException;

    public boolean insertRefCodeAgent(int id, String refCode) throws SQLException;

    public boolean updateUserIsDaily(String userName) throws SQLException;

    public CheckDataAgentModel getUserID(String userName) throws SQLException;

    public boolean isAgentExit(int userID, String agencyCode) throws SQLException;

    public List<AgentResponse> listAgentByParent(int parentId) throws SQLException;

    public List<AgentResponse> listAgent() throws SQLException;

    public List<AgentResponse> listAgentByLevel(int parentId, int level) throws SQLException;

    public List<AgentResponse> listAllAgent() throws SQLException;

    public List<UserAgentModel> listUserAgent(String username, String nickname, String password, String nameagent, String address, String phone, String email,
                                              String facebook, String key, String status, Integer parentid, String namebank, String nameaccount, String numberaccount,
                                              Integer show, Integer active, Date createtime, Date updatetime, Integer order, Integer sms, Integer percent_bonus_vincard,
                                              String site, Date last_login_time, Integer login_times, Integer level, String code, int page, int maxItem) throws SQLException;

    public UserAgentModel DetailUserAgent(Integer id) throws SQLException;

    public UserAgentModel DetailUserAgentByCode(String agencyCode) throws SQLException;
    
    public UserAgentModel DetailUserAgentByUserName(String username) throws SQLException;
    
    public UserAgentModel DetailUserAgentByNickName(String nickname) throws SQLException;

    public String changePasswordUserAgent(String nick_name, String old_password, String new_password) throws SQLException;
    public String getNicknameById(int id) throws SQLException;
    public String resetPasswordUserAgent(String nickname, String newPassword) throws SQLException;

    public long countlistUserAgent(String username, String nickname, String password, String nameagent, String address, String phone, String email,
                                   String facebook, String key, String status, Integer parentid, String namebank, String nameaccount, String numberaccount,
                                   Integer show, Integer active, Date createtime, Date updatetime, Integer order, Integer sms, Integer percent_bonus_vincard,
                                   String site, Date last_login_time, Integer login_times, Integer level, String code) throws SQLException;

    public Boolean AddNewUserAgent(String username, String nickname, String password, String nameagent, String address, String phone, String email,
                                   String facebook, String key, String status, Integer parentid, String namebank, String nameaccount, String numberaccount,
                                   Integer show, Integer active, String createtime, String updatetime, Integer order, Integer sms, Integer percent_bonus_vincard,
                                   String site, Date last_login_time, Integer login_times, Integer level, String code, Double commission_rate) throws SQLException;
    
    public Boolean AddNewUserAgent(UserAgentModel userAgentModel) throws SQLException;
    
    public Boolean AddNewUser(UserAgentModel userAgentModel) throws SQLException;

    public Boolean checkCodeOfUserAgent(String code) throws SQLException;

    public Boolean UpdateUserAgent(Integer id, String username, String nickname, String password, String nameagent, String address, String phone, String email,
                                   String facebook, String key, String status, Integer parentid, String namebank, String nameaccount, String numberaccount,
                                   Integer show, Integer active, String createtime, String updatetime, Integer order, Integer sms, Integer percent_bonus_vincard,
                                   String site, Date last_login_time, Integer login_times, Integer level, String code, Double commission_rate) throws SQLException;
    
    public Boolean UpdateUserAgent(UserAgentModel userAgentModel) throws SQLException;

    public Boolean deleteUserAgent(Integer id) throws SQLException;

    public Map<String, Object> listAgent1(String nick_name,String fromTime,String endTime,int page,int maxItem) throws SQLException;

    public Map<String, Object> listAgent1(String nick_name, String refcode, String fromTime, String endTime,int page,int maxItem) throws SQLException;

    public List<VinPlayAgentModel> listCountUserInListAgent1(String nick_name, String refcode, String fromTime, String endTime,int page,int maxItem) throws SQLException;

    public List<UserOfAgentModel> listUserOfAgent(String referral_code,String nick_name,String fromTime,String endTime,Long doanhThu,int page,int maxItem) throws SQLException;

    public List<DetailUserModel> listUserOfUserAgent(String code, String nickname, String fromTime, String endTime, int page, int maxItem) throws SQLException;

    public long countUserOfUserAgent(String code, String nickname, String fromTime, String endTime) throws SQLException;

    public Long[] analyticsDepositWithdrawOfAllUserOfAgent(String referral_code,String nick_name,String fromTime,String endTime,Long doanhThu) throws SQLException;

    public Long countUserOfAgent(String referral_code,String nick_name,String fromTime,String endTime,Long doanhThu) throws SQLException;

    public List<UserDetailAgentModel> getUserDetailAgent(String nick_name,String fromTime,String endTime) throws SQLException;

    public List<Map<String, Object>> getUsersDepositFirstInDay(String currentTime, String nickname, String referCode, int page, int maxItem) throws SQLException;
    
    public List<Map<String, Object>> getUsersDepositFirstInDay(String fromTime, String endTime, String nickname, String referCode, int page, int maxItem) throws SQLException;
    
    public List<UserDetailAgentModel> getUserDetailAgentCurrentDay(String nick_name, String fromTime, String endTime) throws SQLException;

    public List<Object> getLogUserDetail(String nick_name, String fromTime, String endTime, int page, int maxItem) throws Exception;

    public long totalLogUserDetail(String nick_name, String fromTime, String endTime) throws Exception;

    public List<AgentResponse> listAgentByClient(String client) throws SQLException;
    public AgentResponse listAgentByKey(String key) throws SQLException;

    public List<LogAgentTranferMoneyResponse> searchAgentTranferMoney(String var1, String var2, String var3, String var4, String var5, String var6, int var7);

    public List<AgentResponse> listUserAgent(String var1) throws SQLException;

    public List<AgentResponse> listUserAgentByParentID(int var1) throws SQLException;

    public TranferAgentResponse searchAgentTranfer(String var1, String var2, String var3, String var4) throws SQLException;

    public LogAgentTranferMoneyResponse searchAgentTranferMoney(String tid);

    public AgentDSModel getDS(String var1, String var2, String var3, boolean var4) throws SQLException;

    public Map<String, String> getAllNameAgent() throws SQLException;

    public Map<String, ArrayList<String>> getAllAgent() throws SQLException;

    public boolean checkRefundFeeAgent(String var1, String var2);

    public List<RefundFeeAgentMessage> getLogRefundFeeAgent(String var1, String var2);

    public long countsearchAgentTranferMoney(String var1, String var2, String var3, String var4, String var5, String var6);

    public long totalMoneyVinReceiveFromAgent(String var1, String var2, String var3, String var4, String var5, String var6);

    public long totalMoneyVinSendFromAgent(String var1, String var2, String var3, String var4, String var5, String var6);

    public long totalMoneyVinFeeFromAgent(String var1, String var2, String var3, String var4, String var5, String var6);

    public List<LogAgentTranferMoneyResponse> searchAgentTranferMoneyVinSale(String var1, String var2, String var3, String var4, int var5, int var6);

    public long countSearchAgentTranferMoneyVinSale(String var1, String var2, String var3, String var4);

    public long totalMoneyVinReceiveFromAgentByStatus(String var1, String var2, String var3, String var4);

    public long totalMoneyVinSendFromAgentByStatus(String var1, String var2, String var3, String var4);

    public boolean updateTopDsFromAgent(String var1, String var2, String var3, String var4);

    public boolean updateTopDsFromAgentMySQL(String var1, String var2, String var3, String var4) throws SQLException;

    public boolean logBonusTopDS(BonusTopDSModel var1);

    public boolean checkBonusTopDS(String var1, String var2);

    public List<BonusTopDSModel> getLogBonusTopDS(String var1, String var2);

    public TranferMoneyAgent getTransferMoneyAgent(String var1, String var2, String var3);

    public List<AgentResponse> listUserAgentAdmin(String var1) throws SQLException;

    public List<LogAgentTranferMoneyResponse> searchAgentTranferMoney(String var1, String var2, String var3, String var4, String var5, int var6);

    public long countsearchAgentTranferMoney(String var1, String var2, String var3, String var4, String var5);

    public long totalMoneyVinReceiveFromAgent(String var1, String var2, String var3, String var4, String var5);

    public long totalMoneyVinSendFromAgent(String var1, String var2, String var3, String var4, String var5);

    public long totalMoneyVinFeeFromAgent(String var1, String var2, String var3, String var4, String var5);

    public List<AgentResponse> listUserAgentLevel2ByParentID(int var1) throws SQLException;

    public List<AgentResponse> listUserAgentLevel2ByID(int var1) throws SQLException;

    public List<AgentResponse> listUserAgentActive(String var1) throws SQLException;

    public List<AgentResponse> listUserAgentLevel1Active() throws SQLException;

    public List<LogAgentTranferMoneyResponse> searchAgentTongTranferMoney(String var1, String var2, String var3, String var4, String var5, String var6, int var7);

    public long countsearchAgentTongTranferMoney(String var1, String var2, String var3, String var4, String var5, String var6);

    public long totalMoneyVinReceiveFromAgentTong(String var1, String var2, String var3, String var4, String var5, String var6);

    public long totalMoneyVinSendFromAgentTong(String var1, String var2, String var3, String var4, String var5, String var6);

    public long totalMoneyVinFeeFromAgentTong(String var1, String var2, String var3, String var4, String var5, String var6);

    public List<AgentModel> getListPercentBonusVincard(String var1) throws SQLException;

    public int registerPercentBonusVincard(String var1, int var2) throws SQLException;

    public List<Map<String, Object>> getAgencies(String agencyCode, int page, int maxItem) throws SQLException ;
    
    public List<Map<String, Object>> getAgenciesLevel1(String agencyCode, int page, int maxItem) throws SQLException;

    public List<Map<String, Object>> getProfitComponents4Agency(String agencyCode, String currentMonth) throws SQLException, ParseException;
    
    public List<Map<String, Object>> getProfitComponentsNotAgency(String currentMonth) throws SQLException, ParseException;

    public List<Map<String, Object>> getMemberPassed4Agency(String agencyCode, String currentMonth) throws SQLException, ParseException;
    
    public int getCountMemberNotAgency(String currentMonth) throws SQLException, ParseException;

    public List<UserDetailAgentModel> getUserDetailAgent(List<String> fields, String nick_name,String fromTime,String endTime, String referCode) throws SQLException;

    public List<Map<String, Object>> getUserDetailAgent(List<String> fields, String nick_name, String currentTime, String referCode, int page, int maxItem) throws SQLException;

    public List<Map<String, Object>> getUserDetailAgent(List<String> fields, String nick_name, String fromTime, String endTime, String referCode, int page, int maxItem) throws SQLException;
    
    public List<Map<String, Object>> ExecuteByFields(List<String> fields, String nick_name, String fromTime,
			String endTime, String referCode, int page, int maxItem) throws SQLException;

    public List<Map<String, Object>> getUsers4Agent(String agencyCode, int page, int maxItem) throws SQLException;
    
    public Map<String, Object> getSumPlayInGame(String nick_name, String fromTime,
			String endTime, int page, int maxItem) throws SQLException;
    
    public Boolean checkExistAgencyByCode(String agencyCode) throws SQLException;
    
    public Boolean checkExistAgency(UserAgentModel userAgentModel) throws SQLException;
    
    public String updateAgencyCode4Userplay(String nickname, String agencyCode) throws SQLException;
    
    public int getTotalUserDeposit4Agency(String agencyCode, String fromTime, String endTime) throws SQLException;
    
    public int getTotalUserWithdraw4Agency(String agencyCode, String fromTime, String endTime) throws SQLException;
    
    public int getSumDeposit4Agency(String agencyCode, String fromTime, String endTime) throws SQLException;
    
    public int getSumWithdraw4Agency(String agencyCode, String fromTime, String endTime) throws SQLException;
    
    public List<Long> getSumWDandDO4Agency(String agencyCode, String fromTime, String endTime) throws SQLException;
    
    public List<Long> getSumWDandDO4Agency(String agencyCode, String currentMonth) throws SQLException;
    
    public int getTotalUserBet4Agency(String agencyCode, String fromTime, String endTime) throws SQLException;
    
    public int getTotalUserRegister4Agency(String agencyCode, String fromTime, String endTime) throws SQLException;
    
    public int getTotalUser4Agency(String agencyCode) throws SQLException;

    public List<Map<String, Object>> SynDataLogGameToReport(String currentTime, String gameName);
    
    public List<Map<String, Object>> reportUserPlay4Agent(String referral_code, String nick_name, String fromTime, String endTime, int page, int maxitem) throws SQLException;
    
    public List<String> getListCode() throws SQLException;
    
    public String getListCodeChildsById(int id) throws SQLException;
    
    public String getListCodeChildsByCode(String code) throws SQLException;
    
    public Map<String, Object> getAllChilds(Integer id, int pageIndex, int limit) throws SQLException;
    
    public Map<String, Object> searchChilds(Integer id, String keyword, int level, int pageIndex, int limit) throws SQLException;
    
    public List<UserAgentModel> getParents(Integer id) throws SQLException;
}

