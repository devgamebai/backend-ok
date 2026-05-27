/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.gamebase.entities.EventMission
 *  com.gamebase.service.impl.EventMissionServiceImpl
 *  com.vinplay.otp.service.AccountSecurityService
 *  com.vinplay.otp.service.impl.AccountSecurityServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.models.HtmlTemple
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.mission;

import com.gamebase.entities.EventMission;
import com.gamebase.service.impl.EventMissionServiceImpl;
import com.vinplay.api.processors.common.AuthProcessor;
import com.vinplay.api.utils.PortalUtils;
import com.vinplay.otp.service.AccountSecurityService;
import com.vinplay.otp.service.impl.AccountSecurityServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.HtmlTemple;
import com.vinplay.vbee.common.response.BaseResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class GetListEventMissionProcessor
extends AuthProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");
    AccountSecurityService service = new AccountSecurityServiceImpl();

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        EventMissionServiceImpl eventMissionService = new EventMissionServiceImpl();
        try {
            List eventMissionList = eventMissionService.getEventMissionListUser();
            for (Object _em : eventMissionList) {
                EventMission eventMission = (EventMission) _em;
                eventMission.setContent(this.buildContent(eventMission));
            }
            return BaseResponse.success((String)"0", (String)"", eventMissionList);
        }
        catch (Exception e) {
            logger.debug(e);
            return BaseResponse.error((String)"99", (String)"Server Error!");
        }
    }

    private String buildContent(EventMission eventMission) throws Exception {
        HtmlTemple htmlTemple = PortalUtils.loadHtmlTemple();
        String content = htmlTemple.getEvent().replace("[content]", eventMission.getContent());
        return String.format("data:text/html,%s", content);
    }
}

