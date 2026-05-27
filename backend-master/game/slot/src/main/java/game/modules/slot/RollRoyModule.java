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
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.server.extensions.data.DataCmd
 *  bitzero.server.util.TaskScheduler
 *  bitzero.util.ExtensionUtility
 *  bitzero.util.common.business.Debug
 *  com.vinplay.dal.service.MiniGameService
 *  com.vinplay.dal.service.SlotMachineService
 *  com.vinplay.dal.service.impl.CacheServiceImpl
 *  com.vinplay.usercore.service.UserService
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 *  com.vinplay.vbee.common.models.slot.SlotFreeSpin
 *  com.vinplay.vbee.common.utils.CommonUtils
 */
package game.modules.slot;

import bitzero.server.BitZeroServer;
import bitzero.server.core.BZEventParam;
import bitzero.server.core.BZEventType;
import bitzero.server.core.IBZEvent;
import bitzero.server.core.IBZEventParam;
import bitzero.server.entities.User;
import bitzero.server.exceptions.BZException;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.extensions.data.DataCmd;
import bitzero.util.ExtensionUtility;
import bitzero.util.common.business.Debug;
import com.vinplay.dal.service.impl.CacheServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import com.vinplay.vbee.common.models.slot.SlotFreeSpin;
import com.vinplay.vbee.common.utils.CommonUtils;
import game.BotJackpotTimer.BotJackPotRollRoyceTimer;
import game.modules.slot.cmd.rev.rollRoyce.*;
import game.modules.slot.cmd.send.rollRoy.RollRoyInfoMsg;
import game.modules.slot.cmd.send.rollRoy.UpdatePotRollRoyMsg;
import game.modules.slot.entities.BotMinigame;
import game.modules.slot.room.RollRoyRoom;
import game.modules.slot.utils.SlotUtils;
import game.util.ConfigGame;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RollRoyModule
extends SlotModule {
    private static long referenceId = 1L;
    private String fullLines = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20";
    private Runnable x2Task = new SlotModule.X2Task();
    private byte[] x2Arr = new byte[3];
    private List<User> users = new ArrayList<User>();
    private int countUpdateJackpot = 0;

    private static String slot7IconJackpotFund = "SlotRRoyce_Jackpot_Fund";
    private static String slot7IconMinigameFund = "SlotRRoyce_Minigame_Fund";

    public final String keyBotJackpotRRoyce = "Bot_SlotRRoyce_time";
    private static RollRoyModule _instance;
    public static RollRoyModule getInstance(){
        return _instance;
    }
    public RollRoyModule() {
        this.gameName = Games.ROLL_ROYE.getName();
    }

    public void init() {
        super.init();
        long[] funds = new long[3];
        int[] initPotValues = {500000,5000000,50000000};
        long[] fundsJackPot = new long[3];
        long[] fundMiniGame = new long[3];

        long[] timeJackPot = new long[3];
        try {
            this.pots = this.service.getPots(this.gameName);
            Debug.trace(this.gameName + " POTS: " + CommonUtils.arrayLongToString(this.pots));
            funds = this.service.getFunds(this.gameName);
            Debug.trace(this.gameName + " FUNDS: " + CommonUtils.arrayLongToString(funds));

            fundsJackPot = this.service.getFunds(slot7IconJackpotFund);
            Debug.trace("Slot7Icon FUNDS JACKPOT: " + CommonUtils.arrayLongToString(fundsJackPot));

            fundMiniGame = this.service.getFunds(slot7IconMinigameFund);
            Debug.trace("Slot7Icon FUNDS Minigame: " + CommonUtils.arrayLongToString(fundMiniGame));

            timeJackPot = this.service.getFunds(keyBotJackpotRRoyce);
            Debug.trace("Slot7Icon TIME JACKPOT: " + CommonUtils.arrayLongToString(timeJackPot));
        }
        catch (Exception e) {
            Debug.trace("Init " + this.gameName + " error ", e);
        }
        this.rooms.put(this.gameName + "_vin_100", new RollRoyRoom(this, (byte)0, this.gameName + "_vin_100", (short) 1,
                this.pots[0], funds[0], 100, initPotValues[0],
                fundsJackPot[0],slot7IconJackpotFund+ "_vin_100",
                fundMiniGame[0],slot7IconMinigameFund + "_vin_100"));
        this.rooms.put(this.gameName + "_vin_1000", new RollRoyRoom(this, (byte)1, this.gameName + "_vin_1000", (short) 1,
                this.pots[1], funds[1], 1000, initPotValues[1],
                fundsJackPot[1],slot7IconJackpotFund+ "_vin_1000",
                fundMiniGame[1],slot7IconMinigameFund + "_vin_1000"));
        this.rooms.put(this.gameName + "_vin_10000", new RollRoyRoom(this, (byte)2, this.gameName + "_vin_10000", (short) 1,
                this.pots[2], funds[2], 10000, initPotValues[2],
                fundsJackPot[2],slot7IconJackpotFund+ "_vin_10000",
                fundMiniGame[2],slot7IconMinigameFund + "_vin_10000"));
        Debug.trace("INIT " + this.gameName + " DONE");

        referenceId = this.slotService.getLastReferenceId(this.gameName);
        Debug.trace("START " + this.gameName + " REFERENCE ID= " + referenceId);
        CacheServiceImpl sv = new CacheServiceImpl();
        try {
            sv.removeKey(this.gameName + "_last_day_x2");
        }
        catch (KeyNotFoundException e2) {
            Debug.trace("KEY NOT FOUND");
        }
//        int lastDayFinish = SlotUtils.getLastDayX2(this.gameName);
//        this.ngayX2 = SlotUtils.calculateTimePokeGoX2AsString(this.gameName, SlotUtils.getX2Days(this.gameName), lastDayFinish);
//        int nextX2Time = SlotUtils.calculateTimePokeGoX2(this.gameName, SlotUtils.getX2Days(this.gameName), lastDayFinish);
//        Debug.trace(this.gameName + " Ngay X2: " + this.ngayX2 + ", remain time = " + nextX2Time);
        /*if (nextX2Time >= 0) {
            BitZeroServer.getInstance().getTaskScheduler().schedule(this.x2Task, nextX2Time, TimeUnit.SECONDS);
        } else {
            this.startX2();
        }*/
        this.getParentExtension().addEventListener(BZEventType.USER_DISCONNECT, this);
        BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.gameLoopTask, 10, 1, TimeUnit.SECONDS);
        BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(new BotJackPotRollRoyceTimer(timeJackPot, this), 10, 5, TimeUnit.SECONDS);
//        List<String>  bots = BotMinigame.getBots(10, "vin");
//        RollRoyRoom room = (RollRoyRoom)this.rooms.get(this.gameName + "_vin_100");
//        for(int i = 0;i<10000;i++){
//            room.play(bots.get(0), this.fullLines);
//        }
        _instance  = this;
    }
    @Override
    public long getBaseBetting(byte roomId) {
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
                return 10000L;
            }
            case 4: {
                return 1000L;
            }
            case 5: {
                return 10000L;
            }
            case 6: {
                return 100000L;
            }
        }
        return 0L;
    }
    @Override
    public long getNewReferenceId() {
        return ++referenceId;
    }

    public void updatePot(byte id, long value, byte x2) {
        this.pots[id] = value;
        this.x2Arr[id] = x2;
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastTimeUpdatePotToRoom >= 3000L) {
            UpdatePotRollRoyMsg msg = this.getPotsInfo();
            this.lastTimeUpdatePotToRoom = System.currentTimeMillis();
            SlotModule.SendMsgToAlLUsersThread t = new SlotModule.SendMsgToAlLUsersThread(msg);
            t.start();
        }
    }

    public UpdatePotRollRoyMsg getPotsInfo() {
        UpdatePotRollRoyMsg msg = new UpdatePotRollRoyMsg();
        msg.value100 = this.pots[0];
        msg.value1000 = this.pots[1];
        msg.value10000 = this.pots[2];
        msg.x2Room100 = this.x2Arr[0];
        msg.x2Room1000 = this.x2Arr[1];
        return msg;
    }

    public void handleServerEvent(IBZEvent ibzevent) throws BZException {
        if (ibzevent.getType() == BZEventType.USER_DISCONNECT) {
            User user = (User)ibzevent.getParameter((IBZEventParam)BZEventParam.USER);
            this.userDis(user);
        }
    }

    private void userDis(User user) {
        RollRoyRoom room = (RollRoyRoom)user.getProperty((Object)("MGROOM_" + this.gameName + "_INFO"));
        if (room != null) {
            room.quitRoom(user);
            room.stopAutoPlay(user);
        }
    }

    @Override
    public void handleClientRequest(User user, DataCmd dataCmd) {
        Debug.trace(this.gameName+"handleClientRequest " + dataCmd.getId());

        switch (dataCmd.getId()) {
            case 5003: {
                this.subScribe(user, dataCmd);
                break;
            }
            case 5004: {
                this.unSubScribe(user, dataCmd);
                break;
            }
            case 5005: {
                this.changeRoom(user, dataCmd);
                break;
            }
            case 5006: {
                this.autoPlay(user, dataCmd);
                break;
            }
            case 5001: {
                this.playRollRoy(user, dataCmd);
                break;
            }
            case 5013: {
                this.minimize(user, dataCmd);
            }
        }
    }

    public void updatePotToUser(User user) {
        UpdatePotRollRoyMsg msg = this.getPotsInfo();
        SlotUtils.sendMessageToUser((BaseMsg)msg, user);
    }

    public void updateUserInfo(User user, RollRoyRoom room) {
        RollRoyInfoMsg msg = new RollRoyInfoMsg();
        msg.ngayX2 = this.ngayX2;
        msg.remain = 0;
        msg.currentRoom = room.getId();
        SlotFreeSpin freeSpin = this.slotService.getLuotQuayFreeSlot(String.valueOf(this.gameName) + room.getBetValue(), user.getName());
        if (freeSpin != null && freeSpin.getLines() != null) {
            msg.freeSpin = (byte)freeSpin.getNum();
            msg.lines = freeSpin.getLines();
        }
        msg.currentMoney = this.userService.getMoneyUserCache(user.getName(), "vin");
        this.send((BaseMsg)msg, user);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected void subScribe(User user, DataCmd dataCmd) {
        SubscribeRollRoyCmd cmd = new SubscribeRollRoyCmd(dataCmd);
        if (cmd.roomId == -1) {
            this.updatePotToUser(user);
            List<User> list = this.users;
            synchronized (list) {
                this.users.add(user);
                return;
            }
        }
        List<User> list = this.users;
        synchronized (list) {
            this.users.remove((Object)user);
        }
        RollRoyRoom room = (RollRoyRoom)this.getRoom(cmd.roomId);
        if (room != null) {
            room.joinRoom(user);
            room.userMaximize(user);
            this.updatePotToUser(user);
            this.updateUserInfo(user, room);
        } else {
            Debug.trace((Object)(String.valueOf(this.gameName) + " SUBSCRIBE: room " + cmd.roomId + " not found"));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected void unSubScribe(User user, DataCmd dataCmd) {
        UnSubscribeRollRoyCmd cmd = new UnSubscribeRollRoyCmd(dataCmd);
        if (cmd.roomId == -1) {
            this.updatePotToUser(user);
            List<User> list = this.users;
            synchronized (list) {
                this.users.remove((Object)user);
                return;
            }
        }
        RollRoyRoom room = (RollRoyRoom)this.getRoom(cmd.roomId);
        if (room != null) {
            room.stopAutoPlay(user);
            room.quitRoom(user);
        } else {
            Debug.trace((Object)(String.valueOf(this.gameName) + " UNSUBSCRIBE: room " + cmd.roomId + " not found"));
        }
    }

    protected void minimize(User user, DataCmd dataCmd) {
        MinimizeRollRoyCmd cmd = new MinimizeRollRoyCmd(dataCmd);
        RollRoyRoom room = (RollRoyRoom)this.getRoom(cmd.roomId);
        if (room != null) {
            room.quitRoom(user);
            room.userMinimize(user);
        } else {
            Debug.trace((Object)(String.valueOf(this.gameName) + " MINIMIZE: room " + cmd.roomId + " not found"));
        }
    }

    protected void changeRoom(User user, DataCmd dataCmd) {
        ChangeRoomRollRoyCmd cmd = new ChangeRoomRollRoyCmd(dataCmd);
        RollRoyRoom roomLeaved = (RollRoyRoom)this.getRoom(cmd.roomLeavedId);
        RollRoyRoom roomJoined = (RollRoyRoom)this.getRoom(cmd.roomJoinedId);
        if (roomLeaved != null && roomJoined != null) {
            roomLeaved.stopAutoPlay(user);
            roomLeaved.quitRoom(user);
            roomJoined.joinRoom(user);
            this.updatePotToUser(user);
            this.updateUserInfo(user, roomJoined);
        } else {
            Debug.trace((Object)(String.valueOf(this.gameName) + ": change room error, leaved= " + cmd.roomLeavedId + ", joined= " + cmd.roomJoinedId));
        }
    }

    private void playRollRoy(User user, DataCmd dataCmd) {
        PlayRollRoyCmd cmd = new PlayRollRoyCmd(dataCmd);
        RollRoyRoom room = (RollRoyRoom)user.getProperty((Object)("MGROOM_" + this.gameName + "_INFO"));
        if (room != null) {
            room.play(user, cmd.lines);
        }
    }

    private void autoPlay(User user, DataCmd dataCMD) {
        AutoPlayRollRoyCmd cmd = new AutoPlayRollRoyCmd(dataCMD);
        RollRoyRoom room = (RollRoyRoom)user.getProperty((Object)("MGROOM_" + this.gameName + "_INFO"));
        if (room != null) {
            if (cmd.autoPlay == 1) {
                short result = room.play(user, cmd.lines);
                if (result != 3 && result != 4 && result != 101 && result != 102 && result != 100) {
                    room.autoPlay(user, cmd.lines, result);
                } else {
                    room.forceStopAutoPlay(user);
                }
            } else {
                room.stopAutoPlay(user);
            }
        }
    }

    @Override
    protected String getRoomName(short moneyType, long baseBetting) {
        String moneyTypeStr = "xu";
        if (moneyType == 1) {
            moneyTypeStr = "vin";
        }
        return String.valueOf(this.gameName) + "_" + moneyTypeStr + "_" + baseBetting;
    }

    public void sendMessageToRoomLobby(BaseMsg msg) {
        ArrayList<User> usersCopy = new ArrayList<User>(this.users);
        for (User user : usersCopy) {
            ExtensionUtility.getExtension().send(msg, user);
        }
    }

    @Override
    protected void gameLoop() {
        Debug.trace("gameLoop RollRoy  " );
        List<String> bots;
        RollRoyRoom room;
        ++this.countBot100;
        if (this.countBot100 >= this.getCountTimeBot(String.valueOf(this.gameName) + "_bot_100")) {
            if (this.countBot100 == this.getCountTimeBot(String.valueOf(this.gameName) + "_bot_100")) {
                bots = BotMinigame.getBots(ConfigGame.getIntValue(String.valueOf(this.gameName) + "_num_bot_100"), "vin");
                for (String bot : bots) {
                    if (bot == null) continue;
                    room = (RollRoyRoom)this.rooms.get(String.valueOf(this.gameName) + "_vin_100");
                    room.play(bot, this.fullLines);
                }
            }
            this.countBot100 = 0;
        }
        ++this.countBot1000;
        if (this.countBot1000 >= this.getCountTimeBot(String.valueOf(this.gameName) + "_bot_1000")) {
            if (this.countBot1000 == this.getCountTimeBot(String.valueOf(this.gameName) + "_bot_1000")) {
                bots = BotMinigame.getBots(ConfigGame.getIntValue(String.valueOf(this.gameName) + "_num_bot_1000"), "vin");
                for (String bot : bots) {
                    if (bot == null) continue;
                    room = (RollRoyRoom)this.rooms.get(String.valueOf(this.gameName) + "_vin_1000");
                    room.play(bot, this.fullLines);
                }
            }
            this.countBot1000 = 0;
        }
        ++this.countBot10000;
        if (this.countBot10000 >= this.getCountTimeBot(String.valueOf(this.gameName) + "_bot_10000")) {
            if (this.countBot10000 == this.getCountTimeBot(String.valueOf(this.gameName) + "_bot_10000")) {
                bots = BotMinigame.getBots(ConfigGame.getIntValue(String.valueOf(this.gameName) + "_num_bot_10000"), "vin");
                for (String bot : bots) {
                    if (bot == null) continue;
                    room = (RollRoyRoom)this.rooms.get(String.valueOf(this.gameName) + "_vin_10000");
                    room.play(bot, this.fullLines);
                }
            }
            this.countBot10000 = 0;
        }
        ++this.countUpdateJackpot;
        if (this.countUpdateJackpot >= 3) {
            UpdatePotRollRoyMsg msg = this.getPotsInfo();
            this.countUpdateJackpot = 0;
            this.sendMessageToRoomLobby(msg);
        }
    }
}

