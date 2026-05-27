/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.AgentServiceImpl
 *  com.vinplay.usercore.dao.impl.UserDaoImpl
 *  com.vinplay.usercore.service.impl.MoneyInGameServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.response.ResultAgentRespone
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors;

import com.vinplay.dal.service.impl.AgentServiceImpl;
import com.vinplay.usercore.dao.impl.UserDaoImpl;
import com.vinplay.usercore.service.impl.MoneyInGameServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.ResultAgentRespone;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class FixFreezeMoneyProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        ResultAgentRespone response = new ResultAgentRespone(false, "1001");
        AgentServiceImpl service = new AgentServiceImpl();
        HttpServletRequest request = (HttpServletRequest)param.get();
        String key = request.getParameter("k");
        try {
            UserDaoImpl userDaoImpl;
            List users;
            if (key != null && "gamebaiasd123".equals(key) && (users = (userDaoImpl = new UserDaoImpl()).GetNickNameFreeze()) != null && users.size() > 0) {
                for (Object _u : users) {
                    UserCacheModel user = (UserCacheModel) _u;
                    MoneyInGameServiceImpl migsi = new MoneyInGameServiceImpl();
                    migsi.restoreFreeze(user.getNickname(), "HamCaMap", "*", "*");
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.debug(e);
        }
        return response.toJson();
    }
}

