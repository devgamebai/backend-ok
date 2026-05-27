/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.core.type.TypeReference
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.fasterxml.jackson.databind.ObjectWriter
 *  com.vinplay.dal.service.impl.LogGameServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.LogGameMessage
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.vinplay.dal.service.impl.LogGameServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.LogGameMessage;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class GetLogGameDetailBySessionIDProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String sessionid = request.getParameter("sid");
        String gameName = request.getParameter("gn");
        String timelog = request.getParameter("tg");
        if (sessionid != null) {
            LogGameServiceImpl service = new LogGameServiceImpl();
            String json = "";
            try {
                LogGameMessage loggamedetail = service.getLogGameDetailBySessionID(sessionid, gameName, timelog);
                ObjectMapper mapper = new ObjectMapper();
                json = mapper.writerWithType((TypeReference)new TypeReference<LogGameMessage>(){}).writeValueAsString((Object)loggamedetail);
            }
            catch (JsonProcessingException e) {
                logger.debug((Object)e);
            }
            return json;
        }
        return "MISSING PARAMETTER";
    }

}

