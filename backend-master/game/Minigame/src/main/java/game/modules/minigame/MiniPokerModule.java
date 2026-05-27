/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.BitZeroServer
 *  bitzero.server.core.BZEventParam
 *  bitzero.server.core.BZEventType
 *  bitzero.server.core.IBZEvent
 *  bitzero.server.core.IBZEventListener
 *  bitzero.server.core.IBZEventParam
 *  bitzero.server.core.IBZEventType
 *  bitzero.server.entities.User
 *  bitzero.server.exceptions.BZException
 *  bitzero.server.extensions.BZExtension
 *  bitzero.server.extensions.BaseClientRequestHandler
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.server.extensions.data.DataCmd
 *  bitzero.server.util.TaskScheduler
 *  bitzero.util.common.business.Debug
 *  com.vinplay.dal.service.MiniGameService
 *  com.vinplay.dal.service.impl.CacheServiceImpl
 *  com.vinplay.dal.service.impl.MiniGameServiceImpl
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 *  com.vinplay.vbee.common.utils.CommonUtils
 */
package game.modules.minigame;

import bitzero.server.BitZeroServer;
import bitzero.server.core.BZEventParam;
import bitzero.server.core.BZEventType;
import bitzero.server.core.IBZEvent;
import bitzero.server.core.IBZEventListener;
import bitzero.server.core.IBZEventParam;
import bitzero.server.core.IBZEventType;
import bitzero.server.entities.User;
import bitzero.server.exceptions.BZException;
import bitzero.server.extensions.BZExtension;
import bitzero.server.extensions.BaseClientRequestHandler;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.extensions.data.DataCmd;
import bitzero.server.util.TaskScheduler;
import bitzero.util.common.business.Debug;
import com.vinplay.dal.service.MiniGameService;
import com.vinplay.dal.service.impl.CacheServiceImpl;
import com.vinplay.dal.service.impl.MiniGameServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import com.vinplay.vbee.common.utils.CommonUtils;
import game.modules.minigame.cmd.rev.minipoker.AutoPlayMiniPokerCmd;
import game.modules.minigame.cmd.rev.minipoker.ChangeRoomMiniPokerCmd;
import game.modules.minigame.cmd.rev.minipoker.PlayMiniPokerCmd;
import game.modules.minigame.cmd.rev.minipoker.SubscribeMiniPokerCmd;
import game.modules.minigame.cmd.rev.minipoker.UnSubscribeMiniPokerCmd;
import game.modules.minigame.cmd.send.minipoker.MiniPokerX2Msg;
import game.modules.minigame.cmd.send.minipoker.ResultMiniPokerMsg;
import game.modules.minigame.entities.BotMinigame;
import game.modules.minigame.room.MGRoom;
import game.modules.minigame.room.MGRoomMiniPoker;
import game.modules.minigame.utils.MiniGameUtils;
import game.utils.ConfigGame;
import game.utils.GameUtils;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MiniPokerModule
extends BaseClientRequestHandler {
    public static Map<String, MGRoom> rooms = new HashMap<String, MGRoom>();
    private MiniGameService mgService = new MiniGameServiceImpl();
    private int countBot100 = 0;
    private int countBot1000 = 0;
    private int countBot10000 = 0;
    private GameLoopTask gameLoopTask = new GameLoopTask();
    private static String ngayX2;
    private static Runnable miniPokerX2Task;
    private static String gameName;

    public void init() {
        super.init();
        long[] pots = new long[6];
        long[] funds = new long[6];
        try {
            pots = this.mgService.getPots(Games.MINI_POKER.getName());
            Debug.trace(("MINI POKER POTS: " + CommonUtils.arrayLongToString((long[])pots)));
            funds = this.mgService.getFunds(Games.MINI_POKER.getName());
            Debug.trace(("MINI POKER FUNDS: " + CommonUtils.arrayLongToString((long[])funds)));
        }
        catch (SQLException e) {
            Debug.trace((Object[])new Object[]{"Get mini poker pot error ", e.getMessage()});
        }
        rooms.put(Games.MINI_POKER.getName() + "_vin_100", new MGRoomMiniPoker(gameName + "_vin_100", (byte)1, pots[0], funds[0], 100L, 500000L));
        rooms.put(Games.MINI_POKER.getName() + "_vin_1000", new MGRoomMiniPoker(gameName + "_vin_1000", (byte)1, pots[1], funds[1], 1000L, 5000000L));
        rooms.put(Games.MINI_POKER.getName() + "_vin_10000", new MGRoomMiniPoker(gameName + "_vin_10000", (byte)1, pots[2], funds[2], 10000L, 50000000L));
        rooms.put(Games.MINI_POKER.getName() + "_xu_1000", new MGRoomMiniPoker(gameName + "_xu_1000", (byte)0, pots[3], funds[3], 1000L, 5000000L));
        rooms.put(Games.MINI_POKER.getName() + "_xu_10000", new MGRoomMiniPoker(gameName + "_xu_10000", (byte)0, pots[4], funds[4], 10000L, 50000000L));
        rooms.put(Games.MINI_POKER.getName() + "_xu_100000", new MGRoomMiniPoker(gameName + "_xu_100000", (byte)0, pots[5], funds[5], 100000L, 500000000L));
        Debug.trace("INIT MINI POKER DONE");
        CacheServiceImpl sv = new CacheServiceImpl();
        try {
            sv.removeKey("mini_poker_last_day_x2");
        }
        catch (KeyNotFoundException keyNotFoundException) {
            // empty catch block
        }
        int lastDayFinish = MiniGameUtils.getLastDayX2("mini_poker_last_day_x2");
        int[] daysX2 = MiniGameUtils.getX2Days("mini_poker_days_x2");
        ngayX2 = MiniGameUtils.calculateTimeX2AsString(daysX2, lastDayFinish, "mini_poker_time_x2");
        int nextX2Time = MiniGameUtils.calculateTimeX2(daysX2, lastDayFinish, "mini_poker_time_x2");
        Debug.trace((gameName + " Ngay X2: " + ngayX2 + " remain time= " + nextX2Time));
        if (nextX2Time > 0) {
            BitZeroServer.getInstance().getTaskScheduler().schedule(miniPokerX2Task, nextX2Time, TimeUnit.SECONDS);
        } else {
            MiniPokerModule.startX2();
        }
        this.getParentExtension().addEventListener((IBZEventType)BZEventType.USER_DISCONNECT, (IBZEventListener)this);
        BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate((Runnable)this.gameLoopTask, 10, 1, TimeUnit.SECONDS);
    }

    public void handleServerEvent(IBZEvent ibzevent) throws BZException {
        if (ibzevent.getType() == BZEventType.USER_DISCONNECT) {
            User user = (User)ibzevent.getParameter((IBZEventParam)BZEventParam.USER);
            this.userDis(user);
        }
    }

    private void userDis(User user) {
        MGRoomMiniPoker room = (MGRoomMiniPoker)user.getProperty("MGROOM_MINI_POKER_INFO");
        if (room != null) {
            room.quitRoom(user);
            room.stopAutoPlay(user);
        }
    }

    public void handleClientRequest(User user, DataCmd dataCmd) {
        switch (dataCmd.getId()) {
            case 4001: {
                if (GameUtils.disablePlayMiniGame(user)) {
                    return;
                }
                this.playMiniPoker(user, dataCmd);
                break;
            }
            case 4003: {
                this.subScribeMiniPoker(user, dataCmd);
                break;
            }
            case 4004: {
                this.unSubScribeMiniPoker(user, dataCmd);
                break;
            }
            case 4005: {
                this.changeRoom(user, dataCmd);
                break;
            }
            case 4006: {
                if (GameUtils.disablePlayMiniGame(user)) {
                    return;
                }
                this.autoPlay(user, dataCmd);
            }
        }
    }

    private void subScribeMiniPoker(User user, DataCmd dataCmd) {
        SubscribeMiniPokerCmd cmd = new SubscribeMiniPokerCmd(dataCmd);
        MGRoomMiniPoker room = this.getRoom(cmd.roomId);
        if (room != null) {
            room.joinRoom(user);
            room.updatePotToUser(user);
            MiniPokerX2Msg msg = new MiniPokerX2Msg();
            msg.ngayX2 = ngayX2;
            this.send((BaseMsg)msg, user);
        } else {
            Debug.trace(("MINI POKER SUBSCRIBE: room " + cmd.roomId + " not found"));
        }
    }

    private void unSubScribeMiniPoker(User user, DataCmd dataCmd) {
        UnSubscribeMiniPokerCmd cmd = new UnSubscribeMiniPokerCmd(dataCmd);
        MGRoomMiniPoker room = this.getRoom(cmd.roomId);
        if (room != null) {
            room.stopAutoPlay(user);
            room.quitRoom(user);
        } else {
            Debug.trace(("MINI POKER UNSUBSCRIBE: room " + cmd.roomId + " not found"));
        }
    }

    private void changeRoom(User user, DataCmd dataCmd) {
        ChangeRoomMiniPokerCmd cmd = new ChangeRoomMiniPokerCmd(dataCmd);
        MGRoomMiniPoker roomLeaved = this.getRoom(cmd.roomLeavedId);
        MGRoomMiniPoker roomJoined = this.getRoom(cmd.roomJoinedId);
        if (roomLeaved != null && roomJoined != null) {
            roomLeaved.stopAutoPlay(user);
            roomLeaved.quitRoom(user);
            roomJoined.joinRoom(user);
            roomJoined.updatePotToUser(user);
        } else {
            Debug.trace(("MINI POKER: change room error, leaved= " + cmd.roomLeavedId + ", joined= " + cmd.roomJoinedId));
        }
    }

    private void playMiniPoker(User user, DataCmd dataCmd) {
        PlayMiniPokerCmd cmd = new PlayMiniPokerCmd(dataCmd);
        String roomName = this.getRoomName(cmd.moneyType, cmd.betValue);
        MGRoomMiniPoker room = (MGRoomMiniPoker)rooms.get(roomName);
        if (room != null) {
            room.play(user, cmd.betValue);
        }
    }

    private void autoPlay(User user, DataCmd dataCMD) {
        AutoPlayMiniPokerCmd cmd = new AutoPlayMiniPokerCmd(dataCMD);
        MGRoomMiniPoker room = (MGRoomMiniPoker)user.getProperty("MGROOM_MINI_POKER_INFO");
        if (room != null) {
            if (cmd.autoPlay == 1) {
                short result = room.play(user);
                if (result != 1 && result != 2 && result != 12 && result != 102 && result != 100) {
                    room.autoPlay(user);
                }
            } else {
                room.stopAutoPlay(user);
            }
        }
    }

    private String getRoomName(short moneyType, long baseBetting) {
        String moneyTypeStr = "xu";
        if (moneyType == 1) {
            moneyTypeStr = "vin";
        }
        return Games.MINI_POKER.getName() + "_" + moneyTypeStr + "_" + baseBetting;
    }

    private MGRoomMiniPoker getRoom(byte roomId) {
        short moneyType = this.getMoneyTypeFromRoomId(roomId);
        long baseBetting = this.getBaseBetting(roomId);
        String roomName = this.getRoomName(moneyType, baseBetting);
        MGRoomMiniPoker room = (MGRoomMiniPoker)rooms.get(roomName);
        return room;
    }

    private short getMoneyTypeFromRoomId(byte roomId) {
        if ((0 <= roomId) && (roomId < 3)) {
            return 1;
        }
        return 0;
    }

    private long getBaseBetting(byte roomId) {
        switch (roomId) {
            case 0: {
                return 100L;
            }
            case 1: {
                return 1000L;
            }
            case 2: {
                return 10000L;
            }
            case 3: {
                return 1000L;
            }
            case 4: {
                return 10000L;
            }
            case 5: {
                return 100000L;
            }
        }
        return 0L;
    }

    public static void startX2() {
        ngayX2 = "";
        MiniPokerX2Msg msg = new MiniPokerX2Msg();
        msg.ngayX2 = ngayX2;
        for (MGRoom room : rooms.values()) {
            room.sendMessageToRoom(msg);
        }
    }

    public static void stopX2() {
        ngayX2 = MiniGameUtils.calculateTimeX2AsString(MiniGameUtils.getX2Days("mini_poker_days_x2"), MiniGameUtils.getLastDayX2("mini_poker_last_day_x2"), "mini_poker_time_x2");
        MiniPokerX2Msg msg = new MiniPokerX2Msg();
        msg.ngayX2 = ngayX2;
        for (MGRoom room : rooms.values()) {
            room.sendMessageToRoom(msg);
        }
        Calendar cal = Calendar.getInstance();
        int today = cal.get(7);
        MiniGameUtils.saveLastDayX2("mini_poker_last_day_x2", today);
        int lastDayX2 = MiniGameUtils.getLastDayX2("mini_poker_last_day_x2");
        int nextX2Time = MiniGameUtils.calculateTimeX2(MiniGameUtils.getX2Days("mini_poker_days_x2"), lastDayX2, "mini_poker_time_x2");
        BitZeroServer.getInstance().getTaskScheduler().schedule(miniPokerX2Task, nextX2Time, TimeUnit.SECONDS);
    }

    private int getCountTimeBot100() {
        int n = ConfigGame.getIntValue("mini_poker_bot_100", 0);
        if (n == 0) {
            return 0;
        }
        if (BotMinigame.isNight()) {
            n *= 3;
        }
        return n;
    }

    private int getCountTimeBot1000() {
        int n = ConfigGame.getIntValue("mini_poker_bot_1000", 0);
        if (n == 0) {
            return 0;
        }
        if (BotMinigame.isNight()) {
            n *= 5;
        }
        return n;
    }

    private int getCountTimeBot10000() {
        int n = ConfigGame.getIntValue("poke_go_bot_10000", 0);
        if (n == 0) {
            return 0;
        }
        if (BotMinigame.isNight()) {
            n *= 10;
        }
        return n;
    }

    private void gameLoop() {
        List<String> bots;
        MGRoomMiniPoker room;
        ++this.countBot100;
        if (this.countBot100 >= this.getCountTimeBot100()) {
            this.countBot100 = 0;
            bots = BotMinigame.getBots(ConfigGame.getIntValue("mini_poker_num_bot_100"), "vin");
            for (String bot : bots) {
                if (bot == null) continue;
                room = (MGRoomMiniPoker)rooms.get(Games.MINI_POKER.getName() + "_vin_100");
                room.play(bot, 100L);
            }
        }
        ++this.countBot1000;
        if (this.countBot1000 >= this.getCountTimeBot1000()) {
            this.countBot1000 = 0;
            bots = BotMinigame.getBots(ConfigGame.getIntValue("mini_poker_num_bot_1000"), "vin");
            for (String bot : bots) {
                if (bot == null) continue;
                room = (MGRoomMiniPoker)rooms.get(Games.MINI_POKER.getName() + "r_vin_1000");
                room.play(bot, 1000L);
            }
        }
        ++this.countBot10000;
        if (this.countBot10000 >= this.getCountTimeBot10000()) {
            this.countBot10000 = 0;
            bots = BotMinigame.getBots(ConfigGame.getIntValue("mini_poker_num_bot_10000"), "vin");
            for (String bot : bots) {
                if (bot == null) continue;
                room = (MGRoomMiniPoker)rooms.get(Games.MINI_POKER.getName() + "_vin_10000");
                room.play(bot, 10000L);
            }
        }
    }

    static {
        miniPokerX2Task = new MiniPokerX2Task();
        gameName = Games.MINI_POKER.getName();
    }

    private static final class MiniPokerX2Task
    implements Runnable {
        private MiniPokerX2Task() {
        }

        @Override
        public void run() {
            MiniPokerModule.startX2();
            MGRoomMiniPoker room100 = (MGRoomMiniPoker)rooms.get(Games.MINI_POKER.getName() + "_vin_100");
            room100.startHuX2();
            MGRoomMiniPoker room1000 = (MGRoomMiniPoker)rooms.get(Games.MINI_POKER.getName() + "_vin_1000");
            room1000.startHuX2();
            Debug.trace("POKEGO START X2");
        }
    }

    private final class GameLoopTask
    implements Runnable {
        private GameLoopTask() {
        }

        @Override
        public void run() {
            MiniPokerModule.this.gameLoop();
        }
    }

}

