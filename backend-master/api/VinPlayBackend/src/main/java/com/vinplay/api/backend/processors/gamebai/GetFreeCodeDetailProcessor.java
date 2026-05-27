/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.vinplay.usercore.service.impl.GameTourServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.gamebai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.usercore.service.impl.GameTourServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class GetFreeCodeDetailProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        try {
            int id = Integer.parseInt(request.getParameter("id"));
            String code = request.getParameter("co");
            int packageId = Integer.parseInt(request.getParameter("pkid"));
            String gamename = request.getParameter("gn");
            int amount = Integer.parseInt(request.getParameter("am"));
            int codeType = Integer.parseInt(request.getParameter("ct"));
            int status = Integer.parseInt(request.getParameter("st"));
            String nickname = request.getParameter("nn");
            String addInfo = request.getParameter("adi");
            String startTime = request.getParameter("ts");
            String endTime = request.getParameter("te");
            int timeType = Integer.parseInt(request.getParameter("type"));
            String admin = request.getParameter("adm");
            String accessToken = request.getParameter("at");
            if (accessToken == null || accessToken.isEmpty()) accessToken = request.getParameter("aat");
            List details = new ArrayList();
            if (admin != null && accessToken != null) {
                String[] arr;
                for (String am : arr = GameCommon.getValueStr((String)"SUPER_ADMIN").split(",")) {
                    UserServiceImpl userSer;
                    if (!am.equals(admin) || !(userSer = new UserServiceImpl()).checkAccesstoken(admin, accessToken)) continue;
                    GameTourServiceImpl ser = new GameTourServiceImpl();
                    details = ser.getFreeCodeDetails(id, code, packageId, gamename, amount, codeType, status, nickname, addInfo, startTime, endTime, timeType);
                }
            }
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(details);
        }
        catch (Exception e) {
            return e.getMessage();
        }
    }
}

