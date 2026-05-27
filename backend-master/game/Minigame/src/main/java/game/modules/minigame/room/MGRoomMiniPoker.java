/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.BitZeroServer
 *  bitzero.server.entities.User
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.server.util.TaskScheduler
 *  bitzero.util.common.business.Debug
 *  com.vinplay.cardlib.models.Card
 *  com.vinplay.cardlib.models.GroupType
 *  com.vinplay.cardlib.utils.CardLibUtils
 *  com.vinplay.dal.service.BroadcastMessageService
 *  com.vinplay.dal.service.CacheService
 *  com.vinplay.dal.service.MiniGameService
 *  com.vinplay.dal.service.MiniPokerService
 *  com.vinplay.dal.service.impl.BroadcastMessageServiceImpl
 *  com.vinplay.dal.service.impl.CacheServiceImpl
 *  com.vinplay.dal.service.impl.MiniGameServiceImpl
 *  com.vinplay.dal.service.impl.MiniPokerServiceImpl
 *  com.vinplay.usercore.service.UserService
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.statics.TransType
 */
package game.modules.minigame.room;

import bitzero.server.BitZeroServer;
import bitzero.server.entities.User;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.util.TaskScheduler;
import bitzero.util.common.business.Debug;
import com.vinplay.cardlib.models.Card;
import com.vinplay.cardlib.models.GroupType;
import com.vinplay.cardlib.utils.CardLibUtils;
import com.vinplay.dal.service.BroadcastMessageService;
import com.vinplay.dal.service.CacheService;
import com.vinplay.dal.service.MiniGameService;
import com.vinplay.dal.service.MiniPokerService;
import com.vinplay.dal.service.impl.BroadcastMessageServiceImpl;
import com.vinplay.dal.service.impl.CacheServiceImpl;
import com.vinplay.dal.service.impl.MiniGameServiceImpl;
import com.vinplay.dal.service.impl.MiniPokerServiceImpl;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import com.vinplay.vbee.common.rtp.RtpResolver;
import game.modules.minigame.MiniPokerModule;
import game.modules.minigame.cmd.send.minipoker.ForceStopAuatoPlayMiniPokerMsg;
import game.modules.minigame.cmd.send.minipoker.ResultMiniPokerMsg;
import game.modules.minigame.cmd.send.minipoker.UpdatePotMiniPokerMsg;
import game.modules.minigame.entities.AutoUserMiniPoker;
import game.modules.minigame.entities.MinigameConstant;
import game.modules.minigame.room.MGRoom;
import game.modules.minigame.utils.GenerationMiniPoker;
import game.utils.ConfigGame;
import game.utils.GameUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MGRoomMiniPoker
extends MGRoom {
    private float tax = MinigameConstant.MINIGAME_TAX_VIN;
    public long pot;
    public long fund;
    public long fundJackPot;
    private short moneyType;
    private String moneyTypeStr;
    private long betValue = 0L;
    private long initPotValue = 500000L;
    private UserService userService = new UserServiceImpl();
    private MiniPokerService mpService = new MiniPokerServiceImpl();
    private MiniGameService mgService = new MiniGameServiceImpl();
    private BroadcastMessageService broadcastMsgService = new BroadcastMessageServiceImpl();
    private GenerationMiniPoker gen = new GenerationMiniPoker();
    private Map<String, AutoUserMiniPoker> usersAuto = new HashMap<String, AutoUserMiniPoker>();
    private final Runnable gameLoopTask = new GameLoopTask();
    private long lastTimeUpdatePotToRoom = 0L;
    private long lastTimeUpdateFundToRoom = 0L;
    private ThreadPoolExecutor executor;
    private int countHu = -1;
    private int countNoHuX2 = 0;
    private boolean huX2 = false;
    protected CacheService sv = new CacheServiceImpl();

    public MGRoomMiniPoker(String roomName, short moneyType, long pot, long fund, long baseBetValue, long initPotValue) {
        super(roomName);
        this.gameName = Games.MINI_POKER.getName();
        this.moneyType = moneyType;
        if (moneyType == 1) {
            this.moneyTypeStr = "vin";
            this.tax = MinigameConstant.MINIGAME_TAX_VIN;
        } else if (moneyType == 0) {
            this.moneyTypeStr = "xu";
            this.tax = MinigameConstant.MINIGAME_TAX_XU;
        }
        this.executor = moneyType == 1 ? (ThreadPoolExecutor)Executors.newFixedThreadPool(ConfigGame.getIntValue("mini_poker_thread_pool_per_room_vin")) : (ThreadPoolExecutor)Executors.newFixedThreadPool(ConfigGame.getIntValue("mini_poker_thread_pool_per_room_xu"));
        this.pot = pot;
        CacheServiceImpl cacheService = new CacheServiceImpl();
        cacheService.setValue(this.name, (int)pot);
        this.fund = fund;
        this.betValue = baseBetValue;
        this.initPotValue = initPotValue;
        BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.gameLoopTask, 10, 1, TimeUnit.SECONDS);
        try {
            this.countHu = this.sv.getValueInt(this.name + "_count_hu");
            this.countNoHuX2 = this.sv.getValueInt(this.name + "_count_no_hu_x2");
            this.calculatHuX2();
        }
        catch (KeyNotFoundException keyNotFoundException) {
            // empty catch block
        }
        try {
            this.mgService.savePot(this.name, pot, this.huX2);
        }
        catch (IOException | InterruptedException | TimeoutException exception) {
            // empty catch block
        }
    }

    public short play(User user) {
        return this.play(user, this.betValue);
    }

    public short play(User user, long betValue) {
        ResultMiniPokerMsg msg = this.play(user.getName(), betValue);
        this.sendMessageToUser((BaseMsg)msg, user);
        return msg.result;
    }

    public synchronized ResultMiniPokerMsg play(String username, long betValue) {
        long lastPot = this.pot;
        long lastFund = this.fund;
        ResultMiniPokerMsg resultMiniPokerMsg = new ResultMiniPokerMsg();
        StringBuilder builder = new StringBuilder();
        short result = 0;
        long prize = 0L;
        long currentMoney = this.userService.getMoneyUserCache(username, this.moneyTypeStr);
        UserCacheModel u = this.userService.getUser(username);
        long referenceId = System.currentTimeMillis();
        if (this.fund >= 0L) {
            if (betValue > 0L) {
                if (currentMoney >= betValue) {
                    MoneyResponse moneyRes = this.userService.updateMoney(username, -betValue, this.moneyTypeStr, Games.MINI_POKER.getName(), "Quay MiniPoker", "\u0110\u1eb7t c\u01b0\u1ee3c MiniPoker", 0L, Long.valueOf(referenceId), TransType.START_TRANS);
                    if (moneyRes != null && moneyRes.isSuccess()) {
                        boolean enoughToPair = false;
                        long moneyToPot = betValue * 1L / 100L;
                        long fee = (long)((float)betValue * this.tax / 100.0f);
                        long moneyToFund = betValue - moneyToPot - fee;
                        long tienThuongX2 = 0L;
                        this.pot += moneyToPot;
                        this.fund += moneyToFund;
                        block13 : while (!enoughToPair) {
                            GroupType groupType;
                            prize = 0L;
                            tienThuongX2 = 0L;
                            long moneyExchange = 0L;
                            List<Card> cards = this.gen.randomCards2();
                            if (cards.size() != 5) {
                                cards = this.gen.randomCards();
                            }
                            if ((groupType = CardLibUtils.calculateTypePoker(cards)) == null) continue;
                            switch (groupType) {
                                case HighCard: {
                                    result = 11;
                                    break;
                                }
                                case OnePair: {
                                    if (CardLibUtils.pairEqualOrGreatJack(cards)) {
                                        result = 9;
                                        prize = (int)((float)betValue * 2.5f);
                                        break;
                                    }
                                    result = 10;
                                    prize = 0L;
                                    break;
                                }
                                case TwoPair: {
                                    result = 8;
                                    prize = betValue * 5L;
                                    break;
                                }
                                case ThreeOfKind: {
                                    result = 7;
                                    prize = betValue * 8L;
                                    break;
                                }
                                case Straight: {
                                    result = 6;
                                    if (this.moneyType == 1) {
                                        prize = betValue * 13L;
                                        break;
                                    }
                                    prize = betValue * 12L;
                                    break;
                                }
                                case Flush: {
                                    result = 5;
                                    if (this.moneyType == 1) {
                                        prize = betValue * 20L;
                                        break;
                                    }
                                    prize = betValue * 18L;
                                    break;
                                }
                                case FullHouse: {
                                    result = 4;
                                    if (this.moneyType == 1) {
                                        prize = betValue * 50L;
                                        break;
                                    }
                                    prize = betValue * 40L;
                                    break;
                                }
                                case FourOfKind: {
                                    result = 3;
                                    if (this.moneyType == 1) {
                                        prize = betValue * 150L;
                                        break;
                                    }
                                    prize = betValue * 120L;
                                    break;
                                }
                                case StraightFlush: {
                                    if (!u.isBot()) continue block13;
                                    if (CardLibUtils.isStraightFlushJack(cards)) {
                                        if (!u.isBot()) continue block13;
                                        result = 1;
                                        if (this.huX2) {
                                            tienThuongX2 = this.pot;
                                            prize = this.pot * 2L;
                                            break;
                                        }
                                        prize = this.pot;
                                        break;
                                    }
                                    result = 2;
                                    prize = betValue * 1000L;
                                }
                            }
                            long fundExchange = prize > 0L ? prize : 0L;
                            long l = fundExchange;
                            if (result == 1 ? this.fund - this.initPotValue < 0L : this.fund - fundExchange < 0L) continue;
                            // [SUN-589/Phase4c] RTP Engine: if force-lose fires, re-roll unless jackpot (result==1,12)
                            if (result != 1 && result != 12 && prize > 0L) {
                                double rtpPct = RtpResolver.effectivePct(u.getId(), "minipoker");
                                if (rtpPct >= 0) { // -1.0 = game disabled, allow normal flow in that case
                                    double forceLoseRate = (100.0 - Math.max(10.0, Math.min(99.0, rtpPct))) / 100.0;
                                    if (java.util.concurrent.ThreadLocalRandom.current().nextInt(1000) < (int)(forceLoseRate * 1000)) {
                                        // Force lose: re-roll this hand
                                        continue;
                                    }
                                }
                            }
                            enoughToPair = true;
                            if (cards.size() == 5) {
                                resultMiniPokerMsg.card1 = (byte)cards.get(0).getCode();
                                resultMiniPokerMsg.card2 = (byte)cards.get(1).getCode();
                                resultMiniPokerMsg.card3 = (byte)cards.get(2).getCode();
                                resultMiniPokerMsg.card4 = (byte)cards.get(3).getCode();
                                resultMiniPokerMsg.card5 = (byte)cards.get(4).getCode();
                            }
                            if (prize > 0L) {
                                if (result == 1) {
                                    if (this.huX2) {
                                        result = 12;
                                    }
                                    this.noHuX2();
                                    if (this.moneyType == 1) {
                                        GameUtils.sendSMSToUser(username, "Chuc mung " + username + " da no hu game MiniPoker phong " + betValue + ". So tien no hu: " + this.pot + " Vin");
                                    }
                                    this.pot = this.initPotValue;
                                    this.fund -= this.initPotValue;
                                } else {
                                    this.fund -= fundExchange;
                                }
                            }
                            long moneyAdded = prize;
                            String des = "Quay MiniPoker";
                            if (result == 12) {
                                moneyAdded -= tienThuongX2;
                                this.userService.updateMoney(username, tienThuongX2, this.moneyTypeStr, this.gameName, des, "Th\u1eafng X2", 0L, null, TransType.NO_VIPPOINT);
                            }
                            moneyRes = this.userService.updateMoney(username, moneyAdded, this.moneyTypeStr, Games.MINI_POKER.getName(), des, this.buildDescription(betValue, moneyAdded, result), fee, Long.valueOf(referenceId), TransType.END_TRANS);
                            moneyExchange = prize - betValue;
                            if (moneyRes != null && moneyRes.isSuccess()) {
                                currentMoney = moneyRes.getCurrentMoney();
                                if (this.moneyType == 1 && moneyExchange >= (long)BroadcastMessageServiceImpl.MIN_MONEY) {
                                    this.broadcastMsgService.putMessage(Games.MINI_POKER.getId(), username, moneyExchange);
                                }
                            }
                            builder.append(cards.get(0).toString());
                            for (int i = 1; i < cards.size(); ++i) {
                                builder.append(",");
                                builder.append(cards.get(i).toString());
                            }
                            try {
                                this.mpService.logMiniPoker(username, betValue, result, prize, builder.toString(), lastPot, lastFund, (int)this.moneyType);
                            }
                            catch (IOException | InterruptedException | TimeoutException e) {
                                Debug.trace((Object[])new Object[]{"Log mini poker error ", e.getMessage()});
                            }
                        }
                        this.saveFund();
                        this.savePot();
                    }
                } else {
                    result = 102;
                }
            } else {
                result = 101;
            }
        } else {
            result = 100;
        }
        resultMiniPokerMsg.result = result;
        resultMiniPokerMsg.prize = prize;
        resultMiniPokerMsg.currentMoney = currentMoney;
        return resultMiniPokerMsg;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void autoPlay(User user) {
        Map<String, AutoUserMiniPoker> map;
        Map<String, AutoUserMiniPoker> map2 = map = this.usersAuto;
        synchronized (map2) {
            if (this.usersAuto.containsKey(user.getName())) {
                AutoUserMiniPoker entry = this.usersAuto.get(user.getName());
                this.forceStopAutoPlay(entry.getUser());
            }
            this.usersAuto.put(user.getName(), new AutoUserMiniPoker(user, 0));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void stopAutoPlay(User user) {
        Map<String, AutoUserMiniPoker> map;
        Map<String, AutoUserMiniPoker> map2 = map = this.usersAuto;
        synchronized (map2) {
            AutoUserMiniPoker entry;
//            if (this.usersAuto.containsKey(user.getName()) && (entry = this.usersAuto.get(user.getName())).getUser().getUniqueId() == user.getUniqueId()) {
            if (this.usersAuto.containsKey(user.getName()) && (entry = this.usersAuto.get(user.getName())).getUser().getId() == user.getId()) {
                this.usersAuto.remove(user.getName());
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void forceStopAutoPlay(User user) {
        Map<String, AutoUserMiniPoker> map;
        Map<String, AutoUserMiniPoker> map2 = map = this.usersAuto;
        synchronized (map2) {
            this.usersAuto.remove(user.getName());
            ForceStopAuatoPlayMiniPokerMsg msg = new ForceStopAuatoPlayMiniPokerMsg();
            this.sendMessageToUser((BaseMsg)msg, user);
        }
    }

    private boolean checkDienKienNoHu(String username) {
        try {
            UserModel u = this.userService.getUserByUserName(username);
            return u.isBot();
        }
        catch (Exception u) {
            return false;
        }
    }

    private void saveFund() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastTimeUpdateFundToRoom >= 60000L) {
            try {
                this.mgService.saveFund(this.name, this.fund);
            }
            catch (IOException | InterruptedException | TimeoutException e) {
                Debug.trace((Object[])new Object[]{"MINI POKER: update fund poker error ", e.getMessage()});
            }
            this.lastTimeUpdateFundToRoom = currentTime;
        }
    }

    public void resetPot() {
        this.pot = this.initPotValue;
        this.savePot();
    }

    public synchronized void addMoneyToPot(long money) {
        this.pot += money;
        this.savePot();
    }

    public void botEatJackpot(String keyBot, long nextTime, String userName) {
        long totalPrizes = this.pot;
        this.resetPot();
        try {
            this.mgService.saveFund(keyBot, nextTime);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private void savePot() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastTimeUpdatePotToRoom >= 3000L) {
            UpdatePotMiniPokerMsg msg = new UpdatePotMiniPokerMsg();
            msg.value = this.pot;
            msg.x2 = this.huX2 ? (byte)1 : 0;
            this.sendMessageToRoom(msg);
            this.lastTimeUpdatePotToRoom = currentTime;
            try {
                this.mgService.savePot(this.name, this.pot, false);
            }
            catch (IOException | InterruptedException | TimeoutException e) {
                Debug.trace((Object[])new Object[]{"MINI POKER: update pot poker error ", e.getMessage()});
            }
        }
    }

    public void updatePotToUser(User user) {
        UpdatePotMiniPokerMsg msg = new UpdatePotMiniPokerMsg();
        msg.value = this.pot;
        msg.x2 = this.huX2 ? (byte)1 : 0;
        this.sendMessageToUser((BaseMsg)msg, user);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void gameLoop() {
        Map<String, AutoUserMiniPoker> map;
        ArrayList<User> usersPlay = new ArrayList<User>();
        Map<String, AutoUserMiniPoker> map2 = map = this.usersAuto;
        synchronized (map2) {
            for (AutoUserMiniPoker user : this.usersAuto.values()) {
                boolean play = user.incCount();
                if (!play) continue;
                usersPlay.add(user.getUser());
            }
        }
        int numThreads = usersPlay.size() / 100 + 1;
        for (int i = 1; i <= numThreads; ++i) {
            int fromIndex = (i - 1) * 100;
            int toIndex = i * 100;
            if (toIndex > usersPlay.size()) {
                toIndex = usersPlay.size();
            }
            ArrayList tmp = new ArrayList(usersPlay.subList(fromIndex, toIndex));
            PlayListMiniPokerTask task = new PlayListMiniPokerTask(tmp);
            this.executor.execute(task);
        }
        usersPlay.clear();
    }

    public void playListMiniPoker(List<User> users) {
        for (User user : users) {
            short result = this.play(user, this.betValue);
            if (result != 1 && result != 2 && result != 12 && result != 102 && result != 100) continue;
            this.forceStopAutoPlay(user);
        }
        users.clear();
    }

    private String buildDescription(long totalBet, long totalPrizes, short result) {
        if (totalBet == 0L) {
            return this.resultToString(result) + ": " + totalPrizes;
        }
        return "Quay: " + totalBet + ", " + this.resultToString(result) + ": " + totalPrizes;
    }

    private String resultToString(short result) {
        switch (result) {
            case 1: {
                return "N\u1ed5 h\u0169";
            }
            case 12: {
                return "N\u1ed5 h\u0169 X2";
            }
            case 2: {
                return "Th\u00f9ng ph\u00e1 s\u1ea3nh";
            }
            case 3: {
                return "T\u1ee9 qu\u00fd";
            }
            case 4: {
                return "C\u00f9 l\u0169";
            }
            case 5: {
                return "Th\u1eafng";
            }
            case 6: {
                return "S\u1ea3nh";
            }
            case 7: {
                return "S\u1ea3nh ch\u00faa";
            }
            case 8: {
                return "Hai \u0111\u00f4i";
            }
            case 9: {
                return "L\u00e1 b\u00e0i cao";
            }
        }
        return "Tr\u01b0\u1ee3t";
    }

    @Override
    public boolean joinRoom(User user) {
        boolean result = super.joinRoom(user);
        if (result) {
            user.setProperty("MGROOM_MINI_POKER_INFO", this);
        }
        return result;
    }

    @Override
    public boolean quitRoom(User user) {
        return super.quitRoom(user);
    }

    public void startHuX2() {
        Debug.trace((this.gameName + " start hu X2"));
        this.countHu = 1;
        this.sv.setValue(this.name + "_count_hu", this.countHu);
        if (this.moneyType == 1 && this.betValue == 100L) {
            this.huX2 = true;
        }
    }

    public void stopHuX2() {
        Debug.trace((this.gameName + " stop hu x2"));
        this.countHu = -1;
        this.countNoHuX2 = 0;
        this.huX2 = false;
        this.sv.setValue(this.name + "_count_hu", this.countHu);
        this.sv.setValue(this.name + "_count_no_hu_x2", this.countNoHuX2);
    }

    public void noHuX2() {
        if (this.countHu > -1) {
            ++this.countHu;
            this.sv.setValue(this.name + "_count_hu", this.countHu);
            if (this.huX2) {
                ++this.countNoHuX2;
                this.sv.setValue(this.name + "_count_no_hu_x2", this.countNoHuX2);
                Debug.trace((this.gameName + " No hu X2: " + this.countHu + " , huX2= " + this.countNoHuX2));
                if (this.betValue == 100L && this.countNoHuX2 >= 10) {
                    MiniPokerModule.stopX2();
                    this.stopHuX2();
                }
                if (this.betValue == 1000L && this.countNoHuX2 >= 1) {
                    MiniPokerModule.stopX2();
                    this.stopHuX2();
                }
            }
            this.calculatHuX2();
        }
    }

    private void calculatHuX2() {
        if (this.countHu > -1 && this.moneyType == 1) {
            if (this.betValue == 100L) {
                this.huX2 = this.countHu % 4 == 1 && this.countNoHuX2 < 10;
            } else if (this.betValue == 1000L) {
                this.huX2 = this.countHu == 3 && this.countNoHuX2 < 1;
            }
            Debug.trace(("Count hu X2 " + this.name + ": " + this.countNoHuX2));
        }
        Debug.trace(("Count hu " + this.name + ": " + this.countHu + ", x2= " + this.huX2));
    }

    public class ResultPoker {
        public static final short LOI_HE_THONG = 100;
        public static final short DAT_CUOC_KHONG_HOP_LE = 101;
        public static final short KHONG_DU_TIEN = 102;
        public static final short TRUOT = 0;
        public static final short NO_HU = 1;
        public static final short THUNG_PHA_SANH_NHO = 2;
        public static final short TU_QUY = 3;
        public static final short CU_LU = 4;
        public static final short THUNG = 5;
        public static final short SANH = 6;
        public static final short SAM_CO = 7;
        public static final short HAI_DOI = 8;
        public static final short MOT_DOI_TO = 9;
        public static final short MOT_DOI_NHO = 10;
        public static final short BAI_CAO = 11;
        public static final short NO_HU_X2 = 12;
    }

    private final class GameLoopTask
    implements Runnable {
        private GameLoopTask() {
        }

        @Override
        public void run() {
            try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
                MGRoomMiniPoker.this.gameLoop();
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private final class PlayListMiniPokerTask
    extends Thread {
        private List<User> users;

        private PlayListMiniPokerTask(List<User> users) {
            this.users = users;
            this.setName("AutoPlayMiniPoker");
        }

        @Override
        public void run() {
            MGRoomMiniPoker.this.playListMiniPoker(this.users);
        }
    }

}

