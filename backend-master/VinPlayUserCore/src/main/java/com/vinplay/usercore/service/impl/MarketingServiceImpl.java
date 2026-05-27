/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 *  com.vinplay.vbee.common.messages.UserMarketingMessage
 *  com.vinplay.vbee.common.models.MarketingModel
 *  com.vinplay.vbee.common.models.cache.StatisticUserMarketing
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.usercore.service.impl;

import com.vinplay.usercore.dao.UserMarketingDao;
import com.vinplay.usercore.dao.impl.UserMarketingDaoImpl;
import com.vinplay.usercore.service.MarketingService;
import com.vinplay.usercore.service.impl.CacheServiceImpl;
import com.vinplay.usercore.utils.UserMakertingUtil;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import com.vinplay.vbee.common.messages.UserMarketingMessage;
import com.vinplay.vbee.common.models.MarketingModel;
import com.vinplay.vbee.common.models.cache.StatisticUserMarketing;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.log4j.Logger;

public class MarketingServiceImpl
implements MarketingService {
    private UserMarketingDao dao = new UserMarketingDaoImpl();
    private Logger logger = Logger.getLogger((String)"backend");

    @Override
    public boolean saveUserMarketing(UserMarketingMessage message) {
        this.dao.saveUserMarketing(message);
        return true;
    }

    @Override
    public void loginMarketing(String nickName) {
        String timeLog = VinPlayUtils.getCurrentDateMarketing();
        List<UserMarketingMessage> lstmar = this.dao.getNickNameUserMarketing(nickName, timeLog);
        int index = 0;
        UserMarketingMessage message = new UserMarketingMessage();
        if (lstmar.size() > 0) {
            for (UserMarketingMessage numlog : lstmar) {
                index = numlog.numLogin + 1;
            }
            message.userName = nickName;
            message.numLogin = index;
            message.timeLogin = timeLog;
            this.dao.updateLoginDailyMarketing(message);
        } else {
            message.userName = nickName;
            message.numLogin = 1;
            message.timeLogin = timeLog;
            this.dao.saveLoginDailyMarketing(message);
        }
    }

    @Override
    public List<String> getMediumList(String medium) {
        try {
            return this.dao.getMediumList(medium);
        }
        catch (SQLException ex) {
            return null;
        }
    }

    @Override
    public List<String> getSourceList(String source) {
        try {
            return this.dao.getSourceList(source);
        }
        catch (SQLException ex) {
            return null;
        }
    }

    @Override
    public List<String> getCampaignList(String campaign) {
        try {
            return this.dao.getCampaignList(campaign);
        }
        catch (SQLException ex) {
            return null;
        }
    }

    @Override
    public void statisticMKTInfo(StatisticUserMarketing entry) {
        this.dao.logMKTInfo(entry);
    }

    @Override
    public List<MarketingModel> getMKTInfo(String campaign, String medium, String source, String startDate, String endDate) throws SQLException {
        List<MarketingModel> tmp;
        this.logger.debug(("c= " + campaign + ", m= " + medium + ", s= " + source));
        MarketingServiceImpl mktService = new MarketingServiceImpl();
        ArrayList<MarketingModel> results = new ArrayList<MarketingModel>();
        CacheServiceImpl cacheService = new CacheServiceImpl();
        UserMarketingDaoImpl dao = new UserMarketingDaoImpl();
        List<String> campaigns = dao.getCampaignList(campaign);
        List<String> mediums = dao.getMediumList(medium);
        List<String> sources = dao.getSourceList(source);
        String toDay = DateTimeUtils.getToDayAsDate();
        if (endDate.equalsIgnoreCase(toDay)) {
            this.logger.debug(("c= " + campaigns + ", m= " + mediums + ", s= " + sources));
            for (String m : mediums) {
                for (String s : sources) {
                    for (String c : campaigns) {
                        String key = UserMakertingUtil.buildUserMakertingKey(c, m, s);
                        try {
                            StatisticUserMarketing sUM = (StatisticUserMarketing)cacheService.getObject(key);
                            if (sUM != null) {
                                MarketingModel model = new MarketingModel(c, m, s, sUM.getNRU(), sUM.getPU(), sUM.getTotalNapVin(), toDay);
                                results.add(model);
                                continue;
                            }
                            this.logger.debug(("Cannot find key MKT info: " + key));
                        }
                        catch (KeyNotFoundException e) {
                            this.logger.error(("GET MKT Info: Key " + key + " not found"));
                        }
                    }
                }
            }
        }
        if ((tmp = mktService.getHistoryMKT(campaign, medium, source, startDate, endDate)) != null) {
            results.addAll(tmp);
        }
        return results;
    }

    @Override
    public List<MarketingModel> getHistoryMKT(String campaign, String medium, String source, String startDate, String endDate) {
        return this.dao.getHistoryMKT(campaign, medium, source, startDate, endDate);
    }

    @Override
    public MarketingModel getMKTInfo(String username) {
        return this.dao.getMKTInfo(username);
    }
}

