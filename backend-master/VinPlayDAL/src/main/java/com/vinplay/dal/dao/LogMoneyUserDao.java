/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.gamebai.LogNoHuGameBaiMessage
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.response.LogMoneyUserResponse
 *  com.vinplay.vbee.common.response.LogUserMoneyResponse
 */
package com.vinplay.dal.dao;

import com.vinplay.dal.entities.gamebai.TopGameBaiModel;
import com.vinplay.vbee.common.messages.gamebai.LogNoHuGameBaiMessage;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.LogMoneyUserResponse;
import com.vinplay.vbee.common.response.LogUserMoneyResponse;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface LogMoneyUserDao {
    public List<LogUserMoneyResponse> searchLogMoneyUser(String var1, String var2, String var3, String var4, String var5, String var6, String var7, int var8, int var9, int var10);

    public int countsearchLogMoneyUser(String var1, String var2, String var3, String var4, String var5, String var6, int var7);

    public List<LogMoneyUserResponse> getHistoryTransactionLogMoney(String var1, int var2, int var3);

    public List<LogMoneyUserResponse> getTransactionList(String var1, int var2, int var3, int var4);

    public int countHistoryTransactionLogMoney(String var1, int var2);

    public Map<String, TopGameBaiModel> getTopGameBai(String var1);

    public List<LogNoHuGameBaiMessage> getNoHuGameBaiHistory(int var1, String var2);

    public int countNoHuGameBaiHistory();

    public UserModel getUserByNickName(String var1) throws SQLException;

    public UserModel getUserById(long var1) throws SQLException;

    public List<LogUserMoneyResponse> searchLogMoneyTranferUser(String var1, String var2, String var3, String var4, int var5);

    public boolean UpdateProcessLogChuyenTienDaiLy(String var1, String var2, String var3, String var4);

    public boolean UpdateProcessLogChuyenTienDaiLyMySQL(String var1, String var2, String var3, String var4) throws SQLException;

    public com.vinplay.dal.model.LogMoneyUserVin4Report getLogMoneyUserVin4ReportWithListAction(String nick_name, String fromTime, String endTime, List<String> actions, int page, int maxItem);

    // ── Balance history feed (c=9972) ──────────────────────────────
    java.util.List<com.vinplay.vbee.common.balancehistory.BalanceHistoryRow>
        queryBalanceHistory(String nickName,
                            String fromTime,
                            String endTime,
                            java.util.Set<com.vinplay.vbee.common.balancehistory.BalanceHistoryCategory> categories,
                            int page,
                            int limit);

    long countBalanceHistory(String nickName,
                             String fromTime,
                             String endTime,
                             java.util.Set<com.vinplay.vbee.common.balancehistory.BalanceHistoryCategory> categories);

    com.vinplay.vbee.common.balancehistory.BalanceHistorySummary
        summarizeBalanceHistory(String nickName,
                                String fromTime,
                                String endTime);
}

