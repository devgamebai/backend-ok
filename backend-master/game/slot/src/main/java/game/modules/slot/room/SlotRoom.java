/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.util.ExtensionUtility
 *  bitzero.util.common.business.Debug
 *  com.vinplay.dal.service.BroadcastMessageService
 *  com.vinplay.dal.service.CacheService
 *  com.vinplay.dal.service.MiniGameService
 *  com.vinplay.dal.service.SlotMachineService
 *  com.vinplay.dal.service.impl.BroadcastMessageServiceImpl
 *  com.vinplay.dal.service.impl.CacheServiceImpl
 *  com.vinplay.dal.service.impl.MiniGameServiceImpl
 *  com.vinplay.dal.service.impl.SlotMachineServiceImpl
 *  com.vinplay.usercore.service.UserService
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 */
package game.modules.slot.room;

import bitzero.server.entities.User;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.util.ExtensionUtility;
import bitzero.util.common.business.Debug;
import com.vinplay.dal.service.BroadcastMessageService;
import com.vinplay.dal.service.CacheService;
import com.vinplay.dal.service.MiniGameService;
import com.vinplay.dal.service.SlotMachineService;
import com.vinplay.dal.service.impl.BroadcastMessageServiceImpl;
import com.vinplay.dal.service.impl.CacheServiceImpl;
import com.vinplay.dal.service.impl.MiniGameServiceImpl;
import com.vinplay.dal.service.impl.SlotMachineServiceImpl;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import game.GameConfig.GameConfig;
import game.modules.slot.SlotModule;
import game.modules.slot.entities.slot.AutoUser;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class SlotRoom {
    protected SlotModule module;
    public static final String MGROOM_KHO_BAU_INFO = "MGROOM_KHO_BAU_INFO";
    public static final String MGROOM_NU_DIEP_VIEN_INFO = "MGROOM_NU_DIEP_VIEN_INFO";

    public static final String CACHE_NAME_USER_SPOT = "user_force_jackpot_";
    protected byte id;
    protected String gameName;
    protected String gameID;
    protected String cacheFreeName;
    protected String name;
    protected List<User> users = new ArrayList<User>();
    public long pot;
    public long fund;
    protected long initPotValue;
    protected int betValue;
    protected short moneyType;
    protected String moneyTypeStr;
    protected int countHu = -1;
    protected int countNoHuX2 = 0;
    protected boolean huX2 = false;
    protected UserService userService = new UserServiceImpl();
    protected SlotMachineService slotService = new SlotMachineServiceImpl();
    protected BroadcastMessageService broadcastMsgService = new BroadcastMessageServiceImpl();
    protected MiniGameService mgService = new MiniGameServiceImpl();
    protected CacheService sv = new CacheServiceImpl();
    protected Map<String, AutoUser> usersAuto = new HashMap<String, AutoUser>();

    public long fundJackPot;
    public String fundJackPotName;

    public long fundMinigame;
    public String fundMinigameName;

    public SlotRoom(byte id, String name, int betValue, short moneyType, long pot, long fund, long initPotValue) {
        this.id = id;
        this.name = name;
        this.betValue = betValue;
        this.moneyType = moneyType;
        this.pot = pot;
        this.fund = fund;
        this.initPotValue = initPotValue;
        this.moneyTypeStr = this.moneyType == 1 ? "vin" : "xu";
        try {
            this.countHu = this.sv.getValueInt(String.valueOf(name) + "_count_hu");
            this.countNoHuX2 = this.sv.getValueInt(String.valueOf(name) + "_count_no_hu_x2");
            this.calculatHuX2();
        }
        catch (KeyNotFoundException keyNotFoundException) {
            // empty catch block
        }
        try {
            this.mgService.savePot(name, pot, this.isMultiJackpot());
        }
        catch (InterruptedException interruptedException) {
        }
        catch (TimeoutException timeoutException) {
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    public boolean isUserBigWin(String userName) {
        return this.userService.isUserBigWin(userName);
    }

    public boolean isUserJackpot(String userName){
        return this.slotService.isSetJackpotForUser(this.gameName,userName,this.betValue);
    }

    public boolean isMultiJackpot(){
        return false; // stubbed - SlotMultiJackpotConfig.isMultiJackpot not in runtime
    }

    public void updatePot(User user) {
        // default no-op; subclasses override to send pot update messages
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean joinRoom(User user) {
        List<User> list = this.users;
        synchronized (list) {
            if (!this.users.contains(user)) {
                this.users.add(user);
                return true;
            }
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean quitRoom(User user) {
        List<User> list = this.users;
        synchronized (list) {
            if (this.users.contains(user)) {
                this.users.remove(user);
                return true;
            }
        }
        return false;
    }

    public void sendMessageToRoom(BaseMsg msg) {
        ArrayList<User> usersCopy = new ArrayList<User>(this.users);
        for (User user : usersCopy) {
            ExtensionUtility.getExtension().send(msg, user);
        }
    }

    public void startHuX2() {
//        this.huX2 = false;
        /*
        Debug.trace((Object)(String.valueOf(this.gameName) + " start hu X2"));
        this.countHu = 1;
        this.sv.setValue(String.valueOf(this.name) + "_count_hu", this.countHu);
        if (this.moneyType == 1 && this.betValue == 100) {
            this.huX2 = true;
        }*/
    }

    public void stopHuX2() {
//        Debug.trace((Object)(String.valueOf(this.gameName) + " stop hu x2"));
//        this.countHu = -1;
//        this.countNoHuX2 = 0;
//        this.huX2 = false;
//        this.sv.setValue(String.valueOf(this.name) + "_count_hu", this.countHu);
//        this.sv.setValue(String.valueOf(this.name) + "_count_no_hu_x2", this.countNoHuX2);
    }

    public void noHuX2() {

        /*
        if (this.countHu > -1) {
            ++this.countHu;
            this.sv.setValue(String.valueOf(this.name) + "_count_hu", this.countHu);
            if (this.huX2) {
                ++this.countNoHuX2;
                this.sv.setValue(String.valueOf(this.name) + "_count_no_hu_x2", this.countNoHuX2);
                Debug.trace((Object)(String.valueOf(this.gameName) + " No hu X2: " + this.countHu + " , huX2= " + this.countNoHuX2));
                if (this.betValue == 100 && this.countNoHuX2 >= 10) {
                    this.module.stopX2();
                    this.stopHuX2();
                }
                if (this.betValue == 1000 && this.countNoHuX2 >= 1) {
                    this.module.stopX2();
                    this.stopHuX2();
                }
            }
            this.calculatHuX2();
        }*/
    }

    private void calculatHuX2() {
        /*
        if (this.countHu > -1 && this.moneyType == 1) {
            if (this.betValue == 100) {
                this.huX2 = this.countHu % 4 == 1 && this.countNoHuX2 < 10;
            } else if (this.betValue == 1000) {
                this.huX2 = this.countHu == 3 && this.countNoHuX2 < 1;
            }
        }*/
    }

    public int getBetValue() {
        return this.betValue;
    }

    public void forceStopAutoPlay(User user) {
        user.removeProperty(("auto_" + this.gameName));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void autoPlay(User user, String lines, short resultFirstPlay) {
        Map<String, AutoUser> map = this.usersAuto;
        synchronized (map) {
            if (this.usersAuto.containsKey(user.getName())) {
                AutoUser entry = this.usersAuto.get(user.getName());
                this.forceStopAutoPlay(entry.getUser());
            }
            AutoUser autoUser = new AutoUser(user, lines);
            if (resultFirstPlay == 0) {
                autoUser.setMaxCount(5);
            } else if (resultFirstPlay == 5) {
                autoUser.setMaxCount(20);
            } else {
                autoUser.setMaxCount(8);
            }
            this.usersAuto.put(user.getName(), autoUser);
            user.setProperty(("auto_" + this.gameName), true);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void stopAutoPlay(User user) {
        Map<String, AutoUser> map = this.usersAuto;
        synchronized (map) {
            AutoUser entry;
            if (this.usersAuto.containsKey(user.getName()) && (entry = this.usersAuto.get(user.getName())).getUser().getUniqueId() == user.getUniqueId()) {
                this.usersAuto.remove(user.getName());
                user.removeProperty(("auto_" + this.gameName));
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void userMinimize(User user) {
        Map<String, AutoUser> map = this.usersAuto;
        synchronized (map) {
            AutoUser entry;
            if (this.usersAuto.containsKey(user.getName()) && (entry = this.usersAuto.get(user.getName())).getUser().getUniqueId() == user.getUniqueId()) {
                entry.setMinimize(true);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void userMaximize(User user) {
        Map<String, AutoUser> map = this.usersAuto;
        synchronized (map) {
            AutoUser entry;
            if (this.usersAuto.containsKey(user.getName()) && (entry = this.usersAuto.get(user.getName())).getUser().getUniqueId() == user.getUniqueId()) {
                entry.setMinimize(false);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean isUserMinimize(User user) {
        Map<String, AutoUser> map = this.usersAuto;
        synchronized (map) {
            AutoUser entry;
            if (this.usersAuto.containsKey(user.getName()) && (entry = this.usersAuto.get(user.getName())).getUser().getUniqueId() == user.getUniqueId()) {
                return entry.isMinimize();
            }
        }
        return false;
    }

    public void speedUp(User user, byte autoPlay) {
        synchronized (this.usersAuto) {
            AutoUser entry;
            if (this.usersAuto.containsKey(user.getName()) && (entry = this.usersAuto.get(user.getName())).getUser().getUniqueId() == user.getUniqueId()) {
                entry.setSpeedUp(autoPlay == 1);
            }
        }
    }

    protected abstract void gameLoop();

    protected abstract void checkResetPot();



    protected abstract void playListAuto(List<AutoUser> var1);

    protected String buildDescription(long totalBet, long totalPrizes, short result) {
        if (totalBet == 0L) {
            return String.valueOf(this.resultToString(result)) + ": " + totalPrizes;
        }
        return "Quay: " + (totalBet == 0L ? "free" : Long.valueOf(totalBet)) + ", " + this.resultToString(result) + ": " + totalPrizes;
    }

    protected String resultToString(short result) {
        switch (result) {
            case 3: {
                return "N\u1ed5 h\u0169";
            }
            case 4: {
                return "N\u1ed5 h\u0169 X2";
            }
            case 1:
            case 5: {
                return "Th\u1eafng";
            }
            case 2: {
                return "Th\u1eafng l\u1edbn";
            }
        }
        return "Tr\u01b0\u1ee3t";
    }

    public byte getId() {
        return this.id;
    }

    public class ResultSlot {
        public static final short LOI_HE_THONG = 100;
        public static final short DAT_CUOC_KHONG_HOP_LE = 101;
        public static final short KHONG_DU_TIEN = 102;
        public static final short LUOT_QUAY_FREE_KHONG_HOP_LE = 103;
        public static final short TRUOT = 0;
        public static final short THANG = 1;
        public static final short THANG_LON = 2;
        public static final short NO_HU = 3;
        public static final short NO_HU_X2 = 4;
        public static final short MINIGAME_SLOT = 5;
    }

    protected final class GameLoopTask
    implements Runnable {
        protected GameLoopTask() {
        }

        @Override
        public void run() {
            try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
                SlotRoom.this.gameLoop();
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    protected final class CheckResetPot implements Runnable {

        protected CheckResetPot(){

        }

        @Override
        public void run() {
            try{
                SlotRoom.this.checkResetPot();
            }catch (Throwable e){
                e.printStackTrace();
            }
        }
    }

    protected final class PlayListAutoUserTask
    extends Thread {
        private List<AutoUser> users;

        protected PlayListAutoUserTask(List<AutoUser> users) {
            this.users = users;
            this.setName(String.valueOf(SlotRoom.this.gameName) + "_" + SlotRoom.this.betValue + "_AutoPlayTask");
        }

        @Override
        public void run() {
            SlotRoom.this.playListAuto(this.users);
        }
    }

    /**
     * Cleanup method called when room is being destroyed or recycled
     * Override this method in subclasses to cleanup resources like thread pools
     */
    public void cleanup() {
        // Override in subclasses to cleanup resources
    }

}

