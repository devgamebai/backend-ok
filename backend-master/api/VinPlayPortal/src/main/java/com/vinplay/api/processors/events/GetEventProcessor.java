/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.EventsServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.events;

import com.vinplay.api.processors.events.response.DSEventMoonResponse;
import com.vinplay.dal.service.impl.EventsServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class GetEventProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        DSEventMoonResponse response = new DSEventMoonResponse(false, "1001");
        EventsServiceImpl service = new EventsServiceImpl();
        try {
            List results = service.listEventsMoon();
            response.setLstMoonEvents(results);
            response.setSuccess(true);
            response.setErrorCode("0");
        }
        catch (Exception e) {
            this.logger.error("List event moon Error: ", (Throwable)e);
            return e.getMessage();
        }
        return response.toJson();
    }
}

