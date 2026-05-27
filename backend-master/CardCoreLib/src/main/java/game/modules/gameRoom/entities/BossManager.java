/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.engine.sessions.ISession
 *  bitzero.server.entities.User
 *  bitzero.util.common.business.Debug
 *  com.google.gson.Gson
 *  com.vinplay.dal.service.BotService
 *  com.vinplay.dal.service.impl.BotServiceImpl
 *  com.vinplay.gamebai.entities.XocDiaBoss
 *  com.vinplay.usercore.service.MoneyInGameService
 *  com.vinplay.usercore.service.UserService
 *  com.vinplay.usercore.service.XocDiaService
 *  com.vinplay.usercore.service.impl.MoneyInGameServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.usercore.service.impl.XocDiaServiceImpl
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.models.FreezeModel
 *  com.vinplay.vbee.common.models.StatusUser
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.utils.UserUtil
 */
package game.modules.gameRoom.entities;

import bitzero.engine.sessions.ISession;
import bitzero.server.entities.User;
import bitzero.util.common.business.Debug;
import com.google.gson.Gson;
import com.vinplay.dal.service.BotService;
import com.vinplay.dal.service.impl.BotServiceImpl;
import com.vinplay.gamebai.entities.XocDiaBoss;
import com.vinplay.usercore.service.MoneyInGameService;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.XocDiaService;
import com.vinplay.usercore.service.impl.MoneyInGameServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.usercore.service.impl.XocDiaServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.models.FreezeModel;
import com.vinplay.vbee.common.models.StatusUser;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.utils.UserUtil;
import game.entities.PlayerInfo;
import game.modules.gameRoom.entities.GameMoneyInfo;
import game.modules.gameRoom.entities.GameRoom;
import game.modules.gameRoom.entities.GameRoomGroup;
import game.modules.gameRoom.entities.GameRoomManager;
import game.modules.gameRoom.entities.GameRoomSetting;
import game.utils.GameUtils;
import game.xocdia.conf.XocDiaGameUtils;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BossManager {
    private final Gson gson = new Gson();
    private UserService userSer = new UserServiceImpl();
    private BotService botSer = new BotServiceImpl();
    private XocDiaService xdSer = new XocDiaServiceImpl();
    private MoneyInGameService mnSer = new MoneyInGameServiceImpl();
    private static BossManager bossMgr = null;
    private Map<Integer, XocDiaBoss> bossMap = new HashMap<Integer, XocDiaBoss>();

    public static BossManager instance() {
        if (bossMgr == null) {
            bossMgr = new BossManager();
        }
        return bossMgr;
    }

    private BossManager() {
        this.init();
    }

    private void init() {
        try {
            this.bossMap = XocDiaGameUtils.isXocDia() ? this.xdSer.getListRoomBossActive() : new HashMap<Integer, XocDiaBoss>();
            Debug.trace((Object)("Init boss success, size: " + this.bossMap.size()));
        }
        catch (SQLException e) {
            Debug.trace((Object)("Init boss error: " + e.getMessage()));
            Debug.trace((Object)e);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void initialBoss() {
        Map<Integer, XocDiaBoss> map = this.bossMap;
        synchronized (map) {
            for (Map.Entry<Integer, XocDiaBoss> entry : this.bossMap.entrySet()) {
                XocDiaBoss boss = entry.getValue();
                try {
                    UserCacheModel userModel = this.userSer.getUser(boss.getNickname());
                    if (userModel == null) {
                        userModel = UserUtil.getUserCacheModel((UserModel)this.botSer.login(boss.getNickname()));
                    }
                    if (userModel == null || userModel.isBanLogin() || userModel.getDaily() != 0 || StatusUser.checkStatus((int)userModel.getStatus(), (int)Games.findGameByName((String)GameUtils.gameName).getId())) continue;
                    User user = new User(null);
                    user.setName(boss.getNickname());
                    user.setConnected(true);
                    PlayerInfo.getInfo(user);
                    GameRoomSetting setting = (GameRoomSetting)this.gson.fromJson(boss.getRoomSetting(), GameRoomSetting.class);
                    GameMoneyInfo moneyInfo = new GameMoneyInfo(user, setting);
                    FreezeModel freeze = this.mnSer.getFreeze(boss.getSessionId());
                    if (freeze == null) continue;
                    this.mnSer.pushFreezeToCache(freeze);
                    moneyInfo.sessionId = boss.getSessionId();
                    moneyInfo.freezeMoney = freeze.getMoney();
                    user.setProperty((Object)"GAME_MONEY_INFO", (Object)moneyInfo);
                    GameRoomGroup group = GameRoomManager.instance().getGroup(setting);
                    GameRoom room = group.createRoom(setting, boss.getRoomId());
                    GameRoomManager.instance().joinRoom(user, room, false);
                    boss.setSystemLogin(true);
                    this.bossMap.put(entry.getKey(), boss);
                    Debug.trace((Object)("InitialBoss " + boss.getNickname() + " success"));
                }
                catch (Exception e) {
                    Debug.trace((Object)("InitialBoss " + boss.getNickname() + " error: " + e.getMessage()));
                }
            }
            Debug.trace((Object)"Initial boss finish");
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void putBoss(XocDiaBoss boss) throws SQLException {
        try {
            Map<Integer, XocDiaBoss> map = this.bossMap;
            synchronized (map) {
                this.xdSer.saveRoomBoss(boss);
                this.bossMap.put(boss.getRoomId(), boss);
                Debug.trace((Object)("CREATE BOSS roomId: " + boss.getRoomId() + ", sessionId: " + boss.getSessionId() + ", nickname: " + boss.getNickname() + ", setting: " + boss.getRoomSetting() + ", fundInitial: " + boss.getFundInitial()));
            }
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void removeBoss(int roomId) {
        try {
            Map<Integer, XocDiaBoss> map = this.bossMap;
            synchronized (map) {
                if (this.bossMap.containsKey(roomId)) {
                    this.bossMap.remove(roomId);
                }
            }
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public XocDiaBoss getBoss(int roomId) {
        Map<Integer, XocDiaBoss> map = this.bossMap;
        synchronized (map) {
            if (this.bossMap.containsKey(roomId)) {
                return this.bossMap.get(roomId);
            }
            return null;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean checkRoomId(int roomId) {
        Map<Integer, XocDiaBoss> map = this.bossMap;
        synchronized (map) {
            return this.bossMap.containsKey(roomId);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean checkBossNameAndId(String nickname, int roomId) {
        Map<Integer, XocDiaBoss> map = this.bossMap;
        synchronized (map) {
            for (Map.Entry<Integer, XocDiaBoss> entry : this.bossMap.entrySet()) {
                if (!entry.getValue().getNickname().equals(nickname) || entry.getValue().getRoomId() != roomId) continue;
                return true;
            }
            return false;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean checkBossName(String nickname) {
        Map<Integer, XocDiaBoss> map = this.bossMap;
        synchronized (map) {
            for (Map.Entry<Integer, XocDiaBoss> entry : this.bossMap.entrySet()) {
                if (!entry.getValue().getNickname().equals(nickname)) continue;
                return true;
            }
            return false;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public int getRoomIdByBossName(String nickname) {
        Map<Integer, XocDiaBoss> map = this.bossMap;
        synchronized (map) {
            for (Map.Entry<Integer, XocDiaBoss> entry : this.bossMap.entrySet()) {
                if (!entry.getValue().getNickname().equals(nickname)) continue;
                return entry.getKey();
            }
            return -1;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean checkBossSysLogin(int roomId) {
        Map<Integer, XocDiaBoss> map = this.bossMap;
        synchronized (map) {
            if (this.bossMap.containsKey(roomId)) {
                return this.bossMap.get(roomId).isSystemLogin();
            }
            return false;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public int getNumBoard(String nickname) {
        Map<Integer, XocDiaBoss> map = this.bossMap;
        synchronized (map) {
            int num = 0;
            for (Map.Entry<Integer, XocDiaBoss> entry : this.bossMap.entrySet()) {
                if (!entry.getValue().getNickname().equals(nickname)) continue;
                ++num;
            }
            return num;
        }
    }
}

