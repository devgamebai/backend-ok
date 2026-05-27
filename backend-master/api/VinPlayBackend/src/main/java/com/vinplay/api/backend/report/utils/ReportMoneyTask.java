/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.dal.dao.impl.ReportDaoImpl
 *  com.vinplay.dal.entities.report.ReportTotalMoneyModel
 *  com.vinplay.dal.service.LogPortalService
 *  com.vinplay.dal.service.impl.LogPortalServiceImpl
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.report.utils;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.api.backend.report.utils.ReportMoneyUtils;
import com.vinplay.dal.dao.impl.ReportDaoImpl;
import com.vinplay.dal.entities.report.ReportTotalMoneyModel;
import com.vinplay.dal.service.LogPortalService;
import com.vinplay.dal.service.impl.LogPortalServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import org.apache.log4j.Logger;

public class ReportMoneyTask
extends TimerTask {
    private static final Logger logger = Logger.getLogger((String)"report");
    private static LogPortalService service = new LogPortalServiceImpl();

    @Override
    public void run() {
        try {
            service.saveLog();
            Calendar cal = Calendar.getInstance();
            int hour = cal.get(11);
            int minute = cal.get(12);
            boolean remove = false;
            Date timeRun = new Date();
            String date = "";
            String today = VinPlayUtils.getCurrentDate();
            String yesterday = VinPlayUtils.getYesterday();
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            ReportDaoImpl dao = new ReportDaoImpl();
            if (hour == 3 && minute >= 0 && minute < 5) {
                ReportMoneyUtils.fixYesterdayData(today, yesterday);
                return;
            }
            if (hour != 1 || minute < 0 || minute >= 5) {
                String superAgent = GameCommon.getValueStr((String)"SUPER_AGENT");
                ReportTotalMoneyModel model = dao.getTotalMoney(superAgent);
                dao.saveLogTotalMoney(model);
                return;
            }
            remove = true;
            date = yesterday;
            logger.info("Save report " + date + " start at " + timeRun);
            IMap<String, Object> reportMap = client.getMap("cacheReports");
            for (Map.Entry entry : reportMap.entrySet()) {
                if (!((String)entry.getKey()).contains(date) || !remove) continue;
                reportMap.remove(entry.getKey());
            }
            logger.info((Object)("Save report " + timeRun + " success at " + new Date()));
        }
        catch (Exception e) {
            logger.debug((Object)e);
            e.printStackTrace();
        }
    }
}

