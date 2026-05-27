/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.log4j.Logger
 */
package com.vinplay.liveUser.service.impl;

import com.vinplay.liveUser.dao.LiveUserDepositDAO;
import com.vinplay.liveUser.dao.LiveUserGameDAO;
import com.vinplay.liveUser.dao.impl.LiveUserDepositDAOImpl;
import com.vinplay.liveUser.dao.impl.LiveUserGameDAOImpl;
import com.vinplay.liveUser.entities.LiveUserDepositEntity;
import com.vinplay.liveUser.entities.LiveUserGameEntity;
import com.vinplay.liveUser.service.LiveUserDepositService;
import java.util.Calendar;
import java.util.Random;
import org.apache.log4j.Logger;

public class LiveUserDepositServiceImpl
implements LiveUserDepositService {
    LiveUserGameDAO liveUserGameDAO = new LiveUserGameDAOImpl();
    private static final Logger logger = Logger.getLogger((String)"backend");
    LiveUserDepositDAO liveUserDepositDAO = new LiveUserDepositDAOImpl();
    private Random rand = new Random();

    @Override
    public boolean checkAndCreateDeposit(String nickname, int money, String actionName, int fid, String type, String msg) {
        try {
            LiveUserGameEntity userInfo = this.liveUserGameDAO.getByNickname(nickname);
            if (userInfo == null) {
                return false;
            }
            if (!userInfo.getActive().booleanValue()) {
                return false;
            }
            LiveUserDepositEntity deposit = new LiveUserDepositEntity();
            deposit.setNick_name(nickname);
            deposit.setCash(money);
            deposit.setAction_name(actionName);
            deposit.setFid(String.valueOf(fid));
            deposit.setType(type);
            deposit.setMsgSuccess(msg);
            deposit.setRun(false);
            int nextTime = this.rand.nextInt(20);
            Calendar time = Calendar.getInstance();
            time.add(13, 20 + nextTime);
            deposit.setDeposit_at(time.getTime());
            boolean ok = this.liveUserDepositDAO.create(deposit);
            return ok;
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
            return false;
        }
    }
}

