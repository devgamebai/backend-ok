/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.log4j.Logger
 */
package com.vinplay.usercore.service.impl;

import com.vinplay.usercore.dao.UserWagesDao;
import com.vinplay.usercore.dao.impl.UserWagesDaoImpl;
import com.vinplay.usercore.entities.UserWages;
import com.vinplay.usercore.service.UserWagesService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;

public class UserWagesServiceImpl
implements UserWagesService {
    private static final Logger logger = Logger.getLogger(UserWagesServiceImpl.class);
    private UserWagesDao dao = new UserWagesDaoImpl();

    @Override
    public boolean insertByJob(String date) {
        try {
            return this.dao.insertByJob(date);
        }
        catch (Exception e) {
            logger.error(("Error create user_wages: " + e.getMessage()));
            return false;
        }
    }

    @Override
    public String receivedMoney(long id) {
        try {
            UserWages userWages = this.dao.getById(id);
            if (userWages == null) {
                return "D\u1eef li\u1ec7u kh\u00f4ng t\u1ed3n t\u1ea1i";
            }
            if (userWages.getStatus() > 0) {
                return "B\u1ea1n \u0111\u00e3 nh\u1eadn ti\u1ec1n ng\u00e0y n\u00e0y r\u1ed3i";
            }
            String result = "";
            result = this.dao.updateStatus(id, 1);
            if (!"success".equalsIgnoreCase(result)) {
                return "C\u1eadp nh\u1eadt tr\u1ea1ng th\u00e1i kh\u00f4ng th\u00e0nh c\u00f4ng";
            }
            UserServiceImpl userService = new UserServiceImpl();
            MoneyResponse moneyResponse = userService.updateMoney(userWages.getNick_name(), userWages.getBonus(), "vin", Games.USER_WAGES.getName(), Games.USER_WAGES.getId() + "", "USER_WAGES", 0L, null, TransType.NO_VIPPOINT);
            if (moneyResponse.getErrorCode().equals("0")) {
                return "success";
            }
            userWages = this.dao.getById(id);
            this.dao.updateStatus(id, 0);
            return "C\u1ed9ng ti\u1ec1n th\u01b0\u1edfng kh\u00f4ng th\u00e0nh c\u00f4ng. Vui l\u00f2ng li\u00ean h\u1ec7 b\u1ed9 ph\u1eadn ch\u0103m s\u00f3c kh\u00e1ch h\u00e0ng \u0111\u1ec3 \u0111\u01b0\u1ee3c gi\u00fap \u0111\u1ee1";
        }
        catch (Exception e) {
            logger.error(("Error updateStatus user_wages: " + e.getMessage()));
            return e.getMessage();
        }
    }

    @Override
    public String receivedAllMoney(String nickname) {
        try {
            long totalBonusNotReceived = this.dao.getSumBonusByStatus(nickname, 0);
            UserServiceImpl userService = new UserServiceImpl();
            MoneyResponse moneyResponse = userService.updateMoney(nickname, totalBonusNotReceived, "vin", Games.USER_WAGES.getName(), Games.USER_WAGES.getId() + "", "USER_WAGES", 0L, null, TransType.NO_VIPPOINT);
            if (moneyResponse.getErrorCode().equals("0")) {
                String result = "";
                result = this.dao.updateAllStatusToReceivedBonus(nickname);
                if (!"success".equalsIgnoreCase(result)) {
                    moneyResponse = userService.updateMoney(nickname, totalBonusNotReceived * -1L, "vin", Games.USER_WAGES.getName(), Games.USER_WAGES.getId() + "", "USER_WAGES : rollback because update all status fail", 0L, null, TransType.NO_VIPPOINT);
                    return "C\u1ed9ng ti\u1ec1n th\u01b0\u1edfng kh\u00f4ng th\u00e0nh c\u00f4ng. Vui l\u00f2ng li\u00ean h\u1ec7 b\u1ed9 ph\u1eadn ch\u0103m s\u00f3c kh\u00e1ch h\u00e0ng \u0111\u1ec3 \u0111\u01b0\u1ee3c gi\u00fap \u0111\u1ee1";
                }
                return "success";
            }
            return "C\u1ed9ng ti\u1ec1n th\u01b0\u1edfng kh\u00f4ng th\u00e0nh c\u00f4ng. Vui l\u00f2ng li\u00ean h\u1ec7 b\u1ed9 ph\u1eadn ch\u0103m s\u00f3c kh\u00e1ch h\u00e0ng \u0111\u1ec3 \u0111\u01b0\u1ee3c gi\u00fap \u0111\u1ee1";
        }
        catch (Exception e) {
            logger.error(("Error updateStatus user_wages: " + e.getMessage()));
            return e.getMessage();
        }
    }

    @Override
    public Map<String, Object> history(String nickname, String statDate, String endDate, int status, int pageIndex, int limit) {
        try {
            return this.dao.history(nickname, statDate, endDate, status, pageIndex, limit);
        }
        catch (Exception e) {
            logger.error(("Error history user_wages: " + e.getMessage()));
            return new HashMap<String, Object>();
        }
    }
}

