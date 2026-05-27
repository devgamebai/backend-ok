/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.models.HoanTraModel
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.hoantra;

import com.vinplay.api.processors.hoantra.ReturnMoney;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.HoanTraModel;
import com.vinplay.vbee.common.response.BaseResponse;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class CalculateDataHoanTraProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickname = request.getParameter("nn");
        String accessToken = request.getParameter("at");
        if (nickname == null || nickname.trim().isEmpty()) {
            return BaseResponse.error((String)"5", (String)"Nickname is not empty");
        }
        if (accessToken == null || accessToken.trim().isEmpty()) {
            return BaseResponse.error((String)"5", (String)"Access token is not empty");
        }
        BaseResponse res = new BaseResponse();
        res.setData(null);
        res.setTotalRecords(0L);
        res.setErrorCode("1001");
        res.setMessage("error");
        res.setSuccess(false);
        UserServiceImpl userService = new UserServiceImpl();
        boolean isToken = userService.isActiveToken(nickname, accessToken);
        if (!isToken) {
            res.setData("Phi\u00ean l\u00e0m vi\u1ec7c c\u1ee7a b\u1ea1n \u0111\u00e3 h\u1ebft h\u1ea1n , vui l\u00f2ng t\u1ea3i l\u1ea1i trang !");
            res.setErrorCode("4");
            return res.toJson();
        }
        String dateStr = request.getParameter("date");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return BaseResponse.error((String)"5", (String)"Time not empty");
        }
        Date date = null;
        try {
            date = new Date(dateFormat.parse(dateStr).getTime());
        }
        catch (Exception e) {
            res.setData("time is invalid");
            res.setErrorCode("4");
            return res.toJson();
        }
        try {
            Date maxDate = new Date(Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()).getTime());
            Date minDate = new Date(Date.from(LocalDate.now().plusDays(-7L).atStartOfDay(ZoneId.systemDefault()).toInstant()).getTime());
            if (date.getTime() >= maxDate.getTime() || dateFormat.parse(dateStr).getTime() < minDate.getTime()) {
                res.setData(("Time have between: " + dateFormat.format(minDate) + " and " + dateFormat.format(maxDate)));
                res.setErrorCode("4");
                return res.toJson();
            }
            if (new ReturnMoney().countListHoanTraHistories(date, nickname) > 0L) {
                res.setErrorCode("1002");
                res.setData(("B\u1ea1n \u0111\u00e3 nh\u1eadn ho\u00e0n tr\u1ea3 ng\u00e0y: " + date.toString()));
                return res.toJson();
            }
            if (new ReturnMoney().countListHoanTra(date, nickname) < 1L) {
                res.setErrorCode("1003");
                res.setData("Th\u00f4ng tin ho\u00e0n tr\u1ea3 ch\u01b0a \u0111\u01b0\u1ee3c m\u1edf. Vui l\u00f2ng li\u00ean h\u1ec7 b\u1ed9 ph\u1ea7n CSKH \u0111\u1ec3 \u0111\u01b0\u1ee3c gi\u00fap \u0111\u1ee1");
                return res.toJson();
            }
            List<HoanTraModel> hoanTraModels = new ReturnMoney().getListHoanTra(date, nickname);
            long sumCasino = 0L;
            long sumSport = 0L;
            long sumGame = 0L;
            for (HoanTraModel hoantra : hoanTraModels) {
                sumCasino += hoantra.hoan_tra_casino;
                sumSport += hoantra.hoan_tra_sport;
                sumGame += hoantra.hoan_tra_game;
            }
            if (sumCasino + sumSport + sumGame == 0L) {
                res.setErrorCode("1004");
                res.setData(("B\u1ea1n kh\u00f4ng \u0111\u1ee7 \u0111i\u1ec1u ki\u1ec7n nh\u1eadn ho\u00e0n tr\u1ea3 ng\u00e0y: " + date.toString()));
                return res.toJson();
            }
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("nickname", nickname);
            map.put("sumCasino", sumCasino);
            map.put("sumSport", sumSport);
            map.put("sumGame", sumGame);
            map.put("totalMoney", sumCasino + sumSport + sumGame);
            return res.success(map);
        }
        catch (Exception e) {
            logger.trace(e);
            return BaseResponse.error((String)"-1", (String)e.getMessage());
        }
    }
}

