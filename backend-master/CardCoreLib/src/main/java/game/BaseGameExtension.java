/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.engine.sessions.ISession
 *  bitzero.server.BitZeroServer
 *  bitzero.server.core.BZEventType
 *  bitzero.server.core.IBZEventType
 *  bitzero.server.entities.User
 *  bitzero.server.entities.data.ISFSObject
 *  bitzero.server.exceptions.BZException
 *  bitzero.server.extensions.BZExtension
 *  bitzero.server.extensions.data.DataCmd
 *  bitzero.util.ExtensionUtility
 *  bitzero.util.common.business.Debug
 *  bitzero.util.socialcontroller.bean.UserInfo
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.hazelcast.HazelcastLoader
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.rmq.RMQApi
 */
package game;

import bitzero.engine.sessions.ISession;
import bitzero.server.BitZeroServer;
import bitzero.server.core.BZEventType;
import bitzero.server.core.IBZEventType;
import bitzero.server.entities.User;
import bitzero.server.entities.data.ISFSObject;
import bitzero.server.exceptions.BZException;
import bitzero.server.extensions.BZExtension;
import bitzero.server.extensions.data.DataCmd;
import bitzero.util.ExtensionUtility;
import bitzero.util.common.business.Debug;
import bitzero.util.socialcontroller.bean.UserInfo;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.hazelcast.HazelcastLoader;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.rmq.RMQApi;
import game.ConfigFake.UpdateFakeRoomTimer;
import game.eventHandlers.LoginSuccessHandler;
import game.modules.gameRoom.GameRoomModule;
import game.modules.player.PlayerModule;
import game.modules.player.cmd.rev.LoginCmd;
import game.modules.tour.TourModule;
import game.utils.GameUtils;
import game.xocdia.conf.XocDiaConfig;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class BaseGameExtension
extends BZExtension {
    public static volatile int ccuWeb = 0;
    public static volatile int ccuAD = 0;
    public static volatile int ccuIOS = 0;
    public static volatile int ccuWP = 0;
    public static volatile int ccuFB = 0;
    public static volatile int ccuDT = 0;

    public void init() {
        Debug.trace((Object[])new Object[]{"BaseGameExtension init"});
        try {
            HazelcastLoader.start();
            RMQApi.start((String)"./config/rmq.properties");
        }
        catch (IOException e) {
            Debug.trace((Object[])new Object[]{e});
        }
        // SUN-816: register Hazelcast cacheToken listener. Fires on every
        // card-game server (poker/tlmn/sam/binh/lieng/baicao/bacay/caro/
        // cotuong/coup/pokertour/xizach/xocdia) when a user's accessToken
        // is removed from cacheToken (= new device login), so the stale
        // bitzero session is disconnected regardless of which game the
        // user was playing. Replaces the SUN-767 RMQ direct-queue fanout
        // gap for the entire card-games tier in one hook.
        try {
            com.vinplay.vbee.common.session.SessionKickListener.register(
                new com.vinplay.vbee.common.session.SessionKickListener.KickHandler() {
                    @Override
                    public void kick(String accessToken, String nickname, String reason) {
                        if (nickname == null || nickname.isEmpty()) return;
                        try {
                            // BitzeroAll returns a single User (not a list).
                            bitzero.server.entities.User u =
                                    ExtensionUtility.getExtension().getApi().getUserByName(nickname);
                            if (u == null) return;
                            game.queue.ForceLogoutMsg wire = new game.queue.ForceLogoutMsg();
                            wire.reason = reason != null ? reason : "DUPLICATE_LOGIN";
                            wire.newLoginTime = System.currentTimeMillis();
                            try { ExtensionUtility.getExtension().send(wire, u); } catch (Exception ignored) {}
                            // SUN-1049: delay the disconnect 500ms so cmd 20200 actually
                            // reaches the client. Immediate disconnectUser after an async
                            // send races the writer thread — the packet gets discarded on
                            // socket close before it hits the wire, and the FE never shows
                            // the force-logout popup (user symptom: "not kicked"). Mirrors
                            // the BZBannedUserManager scheduler pattern.
                            final bitzero.server.entities.User uRef = u;
                            try {
                                BitZeroServer.getInstance().getTaskScheduler().schedule(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            BitZeroServer.getInstance().getAPIManager().getBzApi().disconnectUser(uRef);
                                        } catch (Exception ignored) {}
                                    }
                                }, 500, TimeUnit.MILLISECONDS);
                            } catch (Exception ignored) {
                                try {
                                    BitZeroServer.getInstance().getAPIManager().getBzApi().disconnectUser(u);
                                } catch (Exception ignored2) {}
                            }
                            Debug.trace((Object[])new Object[]{"SessionKickListener: kicked "
                                    + GameUtils.gameName + " session for " + nickname
                                    + " reason=" + reason});
                        } catch (Throwable t) {
                            Debug.trace((Object[])new Object[]{"SessionKickListener "
                                    + GameUtils.gameName + " kick err nick=" + nickname
                                    + " err=" + t.getMessage()});
                        }
                    }
                });
        } catch (Exception regErr) {
            Debug.trace((Object[])new Object[]{"SessionKickListener register failed: "
                    + regErr.getMessage()});
        }
        if (GameUtils.isLog) {
            try {
                MongoDBConnectionFactory.init();
            }
            catch (IOException e) {
                Debug.trace((Object[])new Object[]{e});
            }
        }
        if (GameUtils.gameName.equalsIgnoreCase("XocDia") || GameUtils.gameName.equalsIgnoreCase("PokerTour")) {
            try {
                if (GameUtils.gameName.equalsIgnoreCase("XocDia")) {
                    XocDiaConfig.instance();
                }
                GameCommon.init();
            }
            catch (Exception e) {
                Debug.trace((Object[])new Object[]{e});
            }
        }
        try {
            this.addRequestHandler((short)1000, PlayerModule.class);
            Debug.trace((Object[])new Object[]{"BaseGameExtension PlayerModule init"});
            this.addRequestHandler((short)3000, GameRoomModule.class);
            Debug.trace((Object[])new Object[]{"BaseGameExtension GameRoomModule init"});
            if (GameUtils.gameName.equalsIgnoreCase("PokerTour")) {
                this.addRequestHandler((short)5000, TourModule.class);
                Debug.trace((Object[])new Object[]{"BaseGameExtension TourModule init"});
            }
            this.addEventHandler((IBZEventType)BZEventType.USER_LOGIN, LoginSuccessHandler.class);
            Debug.trace((Object[])new Object[]{"BaseGameExtension LoginSuccessHandler init"});
        }
        catch (Exception e) {
            Debug.trace((Object[])new Object[]{e});
        }
        if (GameUtils.gameName.equalsIgnoreCase("Tlmn")) {
            BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate((Runnable)new UpdateFakeRoomTimer(), 1, 30, TimeUnit.SECONDS);
        }
        // CCU logging thread — publishes platform CCU to RabbitMQ every 60 seconds
        final com.vinplay.dal.service.impl.ServerInfoServiceImpl ccuService = new com.vinplay.dal.service.impl.ServerInfoServiceImpl();
        Thread ccuThread = new Thread(new Runnable() {
            public void run() {
                try { Thread.sleep(30000); } catch (InterruptedException ignored) { return; }
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        int totalCCU = 0;
                        try { totalCCU = ExtensionUtility.globalUserManager.getUserCount(); } catch (Exception ignored) {}
                        ccuService.logCCU(totalCCU, ccuWeb, ccuAD, ccuIOS, ccuWP, ccuFB, ccuDT, 0);
                    } catch (Exception ignored) {}
                    try { Thread.sleep(60000); } catch (InterruptedException ignored) { return; }
                }
            }
        }, "CCU-Logger-" + GameUtils.gameName);
        ccuThread.setDaemon(true);
        ccuThread.start();
    }

    public void doLogin(short s, ISession iSession, DataCmd dataCmd) throws BZException {
        if (s != 1 && s != 2) {
            return;
        }
        if (s == 2) {
            LoginCmd cmd = new LoginCmd(dataCmd);
            if (cmd.nickname.equalsIgnoreCase("Johan1") && cmd.sessionKey.equalsIgnoreCase("12FIPjg5e874RzU0Po")) {
                UserInfo info = GameUtils.getAdminInfo();
                ExtensionUtility.instance().canLogin(info, "", iSession);
                Debug.trace((Object[])new Object[]{"Basegame CardCoreLib canlogin 1 init ", cmd.nickname});
            }
            return;
        }
        Debug.trace((Object[])new Object[]{"Check request cmd GameUtils.isMainTain : " + GameUtils.isMainTain});
        if (GameUtils.isMainTain) {
            ExtensionUtility.instance().sendLoginResponse(iSession, 3);
            return;
        }
        LoginCmd cmd = new LoginCmd(dataCmd);
        UserInfo info = null;
        info = GameUtils.dev_mod ? GameUtils.getUserInfoDev(cmd.nickname, cmd.sessionKey) : GameUtils.getUserInfo(cmd.nickname, cmd.sessionKey);
        Debug.trace((Object[])new Object[]{"Check request cmd UserInfo null : " + info == null});
        if (info != null) {
            if (info.getUsername() == null || info.getUsername().length() == 0) {
                ExtensionUtility.instance().sendLoginResponse(iSession, 2);
            } else {
                User user = ExtensionUtility.instance().canLogin(info, "", iSession);
                Debug.trace((Object[])new Object[]{"Basegame CardCoreLib canlogin 2 ", cmd.nickname});
                if (user != null) {
                    saveCCUPlatform(user);
                }
            }
        } else {
            ExtensionUtility.instance().sendLoginResponse(iSession, 1);
        }
    }

    public void doLogin(ISession iSession, ISFSObject iSFSObject) throws Exception {
    }

    public static void saveCCUPlatform(User user) {
        try {
            Object pf = user.getProperty("pf");
            String platform = pf != null ? pf.toString() : "web";
            switch (platform) {
                case "web": ccuWeb++; break;
                case "ad": ccuAD++; break;
                case "ios": ccuIOS++; break;
                case "wp": ccuWP++; break;
                case "fb": ccuFB++; break;
                case "dt": ccuDT++; break;
            }
        } catch (Exception ignored) {}
    }
}

