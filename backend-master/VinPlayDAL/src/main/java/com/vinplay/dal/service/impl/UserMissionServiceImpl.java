/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.userMission.LogReceivedRewardObj
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.UserDaoImpl;
import com.vinplay.dal.service.UserMissionService;
import com.vinplay.vbee.common.response.userMission.LogReceivedRewardObj;
import java.sql.SQLException;
import java.util.List;

public class UserMissionServiceImpl
implements UserMissionService {
    @Override
    public List<LogReceivedRewardObj> getLogReceivedReward(String nickName, String gameName, String moneyType, String timeStart, String timeEnd, int page) throws SQLException {
        UserDaoImpl dao = new UserDaoImpl();
        return dao.getLogReceivedReward(nickName, gameName, moneyType, timeStart, timeEnd, page);
    }

    @Override
    public boolean updateUserMission(String nickName, String missionName, String moneyType, int matchWin) throws SQLException {
        UserDaoImpl dao = new UserDaoImpl();
        return dao.updateUserMission(nickName, missionName, moneyType, matchWin);
    }
}

