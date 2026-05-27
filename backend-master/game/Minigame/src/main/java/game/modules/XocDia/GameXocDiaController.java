package game.modules.XocDia;

import bitzero.server.BitZeroServer;
import bitzero.server.core.BZEventParam;
import bitzero.server.core.BZEventType;
import bitzero.server.core.IBZEvent;
import bitzero.server.core.IBZEventListener;
import bitzero.server.core.IBZEventParam;
import bitzero.server.core.IBZEventType;
import bitzero.server.entities.Room;
import bitzero.server.entities.User;
import bitzero.server.exceptions.BZException;
import bitzero.server.extensions.BaseClientRequestHandler;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.extensions.data.DataCmd;
import bitzero.util.common.business.Debug;
import com.vinplay.game.XocDia.XocDiaSoiCauUtil;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.statics.TransType;
import game.modules.XocDia.model.DiceResult;
import game.modules.XocDia.model.XocDiaBetModel;
import game.modules.XocDia.model.XocDiaFullConfig;
import game.modules.XocDia.model.XocDiaFundModel;
import game.modules.XocDia.model.XocDiaUtil;
import game.modules.XocDia.model.bet.BetItem;
import game.modules.XocDia.model.bet.XocDiaBetDoorItem;
import game.modules.XocDia.msg.in.BetXocDia;
import game.modules.XocDia.msg.out.BetResponseMsg;
import game.modules.XocDia.msg.out.EndEndGameMsg;
import game.modules.XocDia.msg.out.EndGameMsg;
import game.modules.XocDia.msg.out.HistoryMsg;
import game.modules.XocDia.msg.out.InfoSessionMsg;
import game.modules.XocDia.msg.out.MoneyChangeMsg;
import game.modules.XocDia.msg.out.NewPlayerMsg;
import game.modules.description.XocDiaDescription.XocDiaDescriptionUtils;
import game.utils.GameUtil;

import java.util.concurrent.TimeUnit;

public class GameXocDiaController extends BaseClientRequestHandler {
    private UserService userService;
    private Room room;

    // Game loop state
    private XocDiaBetModel betModel;
    private XocDiaFundModel fundModel;
    private XocDiaUtil xocDiaUtil;

    public GameXocDiaController() {
        this.userService = new UserServiceImpl();
    }

    public void init() {
        Debug.trace(new Object[]{"XocDia Tu Linh initializing..."});
        this.betModel = XocDiaBetModel.getInstance();
        this.fundModel = XocDiaFundModel.getInstance();
        this.xocDiaUtil = XocDiaUtil.getInstance();

        try {
            this.room = XocDiaRoomManager.createRoomXocDia();
        } catch (Exception e) {
            Debug.trace(new Object[]{"XocDia createRoom error", e.getMessage()});
        }

        // Start game loop - 1 second tick
        BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(
                new GameLoopTask(), 3, 1, TimeUnit.SECONDS);

        this.getParentExtension().addEventListener(
                (IBZEventType) BZEventType.USER_DISCONNECT, (IBZEventListener) this);

        Debug.trace(new Object[]{"XocDia Tu Linh initialized, room=" + this.room});
    }

    public void handleClientRequest(User user, DataCmd dataCmd) {
        try {
            switch (dataCmd.getId()) {
                case 8001:
                    handleJoinRoom(user);
                    break;
                case 8002:
                    handleLeaveRoom(user);
                    break;
                case 8010:
                    bet(user, dataCmd);
                    break;
                case 8012:
                    getHistory(user);
                    break;
            }
        } catch (Exception e) {
            Debug.trace(new Object[]{"XocDia handleClientRequest error", e.getMessage()});
        }
    }

    public void handleJoinRoom(User user) {
        try {
            if (this.room != null) {
                // Add user to room if not already in
                try {
                    this.room.addUser(user);
                } catch (Exception e) {
                    // might already be in room, ignore
                }

                user.setProperty("XOCDIA_ROOM", this.room);

                // Notify others
                NewPlayerMsg newPlayerMsg = new NewPlayerMsg();
                newPlayerMsg.playerCount = this.room.getSize().getUserCount();
                newPlayerMsg.playerName = user.getName();
                XocDiaUtil.sendMessageToAllUserXocDia(newPlayerMsg);

                // Send current session state to the joining player
                sendInfoSession(user);
            }
        } catch (Exception e) {
            Debug.trace(new Object[]{"XocDia handleJoinRoom error", e.getMessage()});
        }
    }

