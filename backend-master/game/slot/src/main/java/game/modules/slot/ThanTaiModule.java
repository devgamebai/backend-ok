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
import com.vinplay.vbee.common.models.slot.SlotFreeSpin;
import com.vinplay.vbee.common.utils.CommonUtils;
import game.BotJackpotTimer.BotJackPotThanTaiTimer;
import game.modules.slot.cmd.rev.audition.MinimizeAuditionCmd;
import game.modules.slot.cmd.rev.thantai.*;
import game.modules.slot.cmd.send.thantai.ThanTaiInfoMsg;
import game.modules.slot.entities.BotMinigame;
import game.modules.slot.room.ThanTaiRoom;
import game.modules.slot.utils.SlotUtils;
import game.util.ConfigGame;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ThanTaiModule extends SlotModule {
    private long referenceId = 1L;
    private String fullLines = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25";

    private static String thanTaiJackpotFund = "SlotThanTai_Jackpot_Fund";
    private static String thanTaiMinigameFund = "SlotThanTai_Minigame_Fund";

    public final String keyBotJackpotThanTai = "Bot_SlotThanTai_time";

    public ThanTaiModule() {
        this.gameName = Games.SPARTAN.getName();
    }

    private static ThanTaiModule _instance;
    public static ThanTaiModule getInstance() {
        return _instance;
    }

    public void init() {
        super.init();
        long[] funds = new long[3];
        int[] initPotValues = {500000, 5000000, 50000000};
        long[] fundsJackPot = new long[3];
        long[] fundMiniGame = new long[3];
        long[] timeJackPot = new long[3];

        try {
            this.pots = this.service.getPots(this.gameName);
            if (this.pots == null || this.pots.length < 3) {
                this.pots = new long[]{0, 0, 0};
            }
            Debug.trace(this.gameName + " POTS: " + CommonUtils.arrayLongToString(this.pots));
            funds = this.service.getFunds(this.gameName);
            if (funds == null || funds.length < 3) {
                funds = new long[]{0, 0, 0};
            }
            Debug.trace(this.gameName + ": " + CommonUtils.arrayLongToString(funds));

            fundsJackPot = this.service.getFunds(thanTaiJackpotFund);
            if (fundsJackPot == null || fundsJackPot.length < 3) {
                fundsJackPot = new long[]{0, 0, 0};
            }
            Debug.trace("ThanTai FUNDS JACKPOT: " + CommonUtils.arrayLongToString(fundsJackPot));

            fundMiniGame = this.service.getFunds(thanTaiMinigameFund);
            if (fundMiniGame == null || fundMiniGame.length < 3) {
                fundMiniGame = new long[]{0, 0, 0};
            }
            Debug.trace("ThanTai FUNDS Minigame: " + CommonUtils.arrayLongToString(fundMiniGame));

            timeJackPot = this.service.getFunds(keyBotJackpotThanTai);
            if (timeJackPot == null || timeJackPot.length < 3) {
                timeJackPot = new long[]{0, 0, 0};
            }
            Debug.trace("ThanTai TIME JACKPOT: " + CommonUtils.arrayLongToString(timeJackPot));
        } catch (Exception e) {
            Debug.trace("Init " + this.gameName + " error ", e);
        }

        this.rooms.put(this.gameName + "_vin_100", new ThanTaiRoom(this, (byte) 0, this.gameName + "_vin_100", (short) 1,
                this.pots[0], funds[0], 100, initPotValues[0],
                fundsJackPot[0], thanTaiJackpotFund + "_vin_100",
                fundMiniGame[0], thanTaiMinigameFund + "_vin_100"));
        this.rooms.put(this.gameName + "_vin_1000", new ThanTaiRoom(this, (byte) 1, this.gameName + "_vin_1000", (short) 1,
                this.pots[1], funds[1], 1000, initPotValues[1],
                fundsJackPot[1], thanTaiJackpotFund + "_vin_1000",
                fundMiniGame[1], thanTaiMinigameFund + "_vin_1000"));
        this.rooms.put(this.gameName + "_vin_10000", new ThanTaiRoom(this, (byte) 2, this.gameName + "_vin_10000", (short) 1,
                this.pots[2], funds[2], 10000, initPotValues[2],
                fundsJackPot[2], thanTaiJackpotFund + "_vin_10000",
                fundMiniGame[2], thanTaiMinigameFund + "_vin_10000"));

        Debug.trace("INIT " + this.gameName + " DONE");
        this.referenceId = this.slotService.getLastReferenceId(this.gameName);
        Debug.trace("START " + this.gameName + " REFERENCE ID= " + this.referenceId);

        CacheServiceImpl sv = new CacheServiceImpl();
        try {
            sv.removeKey(this.gameName + "_last_day_x2");
        } catch (KeyNotFoundException e2) {
            Debug.trace("KEY NOT FOUND");
        }
        this.ngayX2 = "";

        this.getParentExtension().addEventListener(BZEventType.USER_DISCONNECT, this);
        BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.gameLoopTask, 10, 1, TimeUnit.SECONDS);
        BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(new BotJackPotThanTaiTimer(timeJackPot, this), 10, 5, TimeUnit.SECONDS);

        _instance = this;
    }

    @Override
    public long getBaseBetting(byte roomId) {
        switch (roomId) {
            case 0: return 100L;
            case 1: return 1000L;
            case 2: return 10000L;
        }
        return 0L;
    }

    public void handleServerEvent(IBZEvent ibzevent) throws BZException {
        if (ibzevent.getType() == BZEventType.USER_DISCONNECT) {
            User user = (User) ibzevent.getParameter((IBZEventParam) BZEventParam.USER);
            this.userDis(user);
        }
    }

    private void userDis(User user) {
        ThanTaiRoom room = (ThanTaiRoom) user.getProperty((Object) ("MGROOM_" + this.gameName + "_INFO"));
        if (room != null) {
            room.quitRoom(user);
            room.stopAutoPlay(user);
        }
    }

    @Override
    public void handleClientRequest(User user, DataCmd dataCmd) {
        Debug.trace((Object) ("thantai handleClientRequest " + dataCmd.getId()));
        switch (dataCmd.getId()) {
            case 7003: this.subScribe(user, dataCmd); break;
            case 7004: this.unSubScribe(user, dataCmd); break;
            case 7005: this.changeRoom(user, dataCmd); break;
            case 7006: this.autoPlay(user, dataCmd); break;
            case 7001: this.play(user, dataCmd); break;
            case 7013: this.minimize(user, dataCmd); break;
        }
    }

    @Override
    public long getNewReferenceId() {
        return ++this.referenceId;
    }

    protected void subScribe(User user, DataCmd dataCmd) {
        SubscribeThanTaiCmd cmd = new SubscribeThanTaiCmd(dataCmd);
        ThanTaiRoom room = (ThanTaiRoom) this.getRoom(cmd.roomId);
        if (room != null) {
            room.joinRoom(user);
            room.userMaximize(user);
            room.updatePot(user);
            this.updateInfo(user, room);
        } else {
            Debug.trace((Object) (this.gameName + " SUBSCRIBE: room " + cmd.roomId + " not found"));
        }
    }

    private void updateInfo(User user, ThanTaiRoom room) {
        ThanTaiInfoMsg msg = new ThanTaiInfoMsg();
        msg.ngayX2 = this.ngayX2;
        msg.remain = 0;
        msg.currentMoney = this.userService.getMoneyUserCache(user.getName(), "vin");
        SlotFreeSpin freeSpin = this.slotService.getLuotQuayFreeSlot(this.gameName + room.getBetValue(), user.getName());
        if (freeSpin != null && freeSpin.getLines() != null) {
            msg.freeSpin = (byte) freeSpin.getNum();
            msg.lines = freeSpin.getLines();
        }
        this.send((BaseMsg) msg, user);
    }

    protected void unSubScribe(User user, DataCmd dataCmd) {
        UnSubscribeThanTaiCmd cmd = new UnSubscribeThanTaiCmd(dataCmd);
        ThanTaiRoom room = (ThanTaiRoom) this.getRoom(cmd.roomId);
        if (room != null) {
            room.stopAutoPlay(user);
            room.quitRoom(user);
        }
    }

    protected void minimize(User user, DataCmd dataCmd) {
        MinimizeAuditionCmd cmd = new MinimizeAuditionCmd(dataCmd);
        ThanTaiRoom room = (ThanTaiRoom) this.getRoom(cmd.roomId);
        if (room != null) {
            room.quitRoom(user);
            room.userMinimize(user);
        }
    }

    protected void changeRoom(User user, DataCmd dataCmd) {
        ChangeRoomThanTaiCmd cmd = new ChangeRoomThanTaiCmd(dataCmd);
        ThanTaiRoom roomLeaved = (ThanTaiRoom) this.getRoom(cmd.roomLeavedId);
        ThanTaiRoom roomJoined = (ThanTaiRoom) this.getRoom(cmd.roomJoinedId);
        if (roomLeaved != null && roomJoined != null) {
            roomLeaved.stopAutoPlay(user);
            roomLeaved.quitRoom(user);
            roomJoined.joinRoom(user);
            roomJoined.updatePot(user);
            this.updateInfo(user, roomJoined);
        }
    }

    private void play(User user, DataCmd dataCmd) {
        PlayThanTaiCmd cmd = new PlayThanTaiCmd(dataCmd);
        ThanTaiRoom room = (ThanTaiRoom) user.getProperty("MGROOM_" + this.gameName + "_INFO");
        if (room != null) {
            try {
                room.play(user, cmd.lines);
            } catch (Exception ex) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                ex.printStackTrace(pw);
                Debug.trace((Object) sw.toString());
            }
        }
    }

    private void autoPlay(User user, DataCmd dataCMD) {
        AutoPlayThanTaiCmd cmd = new AutoPlayThanTaiCmd(dataCMD);
        ThanTaiRoom room = (ThanTaiRoom) user.getProperty((Object) ("MGROOM_" + this.gameName + "_INFO"));
        if (room != null) {
            if (cmd.autoPlay == 1) {
                try {
                    short result = room.play(user, cmd.lines);
                    if (result != 3 && result != 4 && result != 101 && result != 102 && result != 100) {
                        room.autoPlay(user, cmd.lines, result);
                    } else {
                        room.forceStopAutoPlay(user);
                    }
                } catch (Exception ex) {
                    Logger.getLogger(ThanTaiModule.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                room.stopAutoPlay(user);
            }
        }
    }

    @Override
    protected String getRoomName(short moneyType, long baseBetting) {
        String moneyTypeStr = moneyType == 1 ? "vin" : "xu";
        return this.gameName + "_" + moneyTypeStr + "_" + baseBetting;
    }

    @Override
    protected void gameLoop() {
        List<String> bots;
        ThanTaiRoom room;
        int botTime100 = this.getCountTimeBot(this.gameName + "_bot_100");
        ++this.countBot100;
        if (this.countBot100 >= botTime100) {
            if (this.countBot100 == botTime100) {
                bots = BotMinigame.getBots(ConfigGame.getIntValue(this.gameName + "_num_bot_100"), "vin");
                for (String bot : bots) {
                    if (bot == null) continue;
                    room = (ThanTaiRoom) this.rooms.get(this.gameName + "_vin_100");
                    room.playNormal(bot, this.fullLines, getNewReferenceId());
                }
            }
            this.countBot100 = 0;
        }
        int botTime1000 = this.getCountTimeBot(this.gameName + "_bot_1000");
        ++this.countBot1000;
        if (this.countBot1000 >= botTime1000) {
            if (this.countBot1000 == botTime1000) {
                bots = BotMinigame.getBots(ConfigGame.getIntValue(this.gameName + "_num_bot_1000"), "vin");
                for (String bot : bots) {
                    if (bot == null) continue;
                    room = (ThanTaiRoom) this.rooms.get(this.gameName + "_vin_1000");
                    room.playNormal(bot, this.fullLines, getNewReferenceId());
                }
            }
            this.countBot1000 = 0;
        }
        int botTime10000 = this.getCountTimeBot(this.gameName + "_bot_10000");
        ++this.countBot10000;
        if (this.countBot10000 >= botTime10000) {
            if (this.countBot10000 == botTime10000) {
                bots = BotMinigame.getBots(ConfigGame.getIntValue(this.gameName + "_num_bot_10000"), "vin");
                for (String bot : bots) {
                    if (bot == null) continue;
                    room = (ThanTaiRoom) this.rooms.get(this.gameName + "_vin_10000");
                    room.playNormal(bot, this.fullLines, getNewReferenceId());
                }
            }
            this.countBot10000 = 0;
        }
    }
}
