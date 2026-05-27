/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.statistic.LoginPortalInfoMsg
 */
package com.vinplay.vbee.rmq.statistic.processor;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.statistic.LoginPortalInfoMsg;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.dao.impl.StatisticDaoImpl;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class LoginPortalInfoProcessor
implements BaseProcessor<byte[], Boolean> {
    public Boolean execute(Param<byte[]> param) {
        LoginPortalInfoMsg msg = (LoginPortalInfoMsg)BaseMessage.fromBytes((byte[])((byte[])param.get()));
        
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            MongoCollection<Document> col = db.getCollection("user_login_info");
            long totalLogins = col.countDocuments(new Document("user_name", msg.getUsername()));
            if (totalLogins > 0) {
                long ipCount = col.countDocuments(new Document("user_name", msg.getUsername()).append("ip", msg.getIp()));
                long deviceCount = col.countDocuments(new Document("user_name", msg.getUsername()).append("device_name", msg.getDeviceName()));
                
                List<String> reasons = new ArrayList<>();
                int riskScore = 0;
                
                if (ipCount == 0 && msg.getIp() != null) {
                    reasons.add("New IP detected");
                    riskScore += 50;
                }
                if (deviceCount == 0 && msg.getDeviceName() != null) {
                    reasons.add("New Device detected");
                    riskScore += 50;
                }
                
                if (riskScore > 0) {
                    msg.setAbnormal(true);
                    msg.setAbnormalReasons(reasons);
                    msg.setRiskScore(riskScore);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        StatisticDaoImpl dao = new StatisticDaoImpl();
        return dao.saveLoginPortalInfo(msg);
    }
}

