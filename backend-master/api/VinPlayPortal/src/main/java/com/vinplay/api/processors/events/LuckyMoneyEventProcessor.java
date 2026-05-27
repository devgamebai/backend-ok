/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.UserBonusServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.models.UserBonusModel
 *  com.vinplay.vbee.common.response.BaseResponse
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.statics.TransType
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.commons.lang.StringUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.events;

import com.vinplay.usercore.service.impl.UserBonusServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.models.UserBonusModel;
import com.vinplay.vbee.common.response.BaseResponse;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class LuckyMoneyEventProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private Logger logger = Logger.getLogger((String)"api");
    private static final SplittableRandom rdom = new SplittableRandom();

    private long getAmount() {
        int r = rdom.nextInt(100) + 1;
        if (r == 1) {
            return rdom.nextBoolean() ? 40000L : 50000L;
        }
        if (r == 2 || r == 3) {
            return rdom.nextBoolean() ? 30000L : 40000L;
        }
        if (r == 4 || r == 5 || r == 6) {
            return rdom.nextBoolean() ? 30000L : 20000L;
        }
        if (r >= 7 && r <= 10) {
            return rdom.nextBoolean() ? 10000L : 20000L;
        }
        if (r > 10 && r <= 25) {
            return rdom.nextBoolean() ? 10000L : 5000L;
        }
        return rdom.nextInt(5001);
    }

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        try {
            String ip = this.getIpAddress(request);
            String nickName = request.getParameter("nn");
            String accessToken = request.getParameter("at");
            String action = request.getParameter("ac");
            if (nickName == null || nickName.trim().isEmpty()) {
                return BaseResponse.error((String)"5", (String)"Nickname kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1eafng");
            }
            if (StringUtils.isBlank((String)accessToken)) {
                return BaseResponse.error((String)"5", (String)"M\u00e3 phi\u00ean l\u00e0m vi\u1ec7c kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1eafng");
            }
            if (StringUtils.isBlank((String)action)) {
                return BaseResponse.error((String)"5", (String)"Thao t\u00e1c kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1eafng");
            }
            if (!("get".equals(action) || "receive".equals(action) || "time".equals(action))) {
                return BaseResponse.error((String)"5", (String)"Thao t\u00e1c kh\u00f4ng \u0111\u00fang");
            }
            UserServiceImpl userService = new UserServiceImpl();
            boolean isToken = userService.isActiveToken(nickName, accessToken);
            if (!isToken) {
                return BaseResponse.error((String)"4", (String)"Phi\u00ean l\u00e0m vi\u1ec7c c\u1ee7a b\u1ea1n \u0111\u00e3 h\u1ebft h\u1ea1n , vui l\u00f2ng t\u1ea3i l\u1ea1i trang !");
            }
            BaseResponse<Object> res = new BaseResponse<Object>();
            int startHour = 16;
            int endHour = 17;
            res = this.checkCondition(nickName, startHour, endHour);
            switch (action) {
                case "get": {
                    return res.toJson();
                }
                case "time": {
                    return LuckyMoneyEventProcessor.getCountDown(nickName, startHour, endHour).toJson();
                }
                case "receive": {
                    if (!res.getErrorCode().equals("0")) {
                        return res.toJson();
                    }
                    long bonus = this.getAmount();
                    UserBonusServiceImpl userBonusService = new UserBonusServiceImpl();
                    UserBonusModel model = new UserBonusModel(nickName, Integer.valueOf(Games.LUCKY_MONEY.getId()), Double.valueOf(bonus), null, ip, "BONUS LUCKY MONEY " + Games.LUCKY_MONEY.getName());
                    userBonusService.insertBonus(model);
                    MoneyResponse moneyResponse = userService.updateMoney(nickName, bonus, "vin", Games.LUCKY_MONEY.getName(), Games.LUCKY_MONEY.getId() + "", "LUCKY_MONEY", 0L, null, TransType.NO_VIPPOINT);
                    if (moneyResponse.getErrorCode().equals("0")) {
                        res.setData(bonus);
                        return res.toJson();
                    }
                    res.setData("Add money fail");
                    res.setErrorCode("1005");
                    res.setMessage("C\u1ed9ng ti\u1ec1n th\u01b0\u1edfng kh\u00f4ng th\u00e0nh c\u00f4ng. Vui l\u00f2ng li\u00ean h\u1ec7 b\u1ed9 ph\u1eadn ch\u0103m s\u00f3c kh\u00e1ch h\u00e0ng \u0111\u1ec3 \u0111\u01b0\u1ee3c gi\u00fap \u0111\u1ee1");
                    res.setSuccess(false);
                    return res.toJson();
                }
            }
            return BaseResponse.error((String)"5", (String)"Thao t\u00e1c kh\u00f4ng \u0111\u00fang");
        }
        catch (Exception e) {
            this.logger.error("Lucky money error: ", (Throwable)e);
            BaseResponse res = new BaseResponse();
            res.setData("Exception");
            res.setErrorCode("1001");
            res.setMessage(e.getMessage());
            res.setSuccess(false);
            return res.toJson();
        }
    }

    private String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    private BaseResponse<Object> checkCondition(String nickname, int startHour, int endHour) {
        BaseResponse res = new BaseResponse();
        UserBonusServiceImpl userBonusService = new UserBonusServiceImpl();
        boolean rs = false;
        rs = userBonusService.checkExit(nickname, Games.LUCKY_MONEY.getId());
        if (rs) {
            res.setData("Received bonus");
            res.setErrorCode("1003");
            res.setMessage("B\u1ea1n \u0111\u00e3 nh\u1eadn ph\u1ea7n th\u01b0\u1edfng ng\u00e0y h\u00f4m nay r\u1ed3i.");
            res.setSuccess(false);
            return res;
        }
        rs = userBonusService.checkConditionsByCurrentTime(nickname);
        if (!rs) {
            res.setData("Not passed conditions");
            res.setErrorCode("1004");
            res.setMessage("B\u1ea1n kh\u00f4ng \u0111\u1ee7 \u0111i\u1ec1u ki\u1ec7n \u0111\u1ec3 nh\u1eadn ph\u1ea7n th\u01b0\u1edfng. Vui l\u00f2ng \u0111\u1ecdc th\u1ec3 l\u1ec7 v\u00e0 ho\u00e0n th\u00e0nh c\u00e1c y\u00eau c\u1ea7u \u0111\u1ec3 \u0111\u01b0\u1ee3c nh\u1eadn th\u01b0\u1edfng");
            res.setSuccess(false);
            return res;
        }
        if (LocalDateTime.now().getHour() >= endHour && LocalDateTime.now().getSecond() > 0 || LocalDateTime.now().getHour() > endHour) {
            res.setData("Over time");
            res.setErrorCode("1005");
            res.setMessage("Khung gi\u1edd nh\u1eadn th\u01b0\u1edfng \u0111\u00e3 \u0111\u00f3ng, vui l\u00f2ng ch\u1edd t\u1edbi khung gi\u1edd ti\u1ebfp theo \u0111\u1ec3 nh\u1eadn th\u01b0\u1edfng.");
            res.setSuccess(false);
            return res;
        }
        if (LocalDateTime.now().getHour() < startHour) {
            LocalDateTime dateTime = LocalDateTime.parse(new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + "T" + startHour + ":00:00");
            res.setData((dateTime.toEpochSecond(ZoneOffset.UTC) - LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)));
            res.setErrorCode("1002");
            res.setMessage("Khung gi\u1edd nh\u1eadn th\u01b0\u1edfng ch\u01b0a \u0111\u01b0\u1ee3c m\u1edf, vui l\u00f2ng ch\u1edd t\u1edbi khung gi\u1edd nh\u1eadn th\u01b0\u1edfng.");
            res.setSuccess(false);
            return res;
        }
        res.setErrorCode("0");
        res.setMessage("success");
        res.setData("success");
        res.setSuccess(true);
        return res;
    }

    public static BaseResponse<Object> getCountDown(String nickname, int startHour, int endHour) {
        BaseResponse res = new BaseResponse();
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            HashMap<String, Object> data = new HashMap<String, Object>();
            LocalDateTime currentTime = LocalDateTime.now().plusSeconds(1L);
            if (currentTime.getHour() < startHour) {
                LocalDateTime targetTime = LocalDateTime.parse(new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + "T" + startHour + ":00:00");
                currentTime = LocalDateTime.now().plusSeconds(1L);
                data.put("currentTime", formatter.format(currentTime));
                data.put("countTime", targetTime.toEpochSecond(ZoneOffset.UTC) - currentTime.toEpochSecond(ZoneOffset.UTC));
                res.setData(data);
                res.setErrorCode("1002");
                res.setMessage(null);
                res.setSuccess(false);
                return res;
            }
            if (currentTime.getHour() >= endHour && currentTime.getSecond() > 0 || currentTime.getHour() > endHour) {
                LocalDateTime targetTime = LocalDateTime.parse(DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusDays(1L)) + "T" + startHour + ":00:00");
                currentTime = LocalDateTime.now().plusSeconds(1L);
                data.put("currentTime", formatter.format(currentTime));
                data.put("countTime", targetTime.toEpochSecond(ZoneOffset.UTC) - currentTime.toEpochSecond(ZoneOffset.UTC));
                res.setData(data);
                res.setErrorCode("1005");
                res.setMessage(null);
                res.setSuccess(false);
                return res;
            }
            UserBonusServiceImpl userBonusService = new UserBonusServiceImpl();
            boolean rs = false;
            rs = userBonusService.checkExit(nickname, Games.LUCKY_MONEY.getId());
            if (rs) {
                res.setData("Received bonus");
                res.setErrorCode("1003");
                res.setMessage("B\u1ea1n \u0111\u00e3 nh\u1eadn ph\u1ea7n th\u01b0\u1edfng ng\u00e0y h\u00f4m nay r\u1ed3i.");
                res.setSuccess(false);
                return res;
            }
            res.setData(null);
            res.setErrorCode("0");
            res.setMessage(null);
            res.setSuccess(true);
            return res;
        }
        catch (Exception e) {
            res.setData(null);
            res.setErrorCode("1001");
            res.setMessage(null);
            res.setSuccess(false);
            return res;
        }
    }

    public static void main(String[] args) {
        LocalDateTime currentTime;
        LocalDateTime targetTime;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LuckyMoneyEventProcessor.getCountDown("", 14, 15);
        LocalDateTime dateTime = LocalDateTime.now().plusHours(7L);
        if (dateTime.getHour() < 14) {
            targetTime = LocalDateTime.parse(new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + " 14:00:00", formatter);
            System.out.println("targetTime: " + formatter.format(targetTime));
            currentTime = LocalDateTime.now().plusSeconds(1L);
            System.out.println("currentTime: " + formatter.format(currentTime));
            System.out.println("rs: " + (targetTime.toEpochSecond(ZoneOffset.UTC) - currentTime.toEpochSecond(ZoneOffset.UTC)));
        }
        if (dateTime.getHour() >= 15 && dateTime.getSecond() > 0) {
            targetTime = LocalDateTime.parse(DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusDays(1L)) + " 14:00:00", formatter);
            System.out.println("targetTime: " + formatter.format(targetTime));
            currentTime = LocalDateTime.now().plusSeconds(1L);
            System.out.println("currentTime: " + formatter.format(currentTime));
            System.out.println("rs: " + (targetTime.toEpochSecond(ZoneOffset.UTC) - dateTime.toEpochSecond(ZoneOffset.UTC)));
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        try {
            long diff = Math.abs(TimeUnit.DAYS.convert(format.parse(format.format(new Date())).getTime() - format.parse("2021-10-30 00:00:00").getTime(), TimeUnit.MILLISECONDS));
            System.out.println("offset: " + diff);
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
    }
}

