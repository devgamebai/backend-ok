/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.UserAdminInfo
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  org.apache.log4j.Logger
 */
package com.vinplay.usercore.service.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.usercore.dao.impl.UserDaoImpl;
import com.vinplay.usercore.service.UserForAdminService;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.UserAdminInfo;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import java.sql.SQLException;
import java.util.List;
import org.apache.log4j.Logger;

public class UserForAdminServiceImpl
implements UserForAdminService {
    private static final Logger logger = Logger.getLogger((String)"user_core");

    @Override
    public UserModel getUserNormalByNickName(String nickName) throws SQLException {
        UserDaoImpl userDao = new UserDaoImpl();
        return userDao.getUserNormalByNickName(nickName);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public boolean updateStatusDailyByNickName(String nickname, int status) throws SQLException {
        boolean res = false;
        UserDaoImpl userDao = new UserDaoImpl();
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey(nickname)) {
            try {
                 userMap.lock(nickname);
                UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                if (!userDao.updateStatusDailyByNickName(nickname, status)) return res;
                user.setDaily(status);
                userMap.put(nickname, user);
                res = true;
                return res;
            }
            catch (Exception e) {
                logger.debug(e);
                return res;
            }
            finally {
                 userMap.unlock(nickname);
            }
        } else {
            if (!userDao.updateStatusDailyByNickName(nickname, status)) return res;
            return true;
        }
    }

    @Override
    public List<UserAdminInfo> searchUserAdmin(String userName, String nickName, String phone, String field, String sort, String daily, String timeStart, String timeEnd, int page, int totalrecord, String bot, String like, String email) throws SQLException {
        UserDaoImpl userDao = new UserDaoImpl();
        return userDao.searchUserAdmin(userName, nickName, phone, field, sort, daily, timeStart, timeEnd, page, totalrecord, bot, like, email);
    }

    @Override
    public int countSearchUserAdmin(String userName, String nickName, String phone, String field, String sort, String daily, String timeStart, String timeEnd, String bot) throws SQLException {
        UserDaoImpl userDao = new UserDaoImpl();
        return userDao.countSearchUserAdmin(userName, nickName, phone, field, sort, daily, timeStart, timeEnd, bot);
    }
}

