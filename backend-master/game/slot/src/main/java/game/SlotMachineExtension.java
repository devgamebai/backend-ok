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
 *  bitzero.server.exceptions.BZException
 *  bitzero.server.extensions.BZExtension
 *  bitzero.server.extensions.data.DataCmd
 *  bitzero.server.util.TaskScheduler
 *  bitzero.util.ExtensionUtility
 *  bitzero.util.common.business.Debug
 *  bitzero.util.socialcontroller.bean.UserInfo
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.hazelcast.HazelcastLoader
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.rmq.RMQApi
 */
package game;

import bitzero.engine.sessions.ISession;
import bitzero.server.BitZeroServer;
import bitzero.server.core.BZEventType;
import bitzero.server.entities.data.ISFSObject;
import bitzero.server.exceptions.BZException;
import bitzero.server.extensions.BZExtension;
import bitzero.server.extensions.data.DataCmd;
import bitzero.util.ExtensionUtility;
import bitzero.util.common.business.Debug;
import bitzero.util.socialcontroller.bean.UserInfo;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.usercore.utils.GameThirdPartyInit;
import com.vinplay.vbee.common.hazelcast.HazelcastLoader;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.rmq.RMQApi;
import com.vinplay.vbee.common.statics.Consts;

import game.GameConfig.GameConfig;
import game.eventHandlers.LoginSuccessHandler;
import game.modules.slot.*;
import game.modules.slot.cmd.rev.LoginCmd;
import game.modules.slot.entities.BotMinigame;
import game.util.ConfigGame;
import game.util.GameUtils;

import java.util.concurrent.TimeUnit;

