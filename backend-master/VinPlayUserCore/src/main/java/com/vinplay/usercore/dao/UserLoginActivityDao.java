package com.vinplay.usercore.dao;

import com.vinplay.vbee.common.response.UserLoginLogModel;
import java.util.List;

public interface UserLoginActivityDao {
    public List<UserLoginLogModel> searchLoginLogs(String username, String dateFrom, String dateTo, int page, int limit);
    public long countLoginLogs(String username, String dateFrom, String dateTo);
}
