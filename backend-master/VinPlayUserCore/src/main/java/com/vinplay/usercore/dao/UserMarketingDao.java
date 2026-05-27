/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.UserMarketingMessage
 *  com.vinplay.vbee.common.models.MarketingModel
 *  com.vinplay.vbee.common.models.cache.StatisticUserMarketing
 */
package com.vinplay.usercore.dao;

import com.vinplay.vbee.common.messages.UserMarketingMessage;
import com.vinplay.vbee.common.models.MarketingModel;
import com.vinplay.vbee.common.models.cache.StatisticUserMarketing;
import java.sql.SQLException;
import java.util.List;

public interface UserMarketingDao {
    public boolean saveUserMarketing(UserMarketingMessage var1);

    public boolean saveLoginDailyMarketing(UserMarketingMessage var1);

    public boolean updateLoginDailyMarketing(UserMarketingMessage var1);

    public List<UserMarketingMessage> getNickNameUserMarketing(String var1, String var2);

    public List<String> getCampaignList(String var1) throws SQLException;

    public List<String> getSourceList(String var1) throws SQLException;

    public List<String> getMediumList(String var1) throws SQLException;

    public void logMKTInfo(StatisticUserMarketing var1);

    public List<MarketingModel> getHistoryMKT(String var1, String var2, String var3, String var4, String var5);

    public MarketingModel getMKTInfo(String var1);
}

