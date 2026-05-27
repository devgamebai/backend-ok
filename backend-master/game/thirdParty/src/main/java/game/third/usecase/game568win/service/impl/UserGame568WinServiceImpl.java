/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  org.apache.log4j.Logger
 */
package game.third.usecase.game568win.service.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import game.third.usecase.config.Game568winConfig;
import game.third.usecase.config.ThirdPartyLoad;
import game.third.usecase.game568win.dao.UserGame568WinDao;
import game.third.usecase.game568win.dao.impl.UserGame568WinDaoImpl;
import game.third.usecase.game568win.entities.UserGame568Win;
import game.third.usecase.game568win.request.RegisterPlayer;
import game.third.usecase.game568win.response.RegisterPlayerResult;
import game.third.usecase.game568win.service.UserGame568WinService;
import game.third.usecase.game568win.service.impl.APIGame568winServiceImpl;
import org.apache.log4j.Logger;

public class UserGame568WinServiceImpl
implements UserGame568WinService {
    private static final Logger logger = Logger.getLogger((String)"service");
    UserGame568WinDao userGame568WinDao = new UserGame568WinDaoImpl();
    APIGame568winServiceImpl apiGame568winService = new APIGame568winServiceImpl();

    @Override
    public UserGame568Win checkAndCreateUser(String username) throws Exception {
        UserGame568Win user;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap userMap = client.getMap("game568win_users");
        if (userMap.containsKey((Object)username)) {
            return (UserGame568Win)userMap.get((Object)username);
        }
        Game568winConfig winConfig = ThirdPartyLoad.getGame568winConfig();
        boolean haveUser = this.userGame568WinDao.checkUserExist(username);
        if (!haveUser) {
            RegisterPlayer request = new RegisterPlayer();
            request.setAgent(winConfig.getAgent());
            request.setDisplayName(username);
            request.setUsername(username);
            request.setServerId(winConfig.getServerId());
            request.setUserGroup(winConfig.getUserGroup());
            RegisterPlayerResult registerPlayerResult = this.apiGame568winService.RegisterPlayer(request);
            if (registerPlayerResult == null) {
                return null;
            }
            if (registerPlayerResult.getError().getId() == 0) {
                UserGame568Win user2 = new UserGame568Win();
                user2.setUserGroup(winConfig.getUserGroup());
                user2.setAgent(winConfig.getAgent());
                user2.setDisplayName(username);
                user2.setUsername(username);
                user2.setServerId(winConfig.getServerId());
                boolean success = this.userGame568WinDao.create(user2);
                if (!success) {
                    logger.error((Object)("Cannot create user " + username));
                }
            } else {
                return null;
            }
        }
        if ((user = this.userGame568WinDao.get(username)) != null) {
            userMap.set((Object)username, (Object)user);
        }
        return user;
    }
}

