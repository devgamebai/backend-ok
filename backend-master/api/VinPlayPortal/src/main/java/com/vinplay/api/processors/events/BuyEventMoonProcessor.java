/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.entities.event.MoonEventModel
 *  com.vinplay.dal.service.impl.EventsServiceImpl
 *  com.vinplay.usercore.service.impl.UserBonusServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.models.UserBonusModel
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.statics.TransType
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.events;

import com.vinplay.api.processors.events.response.BuyEventMoonRespinse;
import com.vinplay.dal.entities.event.MoonEventModel;
import com.vinplay.dal.service.impl.EventsServiceImpl;
import com.vinplay.usercore.service.impl.UserBonusServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.models.UserBonusModel;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class BuyEventMoonProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        BuyEventMoonRespinse response = new BuyEventMoonRespinse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        try {
            String ip = this.getIpAddress(request);
            String nickName = request.getParameter("nn");
            int eventId = Integer.parseInt(request.getParameter("id"));
            EventsServiceImpl service = new EventsServiceImpl();
            MoonEventModel results = service.buyPackEventMoon(nickName, eventId);
            response.setSuccess(true);
            response.setErrorCode(String.valueOf(results.getErrorCode()));
            if (results.getErrorCode() == 0) {
                long bonus = 0L;
                UserBonusServiceImpl userBonusService = new UserBonusServiceImpl();
                UserServiceImpl userService = new UserServiceImpl();
                switch ((int)results.getAmount()) {
                    case 500000: {
                        bonus = 99000L;
                        break;
                    }
                    case 1999000: {
                        bonus = 199000L;
                        break;
                    }
                    case 7999000: {
                        bonus = 999000L;
                        break;
                    }
                    default: {
                        bonus = 0L;
                    }
                }
                UserBonusModel model = new UserBonusModel(nickName, Integer.valueOf(eventId), Double.valueOf(bonus), null, ip, "Khuy\u1ebfn m\u00e3i MOON EVENT " + eventId);
                userBonusService.insertBonus(model);
                MoneyResponse res = userService.updateMoney(nickName, bonus, "vin", Games.MOON_NIGHT.getName(), Games.MOON_NIGHT.getId() + "", "MOON_EVENT", 0L, null, TransType.NO_VIPPOINT);
                response.setMoney(res.getCurrentMoney());
            }
        }
        catch (Exception e) {
            this.logger.error("List event moon Error: ", (Throwable)e);
            return e.getMessage();
        }
        return response.toJson();
    }

    private String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }
}