public class SlotMachineExtension
extends BZExtension {
    public static volatile int ccuWeb = 0;
    public static volatile int ccuAD = 0;
    public static volatile int ccuIOS = 0;
    public static volatile int ccuWP = 0;
    public static volatile int ccuFB = 0;
    public static volatile int ccuDT = 0;
    private int countReloadConfig = 0;
    private int countCCULog = 0;
    private final Runnable gameLoopTask = new GameLoopTask();

    public void init() {
		try {
			RMQApi.start(Consts.RMQ_CONFIG_FILE);
			HazelcastLoader.start();
			MongoDBConnectionFactory.init();
			ConnectionPool.start(Consts.DB_CONFIG_FILE);
			ConfigGame.reload();
			BotMinigame.loadData();
			GameCommon.init();
			GameConfig.getInstance().init();
			GameThirdPartyInit.init();
		} catch (Exception e) {
			Debug.trace("INIT MINIGAME ERROR " + e.getMessage());
		}
		// SUN-816: register Hazelcast cacheToken listener. Kicks the stale
		// bitzero session on this slot server whenever a new device logs in
		// for the same user (the PortalUtils.loginSuccess removes the old
		// accessToken from cacheToken, which fires entryRemoved here).
		try {
			com.vinplay.vbee.common.session.SessionKickListener.register(
				new com.vinplay.vbee.common.session.SessionKickListener.KickHandler() {
					@Override
					public void kick(String accessToken, String nickname, String reason) {
						if (nickname == null || nickname.isEmpty()) return;
						try {
							java.util.List<bitzero.server.entities.User> users =
									ExtensionUtility.getExtension().getApi().getUserByName(nickname);
							if (users == null || users.isEmpty()) return;
							game.queue.ForceLogoutMsg wire = new game.queue.ForceLogoutMsg();
							wire.reason = reason != null ? reason : "DUPLICATE_LOGIN";
							wire.newLoginTime = System.currentTimeMillis();
							for (bitzero.server.entities.User u : users) {
								try { ExtensionUtility.getExtension().send(wire, u); } catch (Exception ignored) {}
								try {
									BitZeroServer.getInstance().getAPIManager().getBzApi().disconnectUser(u);
								} catch (Exception ignored) {}
							}
							Debug.trace("SessionKickListener: kicked " + users.size()
									+ " slot session(s) for " + nickname + " reason=" + reason);
						} catch (Throwable t) {
							Debug.trace("SessionKickListener slot kick err nick=" + nickname
									+ " err=" + t.getMessage());
						}
					}
				});
		} catch (Exception e) {
			Debug.trace("SessionKickListener register failed: " + e.getMessage());
		}
        this.addRequestHandler((short)1000, HallSlotModule.class);

          // Production handler IDs with staging modules (safeAddHandler skips on error)
          this.addRequestHandler((short)10000, HallSlotModule.class);
          safeAddHandler((short)2000, "game.modules.slot.KhoBauModule");          // Slot1 - Pirate King / Tay Du
          safeAddHandler((short)3000, "game.modules.slot.NuDiepVienModule");      // Slot3/Slot10
          safeAddHandler((short)4000, "game.modules.slot.AvengerModule");         // Slot4/Slot7
          safeAddHandler((short)5000, "game.modules.slot.VQVModule");             // Slot5/Slot8
          safeAddHandler((short)6000, "game.modules.slot.ChiemTinhModule");       // Slot6 - Chiem Tinh
          safeAddHandler((short)7000, "game.modules.slot.ThanTaiModule");        // Slot - Than Tai (God of Wealth)
          safeAddHandler((short)8000, "game.modules.slot.GalaxyModule");         // Kim Cuong / Galaxy
          safeAddHandler((short)9000, "game.modules.slot.DragonBallModule");     // Dragon Ball (Slot11Icon engine)
          safeAddHandler((short)11000, "game.modules.slot.AuditionModule");      // Tay Du Than Khi / Audition
          safeAddHandler((short)15000, "game.modules.slot.TamHungModule");       // Kho Tang Ngu Long / TamHung
          safeAddHandler((short)17000, "game.modules.slot.ThanBaiModule");       // The Witcher / ThanBai
          safeAddHandler((short)18000, "game.modules.slot.ThuyCungModule");      // Thuy Cung / Aquarium
          safeAddHandler((short)12000, "game.modules.slot.KhoBauModule");         // Slot - reuse KhoBau
          safeAddHandler((short)13000, "game.modules.slot.NuDiepVienModule");     // Slot - reuse NuDiepVien
          safeAddHandler((short)14000, "game.modules.slot.AvengerModule");        // Slot - reuse Avenger
          safeAddHandler((short)16000, "game.modules.slot.ChiemTinhModule");      // Slot11Bikini - reuse ChiemTinh
          safeAddHandler((short)19000, "game.modules.slot.ThanDenModule");        // Thần Đèn Thần Tài
          this.addEventHandler(BZEventType.USER_LOGIN, LoginSuccessHandler.class);
          this.addEventHandler(BZEventType.USER_DISCONNECT, LoginSuccessHandler.class);
          BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.gameLoopTask, 10, 1, TimeUnit.SECONDS);
     }

    public void doLogin(short s, ISession iSession, DataCmd dataCmd) throws BZException {
        if (s != 1) {
            return;
        }
        LoginCmd cmd = new LoginCmd(dataCmd);
        UserInfo info = GameUtils.getUserInfo(cmd.nickname, cmd.sessionKey);
        if (info != null) {
            ExtensionUtility.instance().canLogin(info, "", iSession);
            Debug.trace("Slot canlogin 1 init");
        }
    }

    public void doLogin(ISession iSession, ISFSObject iSFSObject) throws Exception {
    }

    private void gameLoop() {
        ++this.countReloadConfig;
        if (this.countReloadConfig == 300) {
            Debug.trace((Object)"reload config");
            ConfigGame.reload();
            this.countReloadConfig = 0;
        }
        // CCU logging every 60 ticks (gameLoop runs every 1s, so 60s)
        ++this.countCCULog;
        if (this.countCCULog >= 60) {
            this.countCCULog = 0;
            try {
                int totalCCU = 0;
                try { totalCCU = ExtensionUtility.globalUserManager.getUserCount(); } catch (Exception ignored) {}
                new com.vinplay.dal.service.impl.ServerInfoServiceImpl().logCCU(
                    totalCCU, ccuWeb, ccuAD, ccuIOS, ccuWP, ccuFB, ccuDT, 0);
            } catch (Exception ignored) {}
        }
    }

    @SuppressWarnings("unchecked")
    private void safeAddHandler(short id, String className) {
        try {
            Class<?> clazz = Class.forName(className);
            this.addRequestHandler(id, clazz);
            System.out.println("Registered slot handler " + id + " → " + className);
        } catch (Exception e) {
            System.out.println("Skipped slot handler " + id + " (" + className + "): " + e.getMessage());
        }
    }

    private final class GameLoopTask
    implements Runnable {
        private GameLoopTask() {
        }

        @Override
        public void run() {
            SlotMachineExtension.this.gameLoop();
        }
    }

}

