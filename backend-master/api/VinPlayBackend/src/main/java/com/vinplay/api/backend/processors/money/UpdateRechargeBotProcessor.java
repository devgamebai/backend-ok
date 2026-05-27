/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.dal.dao.impl.ReportDaoImpl
 *  com.vinplay.dal.dao.impl.UserDaoImpl
 *  com.vinplay.dal.service.impl.BotServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors.money;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.dao.impl.ReportDaoImpl;
import com.vinplay.dal.dao.impl.UserDaoImpl;
import com.vinplay.dal.service.impl.BotServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class UpdateRechargeBotProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        block10 : {
            HttpServletRequest request = (HttpServletRequest)param.get();
            try {
                String nickname = request.getParameter("nn");
                String sMoney = request.getParameter("mn");
                BotServiceImpl botService = new BotServiceImpl();
                if (nickname == null || nickname.isEmpty()) {
                    return "MISSING PARAM";
                }
                long money = Long.parseLong(sMoney);
                ReportDaoImpl rDao = new ReportDaoImpl();
                if (!rDao.checkBot(nickname)) {
                    return "NOT BOT";
                }
                UserDaoImpl dao = new UserDaoImpl();
                if (!dao.updateRechargeMoney(nickname, money)) break block10;
                HazelcastInstance client = HazelcastClientFactory.getInstance();
                IMap userMap = client.getMap("users");
                if (!userMap.containsKey((Object)nickname)) {
                    return "SUCCESS";
                }
                UserCacheModel usercheck = (UserCacheModel)userMap.get((Object)nickname);
                if (usercheck.getId() == 0) {
                    userMap.remove((Object)nickname);
                    botService.login(nickname);
                }
                try {
                    userMap.lock((Object)nickname);
                    UserCacheModel user = (UserCacheModel)userMap.get((Object)nickname);
                    user.setRechargeMoney(money);
                    userMap.put((Object)nickname, (Object)user);
                }
                catch (Exception e) {
                    logger.debug((Object)e);
                    return "ERROR";
                }
                try {
                    userMap.unlock((Object)nickname);
                }
                catch (Exception e) {}
            }
            catch (Exception e2) {
                logger.debug((Object)e2);
            }
        }
        return "ERROR";
    }
}

