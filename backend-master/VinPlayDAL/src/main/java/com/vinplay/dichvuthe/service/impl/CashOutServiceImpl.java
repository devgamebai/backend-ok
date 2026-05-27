/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.enums.ProviderType
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  org.apache.log4j.Logger
 */
package com.vinplay.dichvuthe.service.impl;

import com.hazelcast.core.IMap;
import com.vinplay.dichvuthe.service.CashOutService;
import com.vinplay.dichvuthe.service.impl.AlertServiceImpl;
import com.vinplay.vbee.common.enums.ProviderType;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import java.util.ArrayList;
import org.apache.log4j.Logger;

public class CashOutServiceImpl
implements CashOutService {
    private static final Logger logger = Logger.getLogger((String)"cashout");

    private int mapErrorCodeEpay(int status) {
        switch (status) {
            case 0: {
                return 0;
            }
            case 23: 
            case 99: {
                return 30;
            }
            case 30: {
                return 999;
            }
            case 10: 
            case 11: 
            case 12: 
            case 13: 
            case 14: 
            case 15: 
            case 17: 
            case 21: 
            case 22: 
            case 31: 
            case 32: 
            case 33: 
            case 35: 
            case 52: 
            case 101: 
            case 102: 
            case 103: 
            case 104: 
            case 105: 
            case 106: 
            case 107: 
            case 108: 
            case 109: 
            case 110: 
            case 111: 
            case 112: 
            case 113: {
                return 1;
            }
        }
        return 30;
    }

    private int mapErrorCodeVTC(int status) {
        switch (status) {
            case 1: {
                return 0;
            }
            case -55: {
                return 999;
            }
            case -600: 
            case -509: 
            case -503: 
            case -502: 
            case -501: 
            case -500: 
            case -350: 
            case -348: 
            case -320: 
            case -318: 
            case -317: 
            case -316: 
            case -315: 
            case -311: 
            case -310: 
            case -309: 
            case -308: 
            case -307: 
            case -306: 
            case -305: 
            case -304: 
            case -302: {
                return 1;
            }
            case -505: 
            case -504: 
            case -290: 
            case -99: 
            case -1: 
            case 0: {
                return 30;
            }
        }
        return 30;
    }

    private int mapErrorCodeKhoThe(int status) {
        switch (status) {
            case 0: {
                return 0;
            }
            case 5: {
                return 999;
            }
            case -600: 
            case -509: 
            case -503: 
            case -502: 
            case -501: 
            case -500: 
            case -350: 
            case -348: 
            case -320: 
            case -318: 
            case -317: 
            case -316: 
            case -315: 
            case -311: 
            case -310: 
            case -309: 
            case -308: 
            case -307: 
            case -306: 
            case -305: 
            case -304: 
            case -302: {
                return 1;
            }
        }
        return 30;
    }

    private int mapErrorCode1Pay(int status) {
        switch (status) {
            case 0: {
                return 0;
            }
            case 800: {
                return 999;
            }
            case 1: 
            case 2: 
            case 901: 
            case 903: 
            case 1001: 
            case 2000: 
            case 2001: 
            case 2002: 
            case 2003: 
            case 2005: 
            case 9999: {
                return 1;
            }
        }
        return 30;
    }

    private String mapMessage1Pay(int status) {
        switch (status) {
            case 0: {
                return "Th\u00e0nh c\u00f4ng";
            }
            case 1: 
            case 9999: {
                return "Giao d\u1ecbch th\u1ea5t b\u1ea1i";
            }
            case 2: {
                return "Th\u00f4ng tin x\u00e1c th\u1ef1c kh\u00f4ng ch\u00ednh x\u00e1c";
            }
            case 800: {
                return "S\u1ed1 d\u01b0 kh\u00f4ng \u0111\u1ee7";
            }
            case 901: {
                return "Th\u00f4ng tin \u0111\u0103ng nh\u1eadp kh\u00f4ng \u0111\u00fang";
            }
            case 903: {
                return "Th\u00f4ng tin \u0111\u0103ng nh\u1eadp kh\u00f4ng \u0111\u00fang";
            }
            case 1001: {
                return "Nh\u00e0 m\u1ea1ng ng\u1eebng ho\u1ea1t \u0111\u1ed9ng ho\u1eb7c \u0111ang b\u1ea3o tr\u00ec";
            }
            case 2000: {
                return "Tham s\u1ed1 \u0111\u1ea7u v\u00e0o kh\u00f4ng \u0111\u00fang";
            }
            case 2001: {
                return "Tham s\u1ed1 \u0111\u1ea7u v\u00e0o Topup kh\u00f4ng \u0111\u00fang";
            }
            case 2002: {
                return "Tham s\u1ed1 \u0111\u1ea7u v\u00e0o Mua th\u1ebb kh\u00f4ng \u0111\u00fang";
            }
            case 2003: {
                return "S\u1ed1 \u0111i\u1ec7n tho\u1ea1i cung c\u1ea5p kh\u00f4ng \u0111\u00fang";
            }
            case 2005: {
                return "Giao d\u1ecbch b\u1ecb l\u1eb7p";
            }
        }
        return "\u0110ang x\u1eed l\u00fd";
    }

    private int getErrorCodeCard(int status) {
        switch (status) {
            case 0: {
                return 0;
            }
            case 10: {
                return 1;
            }
            case 20: {
                return 22;
            }
            case 99: {
                return 30;
            }
        }
        return 30;
    }

    private int getErrorCodeTopUp(int status) {
        switch (status) {
            case 0: {
                return 0;
            }
            case 10: {
                return 1;
            }
            case 11: {
                return 23;
            }
            case 99: {
                return 30;
            }
        }
        return 30;
    }

    private int getErrorCodeBank(int status) {
        switch (status) {
            case 0: {
                return 0;
            }
            case 10: {
                return 30;
            }
            case 30: {
                return 31;
            }
            case 31: {
                return 32;
            }
            case 32: {
                return 33;
            }
            case 98: {
                return 34;
            }
            case 99: {
                return 34;
            }
        }
        return 34;
    }

    private String getDescErrorCodeBank(int status) {
        switch (status) {
            case 0: {
                return "Thanh cong.";
            }
            case 10: {
                return "That bai.";
            }
            case 30: {
                return "Sai ngan hang.";
            }
            case 31: {
                return "Sai ten tai khoan.";
            }
            case 32: {
                return "Sai so tai khoan.";
            }
            case 98: {
                return "Dang xu ly.";
            }
            case 99: {
                return "Dang xu ly.";
            }
        }
        return "Dang xu ly.";
    }

    private String getServiceName(ProviderType provider) {
        if (provider.getType() == 0) {
            return "Mua th\u1ebb \u0111i\u1ec7n tho\u1ea1i";
        }
        return "Mua th\u1ebb game";
    }

    private void alert(String number, String content, boolean isCall) {
        AlertServiceImpl alertService = new AlertServiceImpl();
        if (number.contains(",")) {
            String[] arr = number.split(",");
            ArrayList<String> mList = new ArrayList<String>();
            for (String m : arr) {
                m = m.trim();
                mList.add(m);
            }
            alertService.alert2List(mList, content, isCall);
        } else {
            alertService.alert2One(number, content, isCall);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void refundWhenError(IMap<String, UserModel> userMap, String nickname, int moneyCashout, long moneyCashoutReal, long money) {
        try {
            userMap.lock(nickname);
            UserCacheModel user = (UserCacheModel)userMap.get(nickname);
            long moneyUser = user.getVin();
            long currentMoney = user.getVinTotal();
            user.setVin(moneyUser += money);
            user.setVinTotal(currentMoney += money);
            user.setCashout(moneyCashout -= (int)moneyCashoutReal);
            userMap.put(nickname, user);
        }
        catch (Exception exception) {
        }
        finally {
            userMap.unlock(nickname);
        }
    }
}

