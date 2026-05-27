/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.entities.AttendanceConfig
 *  com.vinplay.usercore.service.impl.AttendanceServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.attendance;

import com.vinplay.usercore.entities.AttendanceConfig;
import com.vinplay.usercore.service.impl.AttendanceServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class HistoryProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"portal");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickname = request.getParameter("nn");
        String accessToken = request.getParameter("at");
        if (nickname == null || nickname.trim().isEmpty()) {
            return BaseResponse.error((String)"5", (String)"Nickname kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1eafng");
        }
        if (accessToken == null || accessToken.trim().isEmpty()) {
            return BaseResponse.error((String)"5", (String)"Access token kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1eafng");
        }
        int pageIndex = 1;
        try {
            pageIndex = Integer.parseInt(request.getParameter("pg"));
        }
        catch (Exception e) {
            pageIndex = 1;
        }
        int limitItem = 10;
        try {
            limitItem = Integer.parseInt(request.getParameter("mi"));
        }
        catch (Exception e) {
            limitItem = 10;
        }
        UserServiceImpl userService = new UserServiceImpl();
        boolean isToken = userService.isActiveToken(nickname, accessToken);
        if (!isToken) {
            return BaseResponse.error((String)"4", (String)"Phi\u00ean l\u00e0m vi\u1ec7c c\u1ee7a b\u1ea1n \u0111\u00e3 h\u1ebft h\u1ea1n , vui l\u00f2ng t\u1ea3i l\u1ea1i trang !");
        }
        try {
            AttendanceServiceImpl attendanceService = new AttendanceServiceImpl();
            AttendanceConfig attendanceConfig = attendanceService.getConfigLastest();
            Map map = new HashMap();
            map = attendanceService.search(Integer.valueOf(attendanceConfig.getId()), nickname, "", "", pageIndex, limitItem);
            Long totalRecord = Long.parseLong(map.get("totalRecord").toString());
            map.remove("totalRecord");
            return BaseResponse.success(map, (long)totalRecord);
        }
        catch (Exception e) {
            logger.trace(e);
            return BaseResponse.error((String)"1001", (String)"L\u1ed7i h\u1ec7 th\u1ed1ng. Vui l\u00f2ng li\u00ean h\u1ec7 b\u1ed9 ph\u1eadn CSKH \u0111\u1ec3 \u0111\u01b0\u1ee3c gi\u00fap \u0111\u1ee1.");
        }
    }
}