    public void handleLeaveRoom(User user) {
        try {
            if (this.room != null) {
                try {
                    this.room.removeUser(user);
                } catch (Exception e) {
                    // might not be in room
                }
                user.removeProperty("XOCDIA_ROOM");
            }
        } catch (Exception e) {
            Debug.trace(new Object[]{"XocDia handleLeaveRoom error", e.getMessage()});
        }
    }

    private void bet(User user, DataCmd dataCmd) {
        try {
            BetXocDia cmd = new BetXocDia(dataCmd);

            // Validate phase
            if (this.betModel.status != XocDiaBetModel.BET_PHASE) {
                return;
            }

            byte door = cmd.type;
            long betAmount = cmd.money;

            // Validate door (0-5)
            if (door < 0 || door > 5) {
                return;
            }

            // Validate bet amount
            if (betAmount <= 0 || betAmount < 1000) {
                return;
            }

            // Check user balance
            // SUN-748 regression fix: must use vin (actual balance), not vin_total
            // (cumulative P&L). Losing players have vin_total < vin and could have
            // valid bets rejected here, or — worst case, vin_total < 0 — every bet
            // silently rejected.
            long currentMoney = this.userService.getMoneyUserCache(user.getName(), "vin");
            if (currentMoney < betAmount) {
                return;
            }

            // Deduct money
            String gameName = Games.XOC_DIA.getName();
            String gameId = "" + Games.XOC_DIA.getId();
            String description = XocDiaDescriptionUtils.getXocDiaBetDescription(user.getName(), betAmount);
            this.userService.updateMoney(user.getName(), -betAmount, "vin", gameName, gameId,
                    description, this.betModel.referenceIdXocDia, Long.valueOf(0L), TransType.END_TRANS);

            // Record bet
            long userId = user.getId();
            this.betModel.bet(userId, user.getName(), door, betAmount, false);

            // Send bet confirmation
            // SUN-748 regression fix: send vin (actual post-bet balance), not vin_total.
            long newBalance = this.userService.getMoneyUserCache(user.getName(), "vin");
            BetResponseMsg responseMsg = new BetResponseMsg();
            responseMsg.currentMoney = newBalance;
            responseMsg.door = door;
            responseMsg.betAmount = betAmount;
            this.send((BaseMsg) responseMsg, user);

            // Broadcast updated totals to all
            broadcastInfoSession();

        } catch (Exception e) {
            Debug.trace(new Object[]{"XocDia bet error", user.getName(), e.getMessage()});
        }
    }

    private void getHistory(User user) {
        try {
            HistoryMsg msg = new HistoryMsg();
            msg.historyModel = XocDiaSoiCauUtil.getListSoiCau();
            this.send((BaseMsg) msg, user);
        } catch (Exception e) {
            Debug.trace(new Object[]{"XocDia getHistory error", e.getMessage()});
        }
    }

    public void handleServerEvent(IBZEvent ibzevent) throws BZException {
        if (ibzevent.getType() == BZEventType.USER_DISCONNECT) {
            User user = (User) ibzevent.getParameter((IBZEventParam) BZEventParam.USER);
            this.userDis(user);
        }
    }

    public void userDis(User user) {
        try {
            Object roomObj = user.getProperty("XOCDIA_ROOM");
            if (roomObj != null) {
                Room r = (Room) roomObj;
                try {
                    r.removeUser(user);
                } catch (Exception e) {
                    // ignore
                }
                user.removeProperty("XOCDIA_ROOM");
            }
        } catch (Exception e) {
            // ignore
        }
    }

    // --- Game loop ---

    private void sendInfoSession(User user) {
        InfoSessionMsg msg = buildInfoSessionMsg();
        this.send((BaseMsg) msg, user);
    }

    private void broadcastInfoSession() {
        InfoSessionMsg msg = buildInfoSessionMsg();
        XocDiaUtil.sendMessageToAllUserXocDia(msg);
    }

