/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.GetUserInfoResponse
 *  com.vinplay.vbee.common.response.UserInfoResponse
 */
package com.vinplay.usercore.service.impl;

import com.vinplay.usercore.dao.impl.UserInfoDaoImpl;
import com.vinplay.usercore.service.UserInfoService;
import com.vinplay.vbee.common.response.GetUserInfoResponse;
import com.vinplay.vbee.common.response.UserInfoResponse;
import java.sql.SQLException;
import java.util.List;

public class UserInfoServiceImpl
implements UserInfoService {
    @Override
    public List<UserInfoResponse> searchUserInfo(String nickName, String ip, String startDate, String endDate, String type, int page) {
        UserInfoDaoImpl dao = new UserInfoDaoImpl();
        return dao.searchUserInfo(nickName, ip, startDate, endDate, type, page);
    }

    @Override
    public int countsearchUserInfo(String nickName, String ip, String startDate, String endDate, String type) {
        UserInfoDaoImpl dao = new UserInfoDaoImpl();
        return dao.countsearchUserInfo(nickName, ip, startDate, endDate, type);
    }

    @Override
    public GetUserInfoResponse listGetNickName(String nickName) throws SQLException {
        UserInfoDaoImpl dao = new UserInfoDaoImpl();
        return dao.listGetNickName(nickName);
    }
}

