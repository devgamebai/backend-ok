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
 *  com.vinplay.cardlib.models.Card
 *  com.vinplay.dal.service.CaoThapService
 *  com.vinplay.dal.service.impl.CaoThapServiceImpl
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
import bitzero.server.exceptions.BZException;
import bitzero.server.extensions.BZExtension;
import bitzero.server.extensions.BaseClientRequestHandler;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.extensions.data.DataCmd;
import bitzero.server.util.TaskScheduler;
import bitzero.util.common.business.Debug;
import com.vinplay.cardlib.models.Card;
import com.vinplay.dal.service.CaoThapService;
import com.vinplay.dal.service.impl.CaoThapServiceImpl;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import game.modules.minigame.cmd.rev.caothap.ChangeRoomCaoThapCmd;
import game.modules.minigame.cmd.rev.caothap.PlayCaoThapCmd;
import game.modules.minigame.cmd.rev.caothap.StartPlayCaoThapCmd;
import game.modules.minigame.cmd.rev.caothap.StopPlayCaoThapCmd;
import game.modules.minigame.cmd.rev.caothap.UnSubscribeCaoThapCmd;
import game.modules.minigame.cmd.send.caothap.ChangeRoomCaoThapMsg;
import game.modules.minigame.cmd.send.caothap.SubscribeCaoThapMsg;
import game.modules.minigame.cmd.send.caothap.UserInfoCaoThapMsg;
import game.modules.minigame.entities.BotMinigame;
import game.modules.minigame.entities.CaoThapInfo;
import game.modules.minigame.room.MGRoom;
import game.modules.minigame.room.MGRoomCaoThap;
import game.modules.minigame.utils.CaoThapUtils;
import game.modules.minigame.utils.GenerationMiniPoker;
import game.modules.minigame.utils.MiniGameUtils;
import game.utils.ConfigGame;
import game.utils.GameUtils;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CaoThapModule
extends BaseClientRequestHandler {
    public Map<String, MGRoom> rooms = new HashMap<String, MGRoom>();
    private static CaoThapModule _instance;

    public static CaoThapModule getInstance() {
        return _instance;
    }
    private CaoThapService service = new CaoThapServiceImpl();
    private final Runnable rewardDailyTask = new RewardDaily();
    private final Runnable botDailyTask = new BotDailyTask();
    public long referenceId = 0L;

    public void init() {
        super.init();
        long[] pots = new long[10];
        long[] funds = new long[10];
        try {
            pots = this.service.getPotCaoThap();
            Debug.trace(("CAO THAP POTS: " + pots));
            funds = this.service.getFundCaoThap();
            Debug.trace(("CAO THAP FUNDS: " + funds));
            this.referenceId = this.service.getLastReferenceId();
            Debug.trace(("CAO THAP phien: " + this.referenceId));
        }
        catch (SQLException e) {
            Debug.trace((Object[])new Object[]{"Get cao thap pot error ", e.getMessage()});
        }
        this.rooms.put("cao_thap_vin_1000", new MGRoomCaoThap("cao_thap_vin_1000", (byte)1, pots[0], funds[0], 1000));
        this.rooms.put("cao_thap_vin_10000", new MGRoomCaoThap("cao_thap_vin_10000", (byte)1, pots[1], funds[1], 10000));
        this.rooms.put("cao_thap_vin_50000", new MGRoomCaoThap("cao_thap_vin_50000", (byte)1, pots[2], funds[2], 50000));
        this.rooms.put("cao_thap_vin_100000", new MGRoomCaoThap("cao_thap_vin_100000", (byte)1, pots[3], funds[3], 100000));
        this.rooms.put("cao_thap_vin_500000", new MGRoomCaoThap("cao_thap_vin_500000", (byte)1, pots[4], funds[4], 500000));
        this.rooms.put("cao_thap_xu_10000", new MGRoomCaoThap("cao_thap_xu_10000", (byte)0, pots[5], funds[5], 10000));
        this.rooms.put("cao_thap_xu_100000", new MGRoomCaoThap("cao_thap_xu_100000", (byte)0, pots[6], funds[6], 100000));
        this.rooms.put("cao_thap_xu_500000", new MGRoomCaoThap("cao_thap_xu_500000", (byte)0, pots[7], funds[7], 500000));
        this.rooms.put("cao_thap_xu_1000000", new MGRoomCaoThap("cao_thap_xu_1000000", (byte)0, pots[8], funds[8], 1000000));
        this.rooms.put("cao_thap_xu_5000000", new MGRoomCaoThap("cao_thap_xu_5000000", (byte)0, pots[9], funds[9], 5000000));
//        try {
//            int remainTimeTraThuong = MiniGameUtils.calculateTimeRewardOnNextDay("");
//            BitZeroServer.getInstance().getTaskScheduler().schedule(this.rewardDailyTask, remainTimeTraThuong, TimeUnit.SECONDS);
//        }
//        catch (ParseException e) {
//            Debug.trace((Object[])new Object[]{"Calculate time reward Cao Thap error ", e.getMessage()});
//        }
        Debug.trace("INIT CAO THAP DONE");
        this.getParentExtension().addEventListener((IBZEventType)BZEventType.USER_DISCONNECT, (IBZEventListener)this);
        _instance = this;
        this.scheduleBotCT();
    }

    private void scheduleBotCT() {
        long currentTime = System.currentTimeMillis() / 1000L;
        long endToday = DateTimeUtils.getEndTimeToDayAsLong() / 1000L;
        int n = (int)(endToday - currentTime);
        Debug.trace(("current= " + currentTime));
        Debug.trace(("end today= " + endToday));
        Debug.trace(("n= " + n));
        BitZeroServer.getInstance().getTaskScheduler().schedule(this.botDailyTask, n + 100, TimeUnit.SECONDS);
        if (n >= 21600) {
            int numBots = 5;
            int[] times = new int[numBots];
            Random rd = new Random();
            int r = rd.nextInt(100);
            int indexTPSA = -1;
            if (r < 90) {
                indexTPSA = rd.nextInt(numBots);
            }
            for (int i = 0; i < numBots; ++i) {
                times[i] = rd.nextInt(n);
                boolean tpsA = false;
                if (i == indexTPSA) {
                    tpsA = true;
                }
                BotTask botTask = new BotTask(tpsA);
                BitZeroServer.getInstance().getTaskScheduler().schedule((Runnable)botTask, times[i], TimeUnit.SECONDS);
                Debug.trace(("" + i + " = " + times[i]));
            }
        }
    }

    public void handleServerEvent(IBZEvent ibzevent) throws BZException {
        if (ibzevent.getType() == BZEventType.USER_DISCONNECT) {
            User user = (User)ibzevent.getParameter((IBZEventParam)BZEventParam.USER);
            this.userDis(user);
        }
    }

    private void userDis(User user) {
        MGRoomCaoThap room = (MGRoomCaoThap)user.getProperty("MGROOM_CAO_THAP_INFO");
        if (room != null) {
            if (room.getUsers().containsKey(user.getName())) {
                CaoThapInfo info = room.getUsers().get(user.getName());
                info.setId(-1);
                room.getUsers().put(user.getName(), info);
            }
            room.quitRoom(user);
        }
    }

    public void handleClientRequest(User user, DataCmd dataCmd) {
        switch (dataCmd.getId()) {
            case 6004: {
                this.subScribeCaoThap(user, dataCmd);
                break;
            }
            case 6005: {
                this.unSubScribeCaoThap(user, dataCmd);
                break;
            }
            case 6006: {
                this.changeRoom(user, dataCmd);
                break;
            }
            case 6001: {
                if (GameUtils.disablePlayMiniGame(user)) {
                    return;
                }
                this.startPlayCaoThap(user, dataCmd);
                break;
            }
            case 6002: {
                if (GameUtils.disablePlayMiniGame(user)) {
                    return;
                }
                this.playCaoThap(user, dataCmd);
                break;
            }
            case 6007: {
                this.stopPlayCaoThap(user, dataCmd);
            }
        }
    }

    private void subScribeCaoThap(User user, DataCmd dataCmd) {
        SubscribeCaoThapMsg msg = new SubscribeCaoThapMsg();
        byte roomId = -1;
        boolean play = false;
        int cntRoomPlaying = 0;
        for (byte i = 0; i < 10; i = (byte)(i + 1)) {
            MGRoomCaoThap room = this.getRoom(i);
            if (room.getUsers().containsKey(user.getName())) {
                CaoThapInfo info = room.getUsers().get(user.getName());
                if (info.getId() == -1) {
                    info.setId(user.getId());
                    info.setUser(user);
                    room.getUsers().put(user.getName(), info);
                }
                if (info.getId() == user.getId()) {
                    roomId = i;
                    room.joinRoom(user);
                    room.updatePotToUser(user);
                    msg.status = 0;
                    msg.roomId = roomId;
                    this.send((BaseMsg)msg, user);
                    UserInfoCaoThapMsg msgInfo = new UserInfoCaoThapMsg();
                    msgInfo.numA = info.getNumA();
                    msgInfo.card = (byte)info.getCard().getCode();
                    msgInfo.money1 = info.getMoneyUp();
                    msgInfo.money2 = info.getMoney();
                    msgInfo.money3 = info.getMoneyDown();
                    msgInfo.time = info.getTime();
                    msgInfo.step = (byte)info.getStep();
                    msgInfo.referenceId = info.getReferenceId();
                    msgInfo.cards = CaoThapUtils.getCardStr(info.getCarryCards());
                    this.send((BaseMsg)msgInfo, user);
                    return;
                }
                ++cntRoomPlaying;
                continue;
            }
            if (play) continue;
            roomId = i;
            play = true;
        }
        if (cntRoomPlaying >= 10) {
            msg.status = 1;
        } else {
            MGRoomCaoThap room = this.getRoom(roomId);
            room.joinRoom(user);
            room.updatePotToUser(user);
            msg.status = 0;
        }
        msg.roomId = roomId;
        this.send((BaseMsg)msg, user);
    }

    private void unSubScribeCaoThap(User user, DataCmd dataCmd) {
        UnSubscribeCaoThapCmd cmd = new UnSubscribeCaoThapCmd(dataCmd);
        MGRoomCaoThap room = this.getRoom(cmd.roomId);
        if (room != null) {
            room.quitRoom(user);
        } else {
            Debug.trace(("CAO THAP UNSUBSCRIBE: room " + cmd.roomId + " not found"));
        }
    }

    private void changeRoom(User user, DataCmd dataCmd) {
        ChangeRoomCaoThapCmd cmd = new ChangeRoomCaoThapCmd(dataCmd);
        MGRoomCaoThap roomLeaved = this.getRoom(cmd.roomLeavedId);
        MGRoomCaoThap roomJoined = this.getRoom(cmd.roomJoinedId);
        ChangeRoomCaoThapMsg msg = new ChangeRoomCaoThapMsg();
        if (roomLeaved != null && roomJoined != null) {
            if (roomLeaved.getUsers().containsKey(user.getName())) {
                CaoThapInfo info = roomLeaved.getUsers().get(user.getName());
                if (info.getId() != -1 && info.getId() == user.getId()) {
                    msg.status = (byte)2;
                    this.send((BaseMsg)msg, user);
                } else {
                    roomLeaved.quitRoom(user);
                    roomJoined.joinRoom(user);
                    roomJoined.updatePotToUser(user);
                    msg.status = 0;
                    this.send((BaseMsg)msg, user);
                }
            } else if (roomJoined.getUsers().containsKey(user.getName())) {
                CaoThapInfo info = roomJoined.getUsers().get(user.getName());
                if (info.getId() == -1) {
                    info.setUser(user);
                    info.setId(user.getId());
                    roomJoined.getUsers().put(user.getName(), info);
                }
                if (info.getId() == user.getId()) {
                    roomLeaved.quitRoom(user);
                    roomJoined.joinRoom(user);
                    roomJoined.updatePotToUser(user);
                    msg.status = 1;
                    this.send((BaseMsg)msg, user);
                    UserInfoCaoThapMsg msg1 = new UserInfoCaoThapMsg();
                    msg1.numA = info.getNumA();
                    msg1.card = (byte)info.getCard().getCode();
                    msg1.money1 = info.getMoneyUp();
                    msg1.money2 = info.getMoney();
                    msg1.money3 = info.getMoneyDown();
                    msg1.time = info.getTime();
                    msg1.step = (byte)info.getStep();
                    msg1.referenceId = info.getReferenceId();
                    msg1.cards = CaoThapUtils.getCardStr(info.getCarryCards());
                    this.send((BaseMsg)msg1, user);
                } else {
                    msg.status = (byte)3;
                    this.send((BaseMsg)msg, user);
                }
            } else {
                roomLeaved.quitRoom(user);
                roomJoined.joinRoom(user);
                roomJoined.updatePotToUser(user);
                msg.status = 0;
                this.send((BaseMsg)msg, user);
            }
        } else {
            Debug.trace(("CAO THAP: change room error, leaved= " + cmd.roomLeavedId + ", joined= " + cmd.roomJoinedId));
        }
    }

    private void startPlayCaoThap(User user, DataCmd dataCmd) {
        StartPlayCaoThapCmd cmd = new StartPlayCaoThapCmd(dataCmd);
        String roomName = this.getRoomName(cmd.moneyType, cmd.betValue);
        MGRoomCaoThap room = (MGRoomCaoThap)this.rooms.get(roomName);
        if (room != null) {
            ++this.referenceId;
            room.startPlay(user, cmd.betValue, this.referenceId);
        } else {
            Debug.trace(("CAO THAP: room " + roomName + " not found"));
        }
    }

    private void playCaoThap(User user, DataCmd dataCmd) {
        PlayCaoThapCmd cmd = new PlayCaoThapCmd(dataCmd);
        String roomName = this.getRoomName(cmd.moneyType, cmd.betValue);
        MGRoomCaoThap room = (MGRoomCaoThap)this.rooms.get(roomName);
        if (room != null) {
            room.play(user, cmd.choose);
        }
    }

    private void stopPlayCaoThap(User user, DataCmd dataCmd) {
        StopPlayCaoThapCmd cmd = new StopPlayCaoThapCmd(dataCmd);
        String roomName = this.getRoomName(cmd.moneyType, cmd.betValue);
        MGRoomCaoThap room = (MGRoomCaoThap)this.rooms.get(roomName);
        if (room != null) {
            room.stopPlay(user);
        }
    }

    private String getRoomName(short moneyType, long baseBetting) {
        String moneyTypeStr = "xu";
        if (moneyType == 1) {
            moneyTypeStr = "vin";
        }
        return "cao_thap_" + moneyTypeStr + "_" + baseBetting;
    }

    private MGRoomCaoThap getRoom(byte roomId) {
        short moneyType = this.getMoneyTypeFromRoomId(roomId);
        long baseBetting = this.getBaseBetting(roomId);
        String roomName = this.getRoomName(moneyType, baseBetting);
        MGRoomCaoThap room = (MGRoomCaoThap)this.rooms.get(roomName);
        return room;
    }

    private short getMoneyTypeFromRoomId(byte roomId) {
//        return 0 <= roomId && roomId < 5;
        if ((0 <= roomId) && (roomId < 5)) {
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
                return 50000L;
            }
            case 3: {
                return 100000L;
            }
            case 4: {
                return 500000L;
            }
            case 5: {
                return 10000L;
            }
            case 6: {
                return 100000L;
            }
            case 7: {
                return 500000L;
            }
            case 8: {
                return 1000000L;
            }
            case 9: {
                return 5000000L;
            }
        }
        return 0L;
    }

    private int[] getTiLeTPS() {
        String configTPS = ConfigGame.getValueString("cp_ti_le_tps");
        String[] arr = configTPS.split(",");
        int[] tiLe = new int[arr.length];
        for (int i = 0; i < arr.length; ++i) {
            tiLe[i] = Integer.parseInt(arr[i]);
        }
        return tiLe;
    }

    private void insertBotSuKien(boolean thungPhaSanhA) {
        String botName = BotMinigame.getRandomBot("vin");
        int[] tiLe = this.getTiLeTPS();
        if (botName != null) {
            Random rd = new Random();
            int n = rd.nextInt(5);
            long betValue = 1000L;
            long prize = 50000L;
            if (n == 0) {
                betValue = 10000L;
                n = rd.nextInt(950000);
                prize = n + 50000;
            } else {
                n = rd.nextInt(180000);
                prize = n + 20000;
            }
            int type = 0;
            if (thungPhaSanhA) {
                type = 3;
            } else {
                n = rd.nextInt(100);
                for (int i = 0; i < tiLe.length; ++i) {
                    if (n >= tiLe[i]) continue;
                    type = i;
                    break;
                }
            }
            List<Card> cards = new ArrayList();
            switch (type) {
                case 0: {
                    cards = GenerationMiniPoker.randomTuQuy();
                    break;
                }
                case 1: {
                    cards = GenerationMiniPoker.randomThungPhaSanhNho();
                    break;
                }
                case 2: {
                    cards = GenerationMiniPoker.randomThungPhaSanhJDenK();
                    break;
                }
                default: {
                    cards = GenerationMiniPoker.randomThungPhaSanhA();
                }
            }
            StringBuilder cardsStr = new StringBuilder();
            for (Card c : cards) {
                cardsStr.append(c.getCode()).append(",");
            }
            Debug.trace(("CaoThap Su kien: Bot " + botName + " prize= " + prize + ", cards= " + cardsStr));
            this.service.insertBotEvent(botName, betValue, prize, cardsStr.toString());
        }
    }

    private final class RewardDaily
    implements Runnable {
        private RewardDaily() {
        }

        @Override
        public void run() {
//            CaoThapUtils.reward();
//            BitZeroServer.getInstance().getTaskScheduler().schedule(CaoThapModule.this.rewardDailyTask, 24, TimeUnit.HOURS);
//            Debug.trace("Tra thuong Cao Thap");
        }
    }

    private final class BotDailyTask
    implements Runnable {
        private BotDailyTask() {
        }

        @Override
        public void run() {
            CaoThapModule.this.scheduleBotCT();
        }
    }

    private final class BotTask
    implements Runnable {
        private boolean thungPhaSanhA = false;

        public BotTask(boolean thungPhaSanhA) {
            this.thungPhaSanhA = thungPhaSanhA;
        }

        @Override
        public void run() {
            CaoThapModule.this.insertBotSuKien(this.thungPhaSanhA);
        }
    }

}