    private InfoSessionMsg buildInfoSessionMsg() {
        InfoSessionMsg msg = new InfoSessionMsg();
        msg.referenceId = this.betModel.referenceIdXocDia;
        msg.status = this.betModel.status;

        long curTime = GameUtil.getTimeStampInSeconds();
        long elapsed = curTime - this.betModel.startTime;
        int phaseDuration = 0;
        switch (this.betModel.status) {
            case 0: // BET
                phaseDuration = XocDiaFullConfig.BET_PHASE;
                break;
            case 1: // DICE
                phaseDuration = XocDiaFullConfig.DICE_PHASE;
                break;
            case 2: // PAY
                phaseDuration = XocDiaFullConfig.PAY_PHASE;
                break;
        }
        msg.timeRemaining = Math.max(0, (int) (phaseDuration - elapsed));

        for (int i = 0; i < 6; i++) {
            msg.totalBets[i] = this.betModel.totalBet[i];
        }
        msg.fund = this.fundModel.getFund();
        if (this.room != null) {
            msg.playerCount = this.room.getSize().getUserCount();
        }
        msg.lastDiceResult = XocDiaUtil.diceResult;
        return msg;
    }

    private void broadcastEndGame() {
        EndGameMsg msg = new EndGameMsg();
        msg.referenceId = this.betModel.referenceIdXocDia;
        if (XocDiaUtil.diceResult != null) {
            msg.diceResult = XocDiaUtil.diceResult;
        }
        for (int i = 0; i < 6; i++) {
            msg.totalBets[i] = this.betModel.totalBet[i];
        }
        msg.fund = this.fundModel.getFund();
        XocDiaUtil.sendMessageToAllUserXocDia(msg);
    }

    private void broadcastEndEndGame() {
        EndEndGameMsg msg = new EndEndGameMsg();
        msg.nextReferenceId = this.betModel.referenceIdXocDia;
        XocDiaUtil.sendMessageToAllUserXocDia(msg);
    }

    private void broadcastMoneyChange() {
        try {
            Room xdRoom = XocDiaRoomManager.getRoomToJoin();
            if (xdRoom == null) return;
            for (Object obj : xdRoom.getUserList()) {
                User u = (User) obj;
                try {
                    MoneyChangeMsg msg = new MoneyChangeMsg();
                    long currentMoney = this.userService.getMoneyUserCache(u.getName(), "vin");
                    msg.winMoney = 0; // actual win calculated by XocDiaUtil.notifyChangeMoney
                    msg.currentMoney = currentMoney;
                    this.send((BaseMsg) msg, u);
                } catch (Exception e) {
                    Debug.trace(new Object[]{"broadcastMoneyChange error for " + u.getName(), e.getMessage()});
                }
            }
        } catch (Exception e) {
            Debug.trace(new Object[]{"broadcastMoneyChange error", e.getMessage()});
        }
    }

    // --- Inner class: game loop task ---

    private class GameLoopTask implements Runnable {
        private int phaseState = 0; // 0=BET, 1=DICE_SHOWN, 2=PAY_DONE, 3=DELAY_DONE
        private boolean diceShown = false;
        private boolean payDone = false;
        private boolean endEndSent = false;

        public void run() {
            try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
                byte prevStatus = betModel.status;
                xocDiaUtil.onTimer();
                byte newStatus = betModel.status;

                // Detect phase transitions and broadcast
                if (prevStatus == 0 && newStatus == 1) {
                    // BET → DICE: broadcast dice result
                    broadcastEndGame();
                } else if (prevStatus == 1 && newStatus == 2) {
                    // DICE → PAY: broadcast money changes
                    broadcastMoneyChange();
                } else if (prevStatus == 2 && newStatus == 0) {
                    // PAY → NEW ROUND: broadcast end + new session info
                    broadcastEndEndGame();
                    broadcastInfoSession();
                } else if (newStatus == 0) {
                    // Still in BET phase: broadcast info every second
                    broadcastInfoSession();
                }
            } catch (Throwable e) {
                Debug.trace(new Object[]{"XocDia GameLoop error", e.getMessage()});
            }
        }
    }
}
