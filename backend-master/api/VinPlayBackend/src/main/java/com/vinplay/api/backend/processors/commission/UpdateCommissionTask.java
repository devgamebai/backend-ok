/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.usercore.dao.impl.UserDaoImpl
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.commission.AgentCommissionModel
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors.commission;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.usercore.dao.impl.UserDaoImpl;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.commission.AgentCommissionModel;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import org.apache.log4j.Logger;

public class UpdateCommissionTask
extends TimerTask {
    private static final Logger logger = Logger.getLogger((String)"backend");

    @Override
    public void run() {
        try {
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            IMap<String, AgentCommissionModel> agentCommMap = client.getMap("cacheAgentCommission");
            UserDaoImpl dao = new UserDaoImpl();

            for (Map.Entry commission : agentCommMap.entrySet()) {
                try {
                    agentCommMap.lock((String)commission.getKey());
                    AgentCommissionModel model = (AgentCommissionModel)commission.getValue();
                    
                    dao.insertCommission(model.getUserId(), model.getNickName(), model.getFee(), this.getMonth());
                    model.setFee(0L);
                    model.setLastActive(new Date());
                    agentCommMap.put((String)commission.getKey(), model);
                }
                catch (Exception e) {
                    logger.debug((Object)e);
                    e.printStackTrace();
                    continue;
                }
                try {
                    agentCommMap.unlock((String)commission.getKey());
                }
                catch (Exception e) {}
            }
        }
        catch (Exception e2) {
            logger.debug((Object)e2);
            e2.printStackTrace();
        }
    }

    private String getMonth() {
        Calendar c = Calendar.getInstance();
        c.add(5, -1);
        Date date = c.getTime();
        return DateTimeUtils.getFormatTime((String)"MM/yyyy", (Date)date);
    }
}

