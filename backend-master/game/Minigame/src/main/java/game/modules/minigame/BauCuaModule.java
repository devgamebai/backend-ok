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
 *  bitzero.server.entities.managers.IUserManager
 *  bitzero.server.exceptions.BZException
 *  bitzero.server.extensions.BZExtension
 *  bitzero.server.extensions.BaseClientRequestHandler
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.server.extensions.data.DataCmd
 *  bitzero.server.util.TaskScheduler
 *  bitzero.util.ExtensionUtility
 *  bitzero.util.common.business.Debug
 *  com.vinplay.dal.service.BauCuaService
 *  com.vinplay.dal.service.BroadcastMessageService
 *  com.vinplay.dal.service.MiniGameService
 *  com.vinplay.dal.service.impl.BauCuaServiceImpl
 *  com.vinplay.dal.service.impl.BroadcastMessageServiceImpl
 *  com.vinplay.dal.service.impl.MiniGameServiceImpl
 *  com.vinplay.vbee.common.utils.CommonUtils
 *  com.vinplay.vbee.common.utils.DateTimeUtils
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
import bitzero.server.entities.managers.IUserManager;
import bitzero.server.exceptions.BZException;
import bitzero.server.extensions.BZExtension;
import bitzero.server.extensions.BaseClientRequestHandler;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.extensions.data.DataCmd;
import bitzero.server.util.TaskScheduler;
import bitzero.util.ExtensionUtility;
import bitzero.util.common.business.Debug;
import com.vinplay.dal.service.BauCuaService;
import com.vinplay.dal.service.BroadcastMessageService;
import com.vinplay.dal.service.MiniGameService;
import com.vinplay.dal.service.impl.BauCuaServiceImpl;
import com.vinplay.dal.service.impl.BroadcastMessageServiceImpl;
import com.vinplay.dal.service.impl.MiniGameServiceImpl;
import com.vinplay.vbee.common.utils.CommonUtils;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import game.modules.lobby.cmd.send.BroadcastMessageMsg;
import game.modules.minigame.cmd.rev.baucua.BetBauCuaCmd;
import game.modules.minigame.cmd.rev.baucua.ChangeRoomBauCuaCmd;
import game.modules.minigame.cmd.rev.baucua.SubscribeBauCuaCmd;
import game.modules.minigame.cmd.rev.baucua.UnsubscribeBauCuaCmd;
import game.modules.minigame.cmd.send.baucua.StartNewGameBauCuaMsg;
import game.modules.minigame.room.MGRoom;
import game.modules.minigame.room.MGRoomBauCua;
import game.modules.minigame.utils.BauCuaUtils;
import game.modules.minigame.utils.MiniGameUtils;
import game.utils.GameUtils;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class BauCuaModule
extends BaseClientRequestHandler {
    public Map<String, MGRoom> rooms = new HashMap<String, MGRoom>();
    private static BauCuaModule _instance;

    public static BauCuaModule getInstance() {
        return _instance;
    }
    private long referenceId;
    private boolean isBettingRound;
    private byte count = 0;
    private long[] funds = new long[6];
    private boolean serverReady = false;
    private BauCuaService bcService = new BauCuaServiceImpl();
    private MiniGameService mgService = new MiniGameServiceImpl();
    private final Runnable gameLoopTask = new GameLoopTask();
    private final Runnable serverReadyTask = new ServerReadyTask();
//    private final Runnable rewardToiChonCaTask = new RewardToiChonCaTask();
    private BroadcastMessageService broadcastMsg = new BroadcastMessageServiceImpl();

    public void init() {
        super.init();
        this.loadData();
        this.rooms.put("BauCua_vin_1000", new MGRoomBauCua("BauCua_vin_1000", 1000, (byte)1, (byte)0, this.funds[0]));
        this.rooms.put("BauCua_vin_10000", new MGRoomBauCua("BauCua_vin_10000", 10000, (byte)1, (byte)1, this.funds[1]));
        this.rooms.put("BauCua_vin_100000", new MGRoomBauCua("BauCua_vin_100000", 100000, (byte)1, (byte)2, this.funds[2]));
        this.rooms.put("BauCua_xu_10000", new MGRoomBauCua("BauCua_xu_10000", 10000, (byte)0, (byte)3, this.funds[3]));
        this.rooms.put("BauCua_xu_100000", new MGRoomBauCua("BauCua_xu_100000", 100000, (byte)0, (byte)4, this.funds[4]));
        this.rooms.put("BauCua_xu_1000000", new MGRoomBauCua("BauCua_xu_1000000", 1000000, (byte)0, (byte)5, this.funds[5]));
        BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.gameLoopTask, 10, 1, TimeUnit.SECONDS);
        BitZeroServer.getInstance().getTaskScheduler().schedule(this.serverReadyTask, 10, TimeUnit.SECONDS);
        this.getParentExtension().addEventListener((IBZEventType)BZEventType.USER_DISCONNECT, (IBZEventListener)this);
        _instance = this;
//        try {
//            int remainTimeRewardToiChonCa = MiniGameUtils.calculateTimeRewardOnNextDay("");
//            BitZeroServer.getInstance().getTaskScheduler().schedule(this.rewardToiChonCaTask, remainTimeRewardToiChonCa, TimeUnit.SECONDS);
//        }
//        catch (ParseException e) {
//            Debug.trace((Object[])new Object[]{"Calculate time reward Toi chon ca error ", e.getMessage()});
//        }
        String msg = "Start MiniGame " + DateTimeUtils.getCurrentTime((String)"HH-mm-ss yyyy-MM-dd");
        GameUtils.sendAlert(msg);
    }

    private void loadData() {
        try {
            this.referenceId = this.mgService.getReferenceId(3);
            this.funds = this.mgService.getFunds("BauCua");
        }
        catch (SQLException e) {
            Debug.trace(("LOAD DATA BAU CUA ERROR: " + e.getMessage()));
        }
        Debug.trace(("BAU CUA referenceId: " + this.referenceId));
        Debug.trace(("BAU CUA FUND: " + CommonUtils.arrayLongToString((long[])this.funds)));
    }

    public void handleServerEvent(IBZEvent ibzevent) throws BZException {
        if (ibzevent.getType() == BZEventType.USER_DISCONNECT) {
            User user = (User)ibzevent.getParameter((IBZEventParam)BZEventParam.USER);
            this.userDis(user);
        }
    }

    private void userDis(User user) {
        MGRoomBauCua room = (MGRoomBauCua)user.getProperty("MGROOM_BAU_CUA_INFO");
        if (room != null) {
            room.quitRoom(user);
        }
    }

    public void handleClientRequest(User user, DataCmd dataCmd) {
        if (!this.serverReady) {
            Debug.trace("Server bau cua not ready, try again!");
            return;
        }
        switch (dataCmd.getId()) {
            case 5001: {
                this.subscribeBauCua(user, dataCmd);
                break;
            }
            case 5002: {
                this.unsubscribeBauCua(user, dataCmd);
                break;
            }
            case 5003: {
                this.changeRoomBauCua(user, dataCmd);
                break;
            }
            case 5004: {
                if (GameUtils.disablePlayMiniGame(user)) {
                    return;
                }
                this.betBauCua(user, dataCmd);
            }
        }
    }

    private synchronized void gameLoop() {
        this.count = (byte)(this.count + 1);
        for (MGRoom entry : this.rooms.values()) {
            MGRoomBauCua room = (MGRoomBauCua)entry;
            room.updateBauCuaPerSecond(this.getRemainTime(), this.isBettingRound);
            room.botBet(60 - this.count, this.isBettingRound);
        }
        switch (this.count) {
            case 55: {
                break;
            }
            case 60: {
                this.isBettingRound = false;
                break;
            }
            case 61: {
                this.generateResult();
                break;
            }
            case 65: {
                CalculatePrizeTask task = new CalculatePrizeTask();
                task.run();
                break;
            }
            case 90: {
                this.startNewRound();
                break;
            }
            case 85: {
                this.broadcastMessage();
            }
        }
    }

    private void startNewRound() {
        Debug.trace("START NEW ROUND BAU CUA");
        ++this.referenceId;
        StartNewGameBauCuaMsg msg = new StartNewGameBauCuaMsg();
        msg.referenceId = this.referenceId;
        this.sendMessageBauCuaNewThread(msg);
        for (MGRoom entry : this.rooms.values()) {
            MGRoomBauCua room = (MGRoomBauCua)entry;
            room.startNewGame(this.referenceId);
        }
        this.count = 0;
        this.isBettingRound = true;
        this.saveReferences();
    }

    private void subscribeBauCua(User user, DataCmd dataCmd) {
        SubscribeBauCuaCmd cmd = new SubscribeBauCuaCmd(dataCmd);
        MGRoomBauCua room = this.getRoom(cmd.roomId);
        if (room != null) {
            room.joinRoom(user);
            room.updateBauCuaInfoToUser(user, this.getRemainTime(), this.isBettingRound);
        }
    }

    private void unsubscribeBauCua(User user, DataCmd dataCmd) {
        UnsubscribeBauCuaCmd cmd = new UnsubscribeBauCuaCmd(dataCmd);
        MGRoomBauCua room = this.getRoom(cmd.roomId);
        if (room != null) {
            room.quitRoom(user);
        }
    }

    private void changeRoomBauCua(User user, DataCmd dataCmd) {
        ChangeRoomBauCuaCmd cmd = new ChangeRoomBauCuaCmd(dataCmd);
        MGRoomBauCua roomLeaved = this.getRoom(cmd.roomLeavedId);
        MGRoomBauCua roomJoined = this.getRoom(cmd.roomJoinedId);
        if (roomLeaved != null && roomJoined != null) {
            roomLeaved.quitRoom(user);
            roomJoined.joinRoom(user);
            roomJoined.updateBauCuaInfoToUser(user, this.getRemainTime(), this.isBettingRound);
        }
    }

    private void betBauCua(User user, DataCmd dataCmd) {
        BetBauCuaCmd cmd = new BetBauCuaCmd(dataCmd);
        MGRoomBauCua room = (MGRoomBauCua)user.getProperty("MGROOM_BAU_CUA_INFO");
        room.bet(user, cmd.betValue, this.isBettingRound);
    }

    private void generateResult() {
        for (MGRoom entry : this.rooms.values()) {
            MGRoomBauCua room = (MGRoomBauCua)entry;
            room.generateResult();
        }
    }

    private void calculateResult() {
        for (MGRoom entry : this.rooms.values()) {
            MGRoomBauCua room = (MGRoomBauCua)entry;
            room.calculatePrizes();
        }
        if (this.referenceId % 50L == 0L) {
            this.bcService.updateAllTop();
        }
    }

    private byte getRemainTime() {
        if (this.isBettingRound) {
            return (byte)(60 - this.count);
        }
        return (byte)(90 - this.count);
    }

    private String getRoomName(short moneyType, long baseBetting) {
        String moneyTypeStr = "xu";
        if (moneyType == 1) {
            moneyTypeStr = "vin";
        }
        return "BauCua_" + moneyTypeStr + "_" + baseBetting;
    }

    private MGRoomBauCua getRoom(byte roomId) {
        short moneyType = this.getMoneyTypeFromRoomId(roomId);
        long baseBetting = this.getBaseBetting(roomId);
        String roomName = this.getRoomName(moneyType, baseBetting);
        MGRoomBauCua room = (MGRoomBauCua)this.rooms.get(roomName);
        return room;
    }

    private short getMoneyTypeFromRoomId(byte roomId) {
//        return 0 <= roomId && roomId < 3;
        if ((0 <= roomId) && (roomId < 3)) {
            return 1;
        }
        return 0;
    }

    private long getBaseBetting(byte roomId) {
        switch (roomId) {
            case 0: {
                return 1000L;
            }
            case 1: {
                return 10000L;
            }
            case 2: {
                return 100000L;
            }
            case 3: {
                return 10000L;
            }
            case 4: {
                return 100000L;
            }
            case 5: {
                return 1000000L;
            }
        }
        return 0L;
    }

    private void saveReferences() {
        try {
            this.mgService.saveReferenceId(this.referenceId, 3);
        }
        catch (SQLException e) {
            Debug.trace(("Save reference error " + e.getMessage()));
        }
    }

    private void sendMessageToBauCua(BaseMsg msg) {
        for (MGRoom room : this.rooms.values()) {
            room.sendMessageToRoom(msg);
        }
    }

    private void broadcastMessage() {
        String message = this.broadcastMsg.toJson();
        BroadcastMessageMsg msg = new BroadcastMessageMsg();
        msg.message = message;
        List users = ExtensionUtility.globalUserManager.getAllUsers();
        if (users != null) {
            this.send((BaseMsg)msg, users);
        }
        this.broadcastMsg.clearMessage();
    }

    private void sendMessageBauCuaNewThread(BaseMsg msg) {
        SendMessageToTXThread t = new SendMessageToTXThread(msg);
        t.start();
    }

    private final class GameLoopTask
    implements Runnable {
        private GameLoopTask() {
        }

        @Override
        public void run() {
            try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
                BauCuaModule.this.gameLoop();
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private final class SendMessageToTXThread
    extends Thread {
        private BaseMsg msg;

        private SendMessageToTXThread(BaseMsg msg) {
            this.msg = msg;
        }

        @Override
        public void run() {
            BauCuaModule.this.sendMessageToBauCua(this.msg);
        }
    }

    private final class ServerReadyTask
    implements Runnable {
        private ServerReadyTask() {
        }

        @Override
        public void run() {
            if (!BauCuaModule.this.serverReady) {
                Debug.trace("START BAU CUA");
                BauCuaModule.this.serverReady = true;
                BauCuaModule.this.startNewRound();
            }
        }
    }

//    private final class RewardToiChonCaTask
//    implements Runnable {
//        private RewardToiChonCaTask() {
//        }
//
//        @Override
//        public void run() {
//            BauCuaUtils.rewardToiChonCa();
//            BitZeroServer.getInstance().getTaskScheduler().schedule(BauCuaModule.this.rewardToiChonCaTask, 24, TimeUnit.HOURS);
//            Debug.trace("Tra thuong Toi chon ca");
//        }
//    }

    private final class CalculatePrizeTask
    implements Runnable {
        private CalculatePrizeTask() {
        }

        @Override
        public void run() {
            BauCuaModule.this.calculateResult();
        }
    }

}

