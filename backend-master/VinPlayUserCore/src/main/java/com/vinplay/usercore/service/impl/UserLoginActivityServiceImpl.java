package com.vinplay.usercore.service.impl;

import com.vinplay.usercore.dao.UserLoginActivityDao;
import com.vinplay.usercore.dao.impl.UserLoginActivityDaoImpl;
import com.vinplay.usercore.service.UserLoginActivityService;
import com.vinplay.vbee.common.response.UserLoginLogModel;

import java.util.List;

public class UserLoginActivityServiceImpl implements UserLoginActivityService {

    private UserLoginActivityDao dao = new UserLoginActivityDaoImpl();

    @Override
    public List<UserLoginLogModel> searchLoginLogs(String username, String dateFrom, String dateTo, int page, int limit) {
        return dao.searchLoginLogs(username, dateFrom, dateTo, page, limit);
    }

    @Override
    public long countLoginLogs(String username, String dateFrom, String dateTo) {
        return dao.countLoginLogs(username, dateFrom, dateTo);
    }
}
