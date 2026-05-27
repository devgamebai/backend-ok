/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.engine.sessions.ISession
 *  bitzero.server.BitZeroServer
 *  bitzero.server.core.BZEventType
 *  bitzero.server.core.IBZEventType
 *  bitzero.server.entities.User
 *  bitzero.server.entities.data.ISFSObject
 *  bitzero.server.entities.managers.IUserManager
 *  bitzero.server.exceptions.BZException
 *  bitzero.server.extensions.BZExtension
 *  bitzero.server.extensions.data.DataCmd
 *  bitzero.server.util.TaskScheduler
 *  bitzero.util.ExtensionUtility
 *  bitzero.util.common.business.Debug
 *  bitzero.util.socialcontroller.bean.UserInfo
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.dal.service.ServerInfoService
 *  com.vinplay.dal.service.impl.ServerInfoServiceImpl
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.enums.Platform
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.hazelcast.HazelcastLoader
 *  com.vinplay.vbee.common.models.cache.UserExtraInfoModel
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 */
package game;

import bitzero.engine.sessions.ISession;
import bitzero.server.BitZeroServer;
import bitzero.server.core.BZEventType;
import bitzero.server.core.IBZEventType;
import bitzero.server.entities.User;
import bitzero.server.entities.data.ISFSObject;
import bitzero.server.entities.managers.IUserManager;
import bitzero.server.exceptions.BZException;
import bitzero.server.extensions.BZExtension;
import bitzero.server.extensions.data.DataCmd;
import bitzero.server.util.TaskScheduler;
import bitzero.util.ExtensionUtility;
import bitzero.util.common.business.Debug;
import bitzero.util.socialcontroller.bean.UserInfo;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.service.ServerInfoService;
import com.vinplay.dal.service.impl.ServerInfoServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.enums.Platform;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.hazelcast.HazelcastLoader;
import com.vinplay.vbee.common.models.cache.UserExtraInfoModel;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.rmq.RMQApi;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import game.eventHandlers.LoginSuccessHandler;
import game.eventHandlers.UserDisconnectHandler;
import game.modules.gameRoom.GameRoomModule;
import game.modules.minigame.*;
import game.modules.minigame.cmd.MiniGameCMD;
import game.modules.minigame.entities.BotMinigame;
import game.modules.player.PlayerModule;
import game.modules.player.cmd.rev.LoginCmd;
import game.utils.ConfigGame;
import game.utils.GameUtils;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class BaseGameExtension
extends BZExtension {
    private int countReloadConfig = 0;
    private int countLogCCU = 0;
    private final Runnable gameLoopTask = new GameLoopTask();
    private ServerInfoService serverInfoSrv = new ServerInfoServiceImpl();
    private int lastCCU = 0;
    public static int ccuWeb = 0;
    public static int ccuAD = 0;
    public static int ccuIOS = 0;
    public static int ccuWP = 0;
    public static int ccuFB = 0;
    public static int ccuDT = 0;

    public void init() {
        try {
            RMQApi.start((String)"config/rmq.properties");
            // Start balance update consumer for real-time lobby balance push (SUN-690)
            try { new game.queue.BalanceUpdateConsumer().start(); } catch (Exception e) {
                Debug.trace("BalanceUpdateConsumer start failed: " + e.getMessage());
            }
            // SUN-767: start force-logout consumer so new logins can kick the
            // previous bitzero session for the same nickname.
            try { new game.queue.ForceLogoutConsumer().start(); } catch (Exception e) {
                Debug.trace("ForceLogoutConsumer start failed: " + e.getMessage());
            }
            // SUN-RS (C-minigame-bootstrap): Redis Streams consumer runtime.
            // The action queues consumed inside this Minigame container
            // (queue_action_minigame, queue_action_portal) are walked from
            // Minigame's own config/rabbitmq_config.xml — distinct from vbee's
            // XML, since the consumer-side queue lists do not overlap. With
            // every per-queue MESSAGE_BUS_<QUEUE> flag at the default "rmq",
            // start() logs "0 queues enabled" and exits cleanly. Wrapped so a
            // Redis-side failure cannot take down the BalanceUpdate / ForceLogout
            // RMQ paths above.
            try {
                com.vinplay.vbee.common.messagebus.redis.RedisStreamRuntime.start("config/rabbitmq_config.xml");
            } catch (Throwable t) {
                Debug.trace("RedisStreamRuntime.start failed (non-fatal, RMQ path unaffected): " + t.getMessage());
            }
            HazelcastLoader.start();
            // SUN-816: register Hazelcast listener on cacheToken. This
            // fires on every game server when a token is removed
            // (= new-device login), kicking the stale bitzero session
            // regardless of which game the user is in. Superset of the
            // SUN-767 RMQ path — kept alongside for one release as belt
            // and suspenders.
            try {
                com.vinplay.vbee.common.session.SessionKickListener.register(new com.vinplay.vbee.common.session.SessionKickListener.KickHandler() {
                    @Override
                    public void kick(String accessToken, String nickname, String reason) {
                        if (nickname == null || nickname.isEmpty()) return;
                        try {
                            java.util.List<bitzero.server.entities.User> users =
                                    bitzero.util.ExtensionUtility.getExtension().getApi().getUserByName(nickname);
                            if (users == null || users.isEmpty()) return;
                            game.modules.lobby.cmd.send.ForceLogoutMsg wire =
                                    new game.modules.lobby.cmd.send.ForceLogoutMsg();
                            wire.reason = reason != null ? reason : "DUPLICATE_LOGIN";
                            wire.newLoginTime = System.currentTimeMillis();
                            for (bitzero.server.entities.User u : users) {
                                try { bitzero.util.ExtensionUtility.getExtension().send(wire, u); } catch (Exception ignored) {}
                                // SUN-1049: defer the disconnect by 500ms so the
                                // BitZero writer thread has time to flush cmd 20200
                                // to the client BEFORE the socket is torn down.
                                // Without this delay, send() queues the packet and
                                // disconnectUser() closes the channel instantly —
                                // the kernel discards the unwritten buffer and the
                                // FE never receives FORCE_LOGOUT, so no popup fires
                                // and the user appears "still logged in" on the old
                                // device. Mirrors the BZBannedUserManager pattern
                                // which already schedules a delayed disconnect.
                                final bitzero.server.entities.User uRef = u;
                                try {
                                    bitzero.server.BitZeroServer.getInstance().getTaskScheduler().schedule(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                bitzero.server.BitZeroServer.getInstance().getAPIManager().getBzApi().disconnectUser(uRef);
                                            } catch (Exception ignored) {}
                                        }
                                    }, 500, java.util.concurrent.TimeUnit.MILLISECONDS);
                                } catch (Exception ignored) {
                                    // Scheduler unavailable — fall back to immediate disconnect.
                                    try {
                                        bitzero.server.BitZeroServer.getInstance().getAPIManager().getBzApi().disconnectUser(u);
                                    } catch (Exception ignored2) {}
                                }
                            }
                            Debug.trace("SessionKickListener: kicked " + users.size()
                                    + " Minigame session(s) for " + nickname + " reason=" + reason);
                        } catch (Throwable t) {
                            Debug.trace("SessionKickListener Minigame kick err nick=" + nickname
                                    + " err=" + t.getMessage());
                        }
                    }
                });
            } catch (Exception e) {
                Debug.trace("SessionKickListener register failed: " + e.getMessage());
            }
            MongoDBConnectionFactory.init();
            ConnectionPool.start((String)"config/db_pool.properties");
            ConfigGame.reload();
            BotMinigame.loadData();
            GameCommon.init();
        }
        catch (Exception e) {
            Debug.trace(("INIT MINIGAME ERROR " + e.getMessage()));
        }
        this.addRequestHandler((short)1000, PlayerModule.class);
        System.out.println("[BaseGameExtension] gameName='" + GameUtils.gameName + "'");
        if (GameUtils.gameName != null && GameUtils.gameName.equalsIgnoreCase("XocDiaTuLinh")) {
            // Dedicated XocDia Tứ Linh server — only handler 8000, no TàiXỉu/Sicbo noise
            safeAddHandler((short)18000, "game.modules.chat.ChatModule");
            safeAddHandler((short)20000, "game.modules.lobby.LobbyModule");
            // XocDia Tứ Linh registered below (delayed scheduler)
        } else if (GameUtils.gameName.equalsIgnoreCase("Minigame")) {
            // Register each module with graceful skip on failure
            // Handler registrations matching production server
            safeAddHandler((short)2000, "game.modules.minigame.TaiXiuModule");
            safeAddHandler((short)22000, "game.modules.minigame.TaiXiuMD5Module"); // Required for Sicbo (28xxx→22xxx redirect)
            // safeAddHandler((short)23100, "game.modules.chat.ChatMD5Module"); // DISABLED — depends on MD5
            safeAddHandler((short)4000, "game.modules.minigame.MiniPokerModule");
            safeAddHandler((short)5000, "game.modules.minigame.BauCuaModule");
            safeAddHandler((short)6000, "game.modules.minigame.CaoThapModule");
            safeAddHandler((short)7000, "game.modules.minigame.CandyModule");
            safeAddHandler((short)8000, "game.modules.minigame.GalaxyModule");
            safeAddHandler((short)18000, "game.modules.chat.ChatModule");
            safeAddHandler((short)20000, "game.modules.lobby.LobbyModule");
            safeAddHandler((short)21000, "game.modules.mission.MissionModule");
            safeAddHandler((short)30000, "game.modules.minigame.LotteryModule");
            safeAddHandler((short)28000, "game.modules.minigame.SicboModule");
        } else {
            this.addRequestHandler((short)3000, GameRoomModule.class);

            // XocDia server also hosts Tứ Linh (handler 8000)
            // Tứ Linh registered below via delayed scheduler (needs Zone 0)
        }
        this.addEventHandler((IBZEventType)BZEventType.USER_LOGIN, LoginSuccessHandler.class);
        this.addEventHandler((IBZEventType)BZEventType.USER_DISCONNECT, UserDisconnectHandler.class);
        BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.gameLoopTask, 10, 1, TimeUnit.SECONDS);

        // Monitor all registered handlers' scheduled tasks for uncaught errors
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                System.err.println("[UNCAUGHT] Thread " + t.getName() + ": " + e.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace(System.err);
            }
        });

        // XocDia Tứ Linh — create Zone 0 first, then register handler
        final BaseGameExtension self = this;
        BitZeroServer.getInstance().getTaskScheduler().schedule(new Runnable() {
            @SuppressWarnings("unchecked")
            public void run() {
                try {
                    // Ensure Zone 0 exists (XocDia needs it for room creation)
                    bitzero.server.entities.managers.IZoneManager zm = BitZeroServer.getInstance().getZoneManager();
                    if (zm.getZoneById(0) == null) {
                        bitzero.server.config.ZoneSettings zs = new bitzero.server.config.ZoneSettings();
                        zs.name = "XocDiaZone";
                        zs.maxRooms = 100;
                        zs.maxUsers = 10000;
                        zm.createZone(zs);
                        Debug.trace("Created Zone 0 for XocDia Tu Linh");
                    }

                    Class clazz = Class.forName("game.modules.XocDia.GameXocDiaController");
                    java.lang.reflect.Method m = BZExtension.class.getDeclaredMethod("addRequestHandler", short.class, Class.class);
                    m.setAccessible(true);
                    m.invoke(self, (short)8000, clazz);
                    Debug.trace("Registered XocDia Tu Linh handler 8000");
                } catch (Throwable e) {
                    Debug.trace("SKIP XocDia Tu Linh 8000: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }, 5, TimeUnit.SECONDS);
    }

    public void doLogin(short s, ISession iSession, DataCmd dataCmd) throws BZException {
        User user;
        if (s != 1) {
            return;
        }
        LoginCmd cmd = new LoginCmd(dataCmd);
        UserInfo info = GameUtils.getUserInfo(cmd.nickname, cmd.sessionKey);
        if (info != null && (user = ExtensionUtility.instance().canLogin(info, "", iSession)) != null) {
            user.setProperty("dai_ly", info.getStatus());
            this.saveCCUPlatform(user);
        }
    }

    public void doLogin(ISession iSession, ISFSObject iSFSObject) throws Exception {
    }

    public void saveCCUPlatform(User user) {
        HazelcastInstance instance = HazelcastClientFactory.getInstance();
        IMap userExtraModel = instance.getMap("cache_user_extra_info");
        if (userExtraModel.containsKey(user.getName())) {
            UserExtraInfoModel model = (UserExtraInfoModel)userExtraModel.get(user.getName());
            if (model != null && model.getPlatfrom() != null) {
                user.setProperty("pf", model.getPlatfrom());
                Platform platform = Platform.find((String)model.getPlatfrom());
                switch (platform) {
                    case WEB: {
                        ++ccuWeb;
                        break;
                    }
                    case ANDROID: {
                        ++ccuAD;
                        break;
                    }
                    case IOS: {
                        ++ccuIOS;
                        break;
                    }
                    case WINPHONE: {
                        ++ccuWP;
                        break;
                    }
                    case FACEBOOK_APP: {
                        ++ccuFB;
                        break;
                    }
                    case DESKTOP: {
                        ++ccuDT;
                    }
                }
            }
        } else {
            Debug.trace(("Cannot find user's extra info " + user.getName()));
        }
    }

    private void gameLoop() {
        ++this.countReloadConfig;
        if (this.countReloadConfig == 300) {
            Debug.trace("reload config");
            ConfigGame.reload();
            this.countReloadConfig = 0;
        }
        ++this.countLogCCU;
        if (this.countLogCCU == ConfigGame.getIntValue("update_log_ccu")) {
            int ccu = ExtensionUtility.globalUserManager.getUserCount();//  .getUserCountByName();
            long ccuGiam = this.lastCCU - ccu;
            if (ccuGiam >= (long)ConfigGame.getIntValue("min_so_ccu_giam", 50)) {
                GameUtils.sendAlert("CCU giam " + ccuGiam + " trong " + ConfigGame.getIntValue("update_log_ccu") + " (s), time= " + DateTimeUtils.getCurrentTime());
            }
            this.lastCCU = ccu;
            this.countLogCCU = 0;
            int totalPlatform = ccuWeb + ccuAD + ccuIOS + ccuWP + ccuFB + ccuDT;
            // Clamp platform counters if they drifted above actual ccu
            int effectiveWeb = (totalPlatform > ccu) ? Math.max(0, ccuWeb - (totalPlatform - ccu)) : ccuWeb;
            int ccuOT = Math.max(0, ccu - (effectiveWeb + ccuAD + ccuIOS + ccuWP + ccuFB + ccuDT));
            this.serverInfoSrv.logCCU(ccu, effectiveWeb, ccuAD, ccuIOS, ccuWP, ccuFB, ccuDT, ccuOT);
        }
    }

    private final class GameLoopTask
    implements Runnable {
        private GameLoopTask() {
        }

        @Override
        public void run() {
            BaseGameExtension.this.gameLoop();
        }
    }

    @SuppressWarnings("unchecked")
    private void safeAddHandler(short id, String className) {
        try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
            Class clazz = Class.forName(className);
            this.addRequestHandler(id, clazz);
            Debug.trace("Registered handler " + id + " → " + className);
        } catch (Throwable e) {
            Debug.trace("SKIP handler " + id + " (" + className + "): " + e.getMessage());
        }
    }

}

