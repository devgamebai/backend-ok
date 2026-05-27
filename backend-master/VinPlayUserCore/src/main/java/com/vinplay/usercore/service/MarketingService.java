/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.UserMarketingMessage
 *  com.vinplay.vbee.common.models.MarketingModel
 *  com.vinplay.vbee.common.models.cache.StatisticUserMarketing
 */
package com.vinplay.usercore.service;

import com.vinplay.vbee.common.messages.UserMarketingMessage;
import com.vinplay.vbee.common.models.MarketingModel;
import com.vinplay.vbee.common.models.cache.StatisticUserMarketing;
import java.sql.SQLException;
import java.util.List;

public interface MarketingService {
    public boolean saveUserMarketing(UserMarketingMessage var1);

    public void loginMarketing(String var1);

    public List<String> getMediumList(String var1);

    public List<String> getSourceList(String var1);

    public List<String> getCampaignList(String var1);

    public void statisticMKTInfo(StatisticUserMarketing var1);

    public List<MarketingModel> getHistoryMKT(String var1, String var2, String var3, String var4, String var5);

    public List<MarketingModel> getMKTInfo(String var1, String var2, String var3, String var4, String var5) throws SQLException;

    public MarketingModel getMKTInfo(String var1);
}

