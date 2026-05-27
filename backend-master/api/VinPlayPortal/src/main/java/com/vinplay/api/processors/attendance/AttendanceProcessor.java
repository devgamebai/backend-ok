/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.vinplay.usercore.entities.UserAttendance
 *  com.vinplay.usercore.service.impl.AttendanceServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.commons.lang.StringUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.attendance;

import com.google.gson.Gson;
import com.vinplay.usercore.entities.UserAttendance;
import com.vinplay.usercore.service.impl.AttendanceServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class AttendanceProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"portal");

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-FORWARDED-FOR");
        if (ip == null) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && !"".equals(ip)) {
            String[] arrayIp = ip.split(",");
            for (int i = 0; i < (arrayIp.length > 2 ? 2 : arrayIp.length); ++i) {
                if (arrayIp[i].length() > 40) continue;
                ip = arrayIp[i].trim();
                break;
            }
        }
        return ip;
    }

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickname = request.getParameter("nn");
        String accessToken = request.getParameter("at");
        String action = request.getParameter("ac");
        if (nickname == null || nickname.trim().isEmpty()) {
            return BaseResponse.error((String)"5", (String)"Nickname can not empty");
        }
        if (StringUtils.isBlank((String)accessToken)) {
            return BaseResponse.error((String)"5", (String)"Access token can not empty");
        }
        if (StringUtils.isBlank((String)action)) {
            return BaseResponse.error((String)"5", (String)"Action can not empty");
        }
        if (!("get".equals(action) || "receive".equals(action) || "history".equals(action))) {
            return BaseResponse.error((String)"5", (String)"Thao t\u00e1c kh\u00f4ng \u0111\u00fang");
        }
        UserServiceImpl userService = new UserServiceImpl();
        boolean isToken = userService.isActiveToken(nickname, accessToken);
        if (!isToken) {
            return BaseResponse.error((String)"4", (String)"Your trading session is expired, please reload page and login again.");
        }
        String ip = this.getIpAddress(request);
        BaseResponse res = new BaseResponse();
        try {
            AttendanceServiceImpl attendanceService = new AttendanceServiceImpl();
            Map map = new HashMap();
            List rs = new ArrayList();
            switch (action) {
                case "get": {
                    map = attendanceService.CheckAttendance(nickname, ip);
                    break;
                }
                case "receive": {
                    map = attendanceService.Attendance(nickname, ip);
                    break;
                }
                case "history": {
                    rs = attendanceService.historyInRound(nickname);
                }
            }
            if ("history".equals(action)) {
                res.setData(rs);
                res.setErrorCode(rs == null ? "1001" : "0");
                res.setMessage(null);
                res.setSuccess(rs != null);
                return res.toJson();
            }
            String code = "";
            code = map.get("code").toString();
            String msg = "";
            msg = map.get("msg").toString();
            int consecutive = 0;
            if ("get".equals(action)) {
                consecutive = Integer.parseInt(map.get("consecutive").toString());
            }
            switch (code) {
                case "exist": {
                    res.setData(("get".equals(action) ? Integer.valueOf(consecutive) : null));
                    res.setErrorCode("1002");
                    res.setMessage(msg);
                    res.setSuccess(false);
                    return res.toJson();
                }
                case "invalid": {
                    res.setData(("get".equals(action) ? Integer.valueOf(consecutive) : null));
                    res.setErrorCode("1003");
                    res.setMessage(msg);
                    res.setSuccess(false);
                    return res.toJson();
                }
                case "success": {
                    Gson g = new Gson();
                    if ("receive".equals(action)) {
                        UserAttendance p = (UserAttendance)g.fromJson(msg, UserAttendance.class);
                        res.setData(p);
                    } else {
                        res.setData(consecutive);
                    }
                    res.setErrorCode("0");
                    res.setMessage("get".equals(action) ? msg : null);
                    res.setSuccess(true);
                    return res.toJson();
                }
            }
            res.setData(("get".equals(action) ? Integer.valueOf(consecutive) : null));
            res.setErrorCode("1001");
            res.setMessage(msg);
            res.setSuccess(false);
            return res.toJson();
        }
        catch (Exception e) {
            logger.trace(e);
            res.setData(("get".equals(action) ? Integer.valueOf(0) : null));
            res.setErrorCode("1001");
            res.setMessage("Exception, please contact customer care support for help.");
            res.setSuccess(false);
            return res.toJson();
        }
    }
}

