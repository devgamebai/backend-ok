/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.UserNewMobileResponse
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.UserNewMobileDAOImpl;
import com.vinplay.dal.service.UserNewMobileService;
import com.vinplay.vbee.common.response.UserNewMobileResponse;
import java.util.List;

public class UserNewMobileServiceImpl
implements UserNewMobileService {
    @Override
    public List<UserNewMobileResponse> searchUserNewMobile(String nickName, String mobile, String mobileold, int page) {
        UserNewMobileDAOImpl dao = new UserNewMobileDAOImpl();
        return dao.searchUserNewMobile(nickName, mobile, mobileold, page);
    }

    @Override
    public long countSearchUserNewMobile(String nickName, String mobile, String mobileold) {
        UserNewMobileDAOImpl dao = new UserNewMobileDAOImpl();
        return dao.countSearchUserNewMobile(nickName, mobile, mobileold);
    }
}

