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
 *  bitzero.util.common.business.Debug
 *  com.vinplay.dal.service.MiniGameService
 *  com.vinplay.dal.service.SlotMachineService
 *  com.vinplay.dal.service.impl.CacheServiceImpl
 *  com.vinplay.usercore.service.UserService
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
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
import bitzero.util.common.business.Debug;
import com.vinplay.dal.service.impl.CacheServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import com.vinplay.vbee.common.utils.CommonUtils;
import game.BotJackpotTimer.BotJackPotRangeRoverTimer;
import game.modules.slot.cmd.rev.rangeRover.AutoPlayRangeRoverCmd;
import game.modules.slot.cmd.rev.rangeRover.ChangeRoomRangeRoverCmd;
import game.modules.slot.cmd.rev.rangeRover.MinimizeRangeRoverCmd;
import game.modules.slot.cmd.rev.rangeRover.PlayRangeRoverCmd;
import game.modules.slot.cmd.rev.rangeRover.*;
import game.modules.slot.cmd.send.rangeRover.RangeRoverInfoMsg;
import game.modules.slot.cmd.send.rangeRover.UpdatePotRangeRoverMsg;
import game.modules.slot.entities.BotMinigame;
import game.modules.slot.room.RangeRoverRoom;
import game.modules.slot.utils.SlotUtils;
import game.util.ConfigGame;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class RangeRoverModule
        extends SlotModule {
    private static long referenceId = 1L;
    private String fullLines = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20";
    //    private Runnable x2Task = new X2Task(this);
    private Runnable x2Task = new X2Task();
    private byte[] x2Arr = new byte[4];
    protected int countBot5000;

    private static String slot7IconWildJackpotFund = "SlotRRover_Jackpot_Fund";
    private static String slot7IconWildMinigameFund = "SlotRRover_Minigame_Fund";

    public final String keyBotJackpotSlot7IconWild = "Bot_SlotRRover_time";

    public RangeRoverModule() {
        this.gameName = Games.RANGE_ROVER.getName();
    }

    private static RangeRoverModule _instance;
    public static RangeRoverModule getInstance(){
        return _instance;
    }

    public void init() {
        super.init();
        long[] funds = new long[4];
        int[] initPotValues = {500000,5000000,25000000,50000000};
        long[] fundsJackPot = new long[4];
        long[] fundMiniGame = new long[4];
        long[] timeJackPot = new long[4];
        try {
            this.pots = this.service.getPots(Games.RANGE_ROVER.getName());
            Debug.trace(this.gameName+" POTS: " + CommonUtils.arrayLongToString(this.pots));
            funds = this.service.getFunds(Games.RANGE_ROVER.getName());
            Debug.trace(this.gameName+" FUNDS: " + CommonUtils.arrayLongToString(funds));

            fundsJackPot = this.service.getFunds(slot7IconWildJackpotFund);
            Debug.trace("Slot7IconWild FUNDS JACKPOT: " + CommonUtils.arrayLongToString(fundsJackPot));

            fundMiniGame = this.service.getFunds(slot7IconWildMinigameFund);
            Debug.trace("Slot7IconWild FUNDS Minigame: " + CommonUtils.arrayLongToString(fundMiniGame));

            timeJackPot = this.service.getFunds(keyBotJackpotSlot7IconWild);
            Debug.trace("Slot7IconWild TIME JACKPOT: " + CommonUtils.arrayLongToString(timeJackPot));
        }
        catch (Exception e) {
            Debug.trace("Init range rover error ", e);
        }
        this.rooms.put(this.gameName + "_vin_100", new RangeRoverRoom(this, (byte)0, this.gameName + "_vin_100", (short)1,
                this.pots[0], funds[0], 100, initPotValues[0],
                fundsJackPot[0],slot7IconWildJackpotFund+ "_vin_100",
                fundMiniGame[0],slot7IconWildMinigameFund + "_vin_100"));
        this.rooms.put(this.gameName + "_vin_1000", new RangeRoverRoom(this, (byte)1, this.gameName + "_vin_1000", (short)1,
                this.pots[1], funds[1], 1000, initPotValues[1],
                fundsJackPot[1],slot7IconWildJackpotFund+ "_vin_1000",
                fundMiniGame[1],slot7IconWildMinigameFund + "_vin_1000"));
//        this.rooms.put(this.gameName + "_vin_5000", new RangeRoverRoom(this, (byte)2, this.gameName + "_vin_5000", (short) 1,
//                this.pots[2], funds[2], 5000, initPotValues[2],
//                fundsJackPot[2],slot7IconWildJackpotFund+ "_vin_5000",
//                fundMiniGame[2],slot7IconWildMinigameFund + "_vin_5000"));
        this.rooms.put(this.gameName + "_vin_10000", new RangeRoverRoom(this, (byte)3, this.gameName + "_vin_10000", (short)1,
                this.pots[3], funds[3], 10000, initPotValues[3],
                fundsJackPot[3],slot7IconWildJackpotFund+ "_vin_10000",
                fundMiniGame[3],slot7IconWildMinigameFund + "_vin_10000"));

        Debug.trace("INIT "+this.gameName+" DONE");
        this.getParentExtension().addEventListener(BZEventType.USER_DISCONNECT, this);
        referenceId = this.slotService.getLastReferenceId(this.gameName);
        Debug.trace("START "+this.gameName+" REFERENCE ID= " + referenceId);
        CacheServiceImpl sv = new CacheServiceImpl();
        try {
            sv.removeKey(this.gameName + "_last_day_x2");
        }
        catch (KeyNotFoundException e2) {
            Debug.trace((Object)"KEY NOT FOUND");
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
        BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate((Runnable)this.gameLoopTask, 10, 1, TimeUnit.SECONDS);
        BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(new BotJackPotRangeRoverTimer(timeJackPot,this), 10, 5, TimeUnit.SECONDS);

//        RangeRoverRoom room = (RangeRoverRoom)this.rooms.get(String.valueOf(this.gameName) + "_vin_100");
//        List<String> bots = BotMinigame.getBots(10, "vin");
//        for(int i = 0;i<10000;i++){
//            room.play(bots.get(0), this.fullLines);
//        }
        _instance = this;
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
            UpdatePotRangeRoverMsg msg = this.getPotsInfo();
            this.lastTimeUpdatePotToRoom = System.currentTimeMillis();
            SendMsgToAlLUsersThread t = new SendMsgToAlLUsersThread(msg);
//            SendMsgToAlLUsersThread t = new SendMsgToAlLUsersThread(this, msg);
            t.start();
        }
    }

    public UpdatePotRangeRoverMsg getPotsInfo() {
        UpdatePotRangeRoverMsg msg = new UpdatePotRangeRoverMsg();
        msg.value100 = this.pots[0];
        msg.value1000 = this.pots[1];
        msg.value5000 = this.pots[2];
        msg.value10000 = this.pots[3];
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
        RangeRoverRoom room = (RangeRoverRoom)user.getProperty((Object)"MGROOM_"+this.gameName+"_INFO");
        if (room != null) {
            room.quitRoom(user);
            room.stopAutoPlay(user);
        }
    }

    @Override
    public void handleClientRequest(User user, DataCmd dataCmd) {
        Debug.trace((Object)("audition handleClientRequest " + dataCmd.getId()));

        switch (dataCmd.getId()) {
            case 13003: {
                this.subScribe(user, dataCmd);
                break;
            }
            case 13004: {
                this.unSubScribe(user, dataCmd);
                break;
            }
            case 13005: {
                this.changeRoom(user, dataCmd);
                break;
            }
            case 13006: {
                this.autoPlay(user, dataCmd);
                break;
            }
            case 13001: {
                this.playRangeRover(user, dataCmd);
                break;
            }
            case 13013: {
                this.minimize(user, dataCmd);
            }
        }
    }

    public void updatePotToUser(User user) {
        UpdatePotRangeRoverMsg msg = this.getPotsInfo();
        SlotUtils.sendMessageToUser((BaseMsg)msg, user);
    }

    protected void subScribe(User user, DataCmd dataCmd) {
        SubscribeRangeRoverCmd cmd = new SubscribeRangeRoverCmd(dataCmd);
        RangeRoverRoom room = (RangeRoverRoom)this.getRoom(cmd.roomId);
        if (room != null) {
            room.joinRoom(user);
            room.userMaximize(user);
            this.updatePotToUser(user);
            RangeRoverInfoMsg msg = new RangeRoverInfoMsg();
            msg.ngayX2 = this.ngayX2;
            msg.remain = 0;
            msg.currentMoney = this.userService.getMoneyUserCache(user.getName(), "vin");
            this.send((BaseMsg)msg, user);
        } else {
            Debug.trace((Object)(this.gameName+" SUBSCRIBE: room " + cmd.roomId + " not found"));
        }
    }

    protected void unSubScribe(User user, DataCmd dataCmd) {
        UnSubscribeRangeRoverCmd cmd = new UnSubscribeRangeRoverCmd(dataCmd);
        RangeRoverRoom room = (RangeRoverRoom)this.getRoom(cmd.roomId);
        if (room != null) {
            room.stopAutoPlay(user);
            room.quitRoom(user);
        } else {
            Debug.trace((Object)(this.gameName+" UNSUBSCRIBE: room " + cmd.roomId + " not found"));
        }
    }

    protected void minimize(User user, DataCmd dataCmd) {
        MinimizeRangeRoverCmd cmd = new MinimizeRangeRoverCmd(dataCmd);
        RangeRoverRoom room = (RangeRoverRoom)this.getRoom(cmd.roomId);
        if (room != null) {
            room.quitRoom(user);
            room.userMinimize(user);
        } else {
            Debug.trace((Object)(this.gameName+" MINIMIZE: room " + cmd.roomId + " not found"));
        }
    }

    protected void changeRoom(User user, DataCmd dataCmd) {
        ChangeRoomRangeRoverCmd cmd = new ChangeRoomRangeRoverCmd(dataCmd);
        RangeRoverRoom roomLeaved = (RangeRoverRoom)this.getRoom(cmd.roomLeavedId);
        RangeRoverRoom roomJoined = (RangeRoverRoom)this.getRoom(cmd.roomJoinedId);
        if (roomLeaved != null && roomJoined != null) {
            roomLeaved.stopAutoPlay(user);
            roomLeaved.quitRoom(user);
            roomJoined.joinRoom(user);
            this.updatePotToUser(user);
        } else {
            Debug.trace((Object)(this.gameName+": change room error, leaved= " + cmd.roomLeavedId + ", joined= " + cmd.roomJoinedId));
        }
    }

    private void playRangeRover(User user, DataCmd dataCmd) {
        PlayRangeRoverCmd cmd = new PlayRangeRoverCmd(dataCmd);
        RangeRoverRoom room = (RangeRoverRoom)user.getProperty((Object)"MGROOM_"+this.gameName+"_INFO");
        if (room != null) {
            room.play(user, cmd.lines);
        }
    }

    private void autoPlay(User user, DataCmd dataCMD) {
        AutoPlayRangeRoverCmd cmd = new AutoPlayRangeRoverCmd(dataCMD);
        RangeRoverRoom room = (RangeRoverRoom)user.getProperty((Object)"MGROOM_"+this.gameName+"_INFO");
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
        return String.valueOf(Games.RANGE_ROVER.getName()) + "_" + moneyTypeStr + "_" + baseBetting;
    }

    @Override
    protected void gameLoop() {
        List<String> bots;
        RangeRoverRoom room;
        ++this.countBot100;
        if (this.countBot100 >= this.getCountTimeBot(this.gameName+"_bot_100")) {
            if (this.countBot100 == this.getCountTimeBot(this.gameName+"_bot_100")) {
                bots = BotMinigame.getBots(ConfigGame.getIntValue(this.gameName+"_num_bot_100"), "vin");
                for (String bot : bots) {
                    if (bot == null) continue;
                    room = (RangeRoverRoom)this.rooms.get(String.valueOf(Games.RANGE_ROVER.getName()) + "_vin_100");
                    room.play(bot, this.fullLines);
                }
            }
            this.countBot100 = 0;
        }
        ++this.countBot1000;
        if (this.countBot1000 >= this.getCountTimeBot(this.gameName+"_bot_1000")) {
            if (this.countBot1000 == this.getCountTimeBot(this.gameName+"_bot_1000")) {
                bots = BotMinigame.getBots(ConfigGame.getIntValue(this.gameName+"_num_bot_1000"), "vin");
                for (String bot : bots) {
                    if (bot == null) continue;
                    room = (RangeRoverRoom)this.rooms.get(String.valueOf(Games.RANGE_ROVER.getName()) + "_vin_1000");
                    room.play(bot, this.fullLines);
                }
            }
            this.countBot1000 = 0;
        }

        ++this.countBot5000;
        if (this.countBot5000 >= this.getCountTimeBot(this.gameName+"_bot_5000")) {
            if (this.countBot5000 == this.getCountTimeBot(this.gameName+"_bot_5000")) {
                bots = BotMinigame.getBots(ConfigGame.getIntValue(this.gameName+"_num_bot_5000"), "vin");
                for (String bot : bots) {
                    if (bot == null) continue;
                    room = (RangeRoverRoom)this.rooms.get(String.valueOf(Games.RANGE_ROVER.getName()) + "_vin_5000");
                    room.play(bot, this.fullLines);
                }
            }
            this.countBot5000 = 0;
        }

        ++this.countBot10000;
        if (this.countBot10000 >= this.getCountTimeBot(this.gameName+"_bot_10000")) {
            if (this.countBot10000 == this.getCountTimeBot(this.gameName+"_bot_10000")) {
                bots = BotMinigame.getBots(ConfigGame.getIntValue(this.gameName+"_num_bot_10000"), "vin");
                for (String bot : bots) {
                    if (bot == null) continue;
                    room = (RangeRoverRoom)this.rooms.get(String.valueOf(Games.RANGE_ROVER.getName()) + "_vin_10000");
                    room.play(bot, this.fullLines);
                }
            }
            this.countBot10000 = 0;
        }
    }
}

