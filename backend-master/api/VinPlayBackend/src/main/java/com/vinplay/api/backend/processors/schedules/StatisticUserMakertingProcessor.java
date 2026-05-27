/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.CacheServiceImpl
 *  com.vinplay.usercore.service.impl.MarketingServiceImpl
 *  com.vinplay.usercore.utils.UserMakertingUtil
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 *  com.vinplay.vbee.common.models.cache.StatisticUserMarketing
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.schedules;

import com.vinplay.dal.service.impl.CacheServiceImpl;
import com.vinplay.usercore.service.impl.MarketingServiceImpl;
import com.vinplay.usercore.utils.UserMakertingUtil;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import com.vinplay.vbee.common.models.cache.StatisticUserMarketing;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class StatisticUserMakertingProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        CacheServiceImpl cacheService = new CacheServiceImpl();
        MarketingServiceImpl mktService = new MarketingServiceImpl();
        List<String> mediumList = mktService.getMediumList((String)null);
        List<String> sourceList = mktService.getSourceList((String)null);
        List<String> campaignList = mktService.getCampaignList((String)null);
        if (mediumList != null && sourceList != null && campaignList != null) {
            Date toDay = new Date();
            for (String m : mediumList) {
                for (String s : sourceList) {
                    for (String c : campaignList) {
                        String key = UserMakertingUtil.buildUserMakertingKey((String)c, (String)m, (String)s);
                        try {
                            StatisticUserMarketing sUM = (StatisticUserMarketing)cacheService.getObject(key);
                            if (sUM == null || sUM.getUpdateTime().compareTo(toDay) >= 0) continue;
                            mktService.statisticMKTInfo(sUM);
                            sUM.setNRU(0);
                            sUM.setPU(0);
                            sUM.setTotalNapVin(0L);
                            sUM.setTotalTieuVin(0L);
                            sUM.setUpdateTime(new Date());
                            cacheService.setObject(key, (Object)sUM);
                        }
                        catch (KeyNotFoundException sUM) {}
                    }
                }
            }
        }
        return "-1";
    }
}

