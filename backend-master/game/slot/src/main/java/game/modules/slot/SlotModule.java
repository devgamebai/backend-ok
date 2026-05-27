package game.modules.slot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vinplay.dal.service.MiniGameService;
import com.vinplay.dal.service.SlotMachineService;
import com.vinplay.dal.service.impl.MiniGameServiceImpl;
import com.vinplay.dal.service.impl.SlotMachineServiceImpl;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;

import bitzero.server.entities.User;
import bitzero.server.extensions.BaseClientRequestHandler;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.extensions.data.DataCmd;
import bitzero.util.ExtensionUtility;
import game.modules.slot.room.SlotRoom;
import game.util.ConfigGame;

public abstract class SlotModule
extends BaseClientRequestHandler {
    public Map<String, SlotRoom> rooms = new HashMap<String, SlotRoom>();
    protected MiniGameService service = new MiniGameServiceImpl();
    protected SlotMachineService slotService = new SlotMachineServiceImpl();
    protected UserService userService = new UserServiceImpl();
    protected boolean eventX2 = false;
    protected String ngayX2 = "";
    protected long[] pots = new long[3];
    protected long lastTimeUpdatePotToRoom = 0L;
    protected int countBot100;
    protected int countBot1000;
    protected int countBot5000;
    protected int countBot10000;
    protected GameLoopTask gameLoopTask = new GameLoopTask();
    public String gameName;
    protected X2Task x2Task = new X2Task();

    public void handleClientRequest(User user, DataCmd dataCmd) {
    }

    protected abstract String getRoomName(short var1, long var2);

    protected abstract void gameLoop();

    public abstract long getNewReferenceId();

    public void startX2() {
        this.eventX2 = true;
        this.ngayX2 = "";
    }

    public void stopX2() {
        /*this.eventX2 = false;
        this.ngayX2 = SlotUtils.calculateTimePokeGoX2AsString(this.gameName, SlotUtils.getX2Days(this.gameName), SlotUtils.getLastDayX2(this.gameName));
        PokeGoX2Msg msg = new PokeGoX2Msg();
        msg.ngayX2 = this.ngayX2;
        for (SlotRoom room : this.rooms.values()) {
            room.sendMessageToRoom(msg);
        }
        Calendar cal = Calendar.getInstance();
        int today = cal.get(7);
        SlotUtils.saveLastDayX2(this.gameName, today);
        int lastDayX2 = SlotUtils.getLastDayX2(this.gameName);
        int nextX2Time = SlotUtils.calculateTimePokeGoX2(this.gameName, SlotUtils.getX2Days(this.gameName), lastDayX2);
        BitZeroServer.getInstance().getTaskScheduler().schedule((Runnable)this.x2Task, nextX2Time, TimeUnit.SECONDS);*/
    }

    protected int getCountTimeBot(String name) {
        int n = ConfigGame.getIntValue(name, 0);
        if (n == 0) {
            return 0;
        }
//        if (BotMinigame.isNight()) {
//            n *= 3;
//        }
        return n;
    }

    protected SlotRoom getRoom(byte roomId) {
        short moneyType = this.getMoneyTypeFromRoomId(roomId);
        long baseBetting = this.getBaseBetting(roomId);
        String roomName = this.getRoomName(moneyType, baseBetting);
        SlotRoom room = this.rooms.get(roomName);
        return room;
    }

    protected short getMoneyTypeFromRoomId(byte roomId) {
        if(roomId >= 0 && roomId < 4)
            return 1;
        return 0;
    }

    protected long getBaseBetting(byte roomId) {
        switch (roomId) {
            case 0: {
                return 100L;
            }
            case 1: {
                return 1000L;
            }
            case 2: {
                return 5000L;
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

    protected void sendMessageToTS(BaseMsg msg) {
//        for (SlotRoom room : this.rooms.values()) {
//            room.sendMessageToRoom(msg);
//        }
        List users = ExtensionUtility.globalUserManager.getAllUsers();
        if (users != null) {
        	this.send(msg, users);
        }
        
    }

    public void sendMsgToAllUsers(BaseMsg msg) {
        SendMsgToAlLUsersThread t = new SendMsgToAlLUsersThread(msg);
        t.start();
    }

    protected final class X2Task
    implements Runnable {
        protected X2Task() {
        }

        @Override
        public void run() {
            SlotModule.this.startX2();
            SlotRoom room100 = SlotModule.this.rooms.get(String.valueOf(SlotModule.this.gameName) + "_vin_100");
            room100.startHuX2();
            SlotRoom room101 = SlotModule.this.rooms.get(String.valueOf(SlotModule.this.gameName) + "_vin_1000");
            room101.startHuX2();
        }
    }

    protected final class SendMsgToAlLUsersThread
    extends Thread {
        private BaseMsg msg;

        protected SendMsgToAlLUsersThread(BaseMsg msg) {
            this.msg = msg;
        }

        @Override
        public void run() {
            SlotModule.this.sendMessageToTS(this.msg);
        }
    }

    protected final class GameLoopTask
    implements Runnable {
        protected GameLoopTask() {
        }

        @Override
        public void run() {
            SlotModule.this.gameLoop();
        }
    }

}

