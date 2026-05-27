/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.LogMoneyUserMessage
 *  com.vinplay.vbee.common.models.cache.ReportModel
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.vbee.rmq.report.processor;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.models.cache.ReportModel;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.text.ParseException;
import org.apache.log4j.Logger;

public class ReportMoneyProcessor
implements BaseProcessor<byte[], Boolean> {
    private static final Logger logger = Logger.getLogger((String)"vbee");

    public Boolean execute(Param<byte[]> param) {
        LogMoneyUserMessage message = (LogMoneyUserMessage)BaseMessage.fromBytes((byte[])((byte[])param.get()));
        if (message.isBot()) {
            return true;
        }
        if (message.getMoneyType().equals("vin")) {
            if (message.getActionName().equals("TaiXiu") && (message.getServiceName().equals("T\u00e0i x\u1ec9u - T\u00e1n l\u1ed9c") || message.getServiceName().equals("T\u00e0i x\u1ec9u - R\u00fat l\u1ed9c"))) {
                return true;
            }
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            IMap reportMap = client.getMap("cacheReports");
            String date = "";
            try {
                date = VinPlayUtils.getDateFromDateTime((String)message.getCreateTime());
            }
            catch (ParseException e) {
                date = VinPlayUtils.getCurrentDate();
            }
            this.pushReportMap((IMap<String, ReportModel>)reportMap, message.getNickname(), message.getActionName(), date, message.getMoneyExchange(), message.getFee(), message.isVp(), message.isBot());
        }
        return true;
    }

    private void pushReportMap(IMap<String, ReportModel> reportMap, String nickname, String actionname, String date, long money, long fee, boolean playGame, boolean isBot) {
        String key = nickname + "," + actionname + "," + date;
        try {
            if (reportMap.containsKey((Object)key)) {
                try {
                    ReportModel reportModel = (ReportModel)reportMap.get((Object)key);
                    if (playGame) {
                        if (money > 0L) {
                            ReportModel reportModel2 = reportModel;
                            reportModel2.moneyWin += money;
                        } else {
                            ReportModel reportModel3 = reportModel;
                            reportModel3.moneyLost += money;
                        }
                    } else {
                        ReportModel reportModel4 = reportModel;
                        reportModel4.moneyOther += money;
                    }
                    ReportModel reportModel5 = reportModel;
                    reportModel5.fee += fee;
                    reportMap.put(key, reportModel);
                }
                catch (Exception e) {
                    logger.debug((Object)e);
                }
            } else {
                ReportModel reportModel = new ReportModel();
                reportModel.isBot = isBot;
                if (playGame) {
                    if (money > 0L) {
                        reportModel.moneyWin = money;
                    } else {
                        reportModel.moneyLost = money;
                    }
                } else {
                    reportModel.moneyOther = money;
                }
                reportModel.fee = fee;
                reportMap.put(key, reportModel);
            }
        }
        catch (Exception e) {
            logger.debug((Object)e);
        }
    }
}

