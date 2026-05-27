package game.modules.XocDia.model;

import bitzero.server.entities.Room;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.util.ExtensionUtility;
import bitzero.util.common.business.Debug;
import com.vinplay.game.XocDia.XocDiaSoiCauUtil;
import com.vinplay.game.XocDia.history.GamePlayXocDiaModel;
import com.vinplay.game.XocDia.history.XocDiaGamePlayHistoryDetail;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.statics.TransType;
import game.modules.XocDia.XocDiaConstant;
import game.modules.XocDia.XocDiaRoomManager;
import game.modules.XocDia.bot.BotXocDia;
import game.modules.XocDia.model.bet.BetItem;
import game.modules.XocDia.model.bet.XocDiaBetDoorItem;
import game.modules.description.XocDiaDescription.XocDiaDescriptionUtils;
import game.utils.GameUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class XocDiaUtil {
    public static XocDiaUtil _instance;
    private XocDiaBetModel xocDiaBetModel;
    private XocDiaFundModel xocDiaFundModel;
    public BotXocDia botXocDia;
    private GamePlayXocDiaModel mHistoryGamePlay;
    private List<XocDiaGamePlayHistoryDetail> listAllPlayer;
    public static byte[] diceResult;
    UserService userService;

    public static XocDiaUtil getInstance() {
        if (_instance == null) {
            synchronized (XocDiaUtil.class) {
                if (_instance == null) {
                    _instance = new XocDiaUtil();
                }
            }
        }
        return _instance;
    }

    private XocDiaUtil() {
        this.xocDiaBetModel = XocDiaBetModel.getInstance();
        this.xocDiaFundModel = XocDiaFundModel.getInstance();
        this.botXocDia = new BotXocDia();
        this.mHistoryGamePlay = null;
        this.listAllPlayer = new ArrayList<XocDiaGamePlayHistoryDetail>();
        this.userService = new UserServiceImpl();
    }

    public void onTimer() {
        try {
            long curTime = GameUtil.getTimeStampInSeconds();
            long secondTime = curTime - this.xocDiaBetModel.startTime;

            if (this.xocDiaBetModel.status == XocDiaBetModel.BET_PHASE) {
                if (secondTime >= XocDiaFullConfig.BET_PHASE) {
                    this.xocDiaBetModel.updateStatus(XocDiaBetModel.DICE_PHASE);
                    this.xocDiaBetModel.isStart = 1;
                    this.botXocDia.setupBetFun();
                    return; // let next tick handle DICE phase with fresh secondTime
                } else {
                    // During bet phase, let bots bet
                    if (this.botXocDia.timeStartBetFun > 0 && secondTime >= this.botXocDia.timeStartBetFun) {
                        this.botXocDia.betFun();
                    }
                }
            }

            if (this.xocDiaBetModel.status == XocDiaBetModel.DICE_PHASE) {
                if (!this.xocDiaBetModel.flagRunDice) {
                    this.xocDiaBetModel.flagRunDice = true;
                    this.showResult();
                    this.mHistoryGamePlay = new GamePlayXocDiaModel(this.xocDiaBetModel.referenceIdXocDia, this.xocDiaBetModel.totalBet);
                    this.mHistoryGamePlay.setDiceResult(diceResult);
                }

                if (secondTime >= XocDiaFullConfig.DICE_PHASE) {
                    this.xocDiaBetModel.updateStatus(XocDiaBetModel.PAY_PHASE);
                    return; // let next tick handle PAY phase with fresh secondTime
                }
            }

            if (this.xocDiaBetModel.status == XocDiaBetModel.PAY_PHASE) {
                if (!this.xocDiaBetModel.flagRunPayMoney) {
                    this.xocDiaBetModel.flagRunPayMoney = true;
                    this.payMoney();
                    XocDiaSoiCauUtil.addListSoiCau(this.xocDiaBetModel.referenceIdXocDia, diceResult);
                }

                if (secondTime >= XocDiaFullConfig.PAY_PHASE) {
                    this.notifyChangeMoney();
                    this.xocDiaBetModel.saveReferenceIdXocDia();
                    this.xocDiaFundModel.saveFund();
                    this.xocDiaBetModel.reset();
                    this.listAllPlayer.clear();
                }
            }
        } catch (Exception e) {
            Debug.trace(new Object[]{"XocDia onTimer error", e.getMessage()});
        }
    }

    private void showResult() {
        long[] userBet = new long[6];
        for (int i = 0; i < 6; i++) {
            userBet[i] = this.xocDiaBetModel.totalBet[i] - this.xocDiaBetModel.listBotBet[i];
        }
        DiceResult dr = new DiceResult();
        diceResult = dr.getResult(userBet);
    }

    private void payMoney() {
        // Count trang (0s) in dice result
        byte listTrang = 0;
        byte listDen = 0;
        for (int i = 0; i < diceResult.length; i++) {
            if (diceResult[i] == 0) {
                listTrang++;
            } else {
                listDen++;
            }
        }

        // Pay chan door (index 0) if even trang count
        if (listTrang % 2 == 0) {
            XocDiaBetDoorItem xocDiaBetDoorItem = this.xocDiaBetModel.mDoor[XocDiaConstant.BET_CHAN];
            payMoneyHandler(xocDiaBetDoorItem.listBet, xocDiaBetDoorItem.historyBet, (byte) 2, XocDiaConstant.BET_CHAN);
        }
        // Pay le door (index 1) if odd trang count
        if (listTrang % 2 != 0) {
            XocDiaBetDoorItem xocDiaBetDoorItem = this.xocDiaBetModel.mDoor[XocDiaConstant.BET_LE];
            payMoneyHandler(xocDiaBetDoorItem.listBet, xocDiaBetDoorItem.historyBet, (byte) 2, XocDiaConstant.BET_LE);
        }
        // Pay 3-1 door (index 2) if 3 trang
        if (listTrang == 3) {
            XocDiaBetDoorItem xocDiaBetDoorItem = this.xocDiaBetModel.mDoor[XocDiaConstant.BET_3_1];
            payMoneyHandler(xocDiaBetDoorItem.listBet, xocDiaBetDoorItem.historyBet, (byte) 4, XocDiaConstant.BET_3_1);
        }
        // Pay 1-3 door (index 3) if 1 trang
        if (listTrang == 1) {
            XocDiaBetDoorItem xocDiaBetDoorItem = this.xocDiaBetModel.mDoor[XocDiaConstant.BET_1_3];
            payMoneyHandler(xocDiaBetDoorItem.listBet, xocDiaBetDoorItem.historyBet, (byte) 4, XocDiaConstant.BET_1_3);
        }
        // Pay 4-0 door (index 4) if 4 trang
        if (listTrang == 4) {
            XocDiaBetDoorItem xocDiaBetDoorItem = this.xocDiaBetModel.mDoor[XocDiaConstant.BET_4_0];
            payMoneyHandler(xocDiaBetDoorItem.listBet, xocDiaBetDoorItem.historyBet, (byte) 16, XocDiaConstant.BET_4_0);
        }
        // Pay 0-4 door (index 5) if 0 trang (all den)
        if (listTrang == 0) {
            XocDiaBetDoorItem xocDiaBetDoorItem = this.xocDiaBetModel.mDoor[XocDiaConstant.BET_0_4];
            payMoneyHandler(xocDiaBetDoorItem.listBet, xocDiaBetDoorItem.historyBet, (byte) 16, XocDiaConstant.BET_0_4);
        }
    }

    private void payMoneyHandler(Map<Long, BetItem> listBet, List<XocDiaGamePlayHistoryDetail> listHistoryDetail, byte multiplyWin, byte door) {
        for (Map.Entry<Long, BetItem> entry : listBet.entrySet()) {
            Long uId = entry.getKey();
            BetItem betItem = entry.getValue();

            XocDiaGamePlayHistoryDetail detail = new XocDiaGamePlayHistoryDetail(
                    betItem.uId, this.xocDiaBetModel.referenceIdXocDia,
                    betItem.userName, door, betItem.money, betItem.time);

            if (!betItem.isBot) {
                // Calculate win: bet * multiplier * 98/100 (2% fund)
                long winAmount = betItem.money * multiplyWin * 98L / 100L;
                detail.setPay(winAmount);
                listHistoryDetail.add(detail);
                this.listAllPlayer.add(detail);

                // Fund gets 2% of bet
                long fundAmount = betItem.money * 2L / 100L;
                this.xocDiaFundModel.addMoneyToFund(fundAmount);

                // Credit player
                try {
                    String gameName = Games.XOC_DIA.getName();
                    String gameId = "" + Games.XOC_DIA.getId();
                    String description = XocDiaDescriptionUtils.getXocDiaWinDescription(betItem.userName, winAmount);
                    this.userService.updateMoney(betItem.userName, winAmount, "vin", gameName, gameId,
                            description, this.xocDiaBetModel.referenceIdXocDia, Long.valueOf(0L), TransType.END_TRANS);
                } catch (Exception e) {
                    Debug.trace(new Object[]{"XocDia pay error", betItem.userName, e.getMessage()});
                }
            }
        }
    }

    private void notifyChangeMoney() {
        // Group all player results by username and send money change notifications
        for (XocDiaGamePlayHistoryDetail detail : this.listAllPlayer) {
            try {
                String username = detail.getUsername();
                // SUN-748 regression fix: use vin balance, not vin_total (cumulative
                // P&L). See MGRoomTaiXiu.java:1175 for full rationale.
                long currentMoney = this.userService.getMoneyUserCache(username, "vin");
                long pay = detail.getPay();

                game.modules.XocDia.msg.out.MoneyChangeMsg msg = new game.modules.XocDia.msg.out.MoneyChangeMsg();
                msg.currentMoney = currentMoney;
                msg.winMoney = pay;
                msg.door = detail.getDoor();
                sendMessageToUserXocDia(msg, username);
            } catch (Exception e) {
                Debug.trace(new Object[]{"XocDia notifyChangeMoney error", e.getMessage()});
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void sendMessageToUserXocDia(BaseMsg msg, String username) {
        try {
            List users = ExtensionUtility.getExtension().getApi().getUserByName(username);
            if (users != null && !users.isEmpty()) {
                ExtensionUtility.getExtension().sendUsers(msg, users);
            }
        } catch (Exception e) {
            // user offline, ignore
        }
    }

    @SuppressWarnings("unchecked")
    public static void sendMessageToAllUserXocDia(BaseMsg msg) {
        try {
            Room room = XocDiaRoomManager.getRoomToJoin();
            java.util.Collection sessions = (java.util.Collection) room.getSessionList();
            ExtensionUtility.getExtension().send(msg, sessions);
        } catch (Exception e) {
            Debug.trace(new Object[]{"XocDia broadcast error", e.getMessage()});
        }
    }
}
