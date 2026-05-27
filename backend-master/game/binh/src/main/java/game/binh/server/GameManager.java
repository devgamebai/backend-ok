/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  bitzero.server.extensions.data.BaseMsg
 *  game.modules.bot.BotManager
 *  game.modules.gameRoom.entities.GameRoom
 *  game.modules.gameRoom.entities.GameRoomSetting
 *  game.utils.GameUtils
 */
package game.binh.server;

import bitzero.server.entities.User;
import bitzero.server.extensions.data.BaseMsg;
import game.binh.server.BinhGameServer;
import game.binh.server.GameLogic;
import game.binh.server.GamePlayer;
import game.binh.server.cmd.send.SendUpdateAutoStart;
import game.binh.server.logic.CardSuit;
import game.binh.server.logic.Gamble;
import game.binh.server.logic.GroupCard;
import game.binh.server.logic.ai.BinhGroup;
import game.modules.bot.BotManager;
import game.modules.gameRoom.entities.GameRoom;
import game.modules.gameRoom.entities.GameRoomSetting;
import game.utils.GameUtils;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

public class GameManager {
    public static final int DEM_LA = 1;
    public static final int GS_NO_START = 0;
    public static final int GS_GAME_PLAYING = 1;
    public static final int GS_GAME_END = 2;
    public static final int NO_ACTION = 0;
    public static final int CHIA_BAI = 1;
    public static final int BINH_SO_CHI = 2;
    public static final int HIEN_KET_QUA = 3;
    public int roomOwnerChair = 4;
    public int roomCreatorUserId;
    public int gameState = 0;
    public int gameAction = 0;
    public int countDown = 0;
    public boolean isAutoStart = false;
    public Gamble game = new Gamble();
    public BinhGameServer gameServer;
    public GameLogic logic = new GameLogic();

    public int getGameState() {
        return this.gameState;
    }

    public void prepareNewGame() {
        this.game.reset();
        this.isAutoStart = false;
        this.gameServer.kiemTraTuDongBatDau(5);
    }

    public void gameLoop() {
        if (this.gameState == 0 && this.isAutoStart) {
            --this.countDown;
            if (this.countDown <= 0) {
                this.gameState = 1;
                this.gameServer.start();
            }
        } else if (this.gameState == 1) {
            if (this.gameAction != 0) {
                --this.countDown;
                if (GameUtils.isBot && this.gameAction == 2) {
                    this.gameServer.botAutoPlay();
                }
                if (this.countDown <= 0) {
                    if (this.gameAction == 1) {
                        this.chiaBai();
                    } else if (this.gameAction == 2) {
                        this.gameAction = 3;
                        this.gameServer.endGame();
                    }
                }
            }
        } else if (this.gameState == 2) {
            --this.countDown;
            if (this.countDown == 5) {
                this.gameServer.notifyNoHu();
            }
            if (this.countDown <= 0) {
                this.gameServer.pPrepareNewGame();
            }
        } else {
            ++this.countDown;
            this.gameServer.kiemTraTuDongBatDau(5);
            if (this.countDown % 11 == 10) {
                this.gameServer.botJoinRoom();
            }
        }
    }

    public void notifyAutoStartToUsers(int after) {
        SendUpdateAutoStart msg = new SendUpdateAutoStart();
        msg.isAutoStart = this.isAutoStart;
        msg.autoStartTime = (byte)after;
        this.gameServer.send(msg);
    }

    public void cancelAutoStart() {
        this.isAutoStart = false;
        this.notifyAutoStartToUsers(0);
    }

    public void makeAutoStart(int after) {
        if (this.gameState != 0) {
            return;
        }
        if (!this.isAutoStart) {
            this.countDown = after;
        } else if (after < this.countDown) {
            this.countDown = after;
        } else {
            after = this.countDown;
        }
        this.isAutoStart = true;
        this.notifyAutoStartToUsers(after);
    }

    public void chiaBai() {
        int botCount = 0;
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.gameServer.playerList.get(i);
            if (gp.getUser() == null || !gp.getUser().isBot()) continue;
            ++botCount;
        }
        if (botCount == 0) {
            this.chiaBaiNgauNhien();
        }
//        else if (BotManager.instance().balanceMode == 0) {
//            int x = BotManager.instance().getRandomNumber(2);
//            if (x == 0 && this.gameServer.getRoom().setting.moneyBet >= 5000L) {
//                this.chiaBaiCanBang(true, botCount);
//            } else {
//                this.chiaBaiNgauNhien();
//            }
//        }
        else {
//            boolean isUp = BotManager.instance().balanceMode == 1;
            boolean isUp = true;
//            int x = BotManager.instance().getRandomNumber(3);
//            if (isUp && this.gameServer.getRoom().setting.moneyBet >= 5000L) {
//                x = 1;
//            }
//            if (x != 0) {
                this.chiaBaiCanBang(isUp, botCount);
//            } else {
//                this.chiaBaiNgauNhien();
//            }
        }
    }

    public void chiaBaiNgauNhien() {
        boolean canJackpot = this.gameServer.getRoom().setting.moneyBet < 1000L;
        List<BinhGroup> cards = this.game.suit.dealCards(this.gameServer.getRoom().setting.rule, canJackpot);
        if (this.game.suit.cheat == 0) {
            Collections.shuffle(cards);
        }
        for (int i = 0; i < 4; ++i) {
            GroupCard gc;
            GamePlayer gp = this.gameServer.playerList.get(i);
            if (gp.getUser() != null && gp.getUser().isBot()) {
                gc = cards.get(i).getOrderGroupCard();
                gp.addCards(gc, this.game.isCheat, this.gameServer.getRoom().setting.rule);
                continue;
            }
            gc = cards.get(i).getRandomGroupCard();
            gp.addCards(gc, this.game.isCheat, this.gameServer.getRoom().setting.rule);
        }
        this.gameServer.chiabai();
        this.gameAction = 2;
        this.countDown = 66;
    }

    public void chiaBaiCanBang(boolean isUp, int botCount) {
        boolean canJackpot = this.gameServer.getRoom().setting.moneyBet < 1000L;
        List<BinhGroup> cards = this.game.suit.dealCards(this.gameServer.getRoom().setting.rule, canJackpot);
        Collections.sort(cards, BinhGroup.SORT_COMPARATOR);
        LinkedList<BinhGroup> high = new LinkedList<BinhGroup>();
        LinkedList<BinhGroup> low = new LinkedList<BinhGroup>();
        int highSize = 4 - botCount;
        if (isUp) {
            highSize = botCount;
        }
        for (int i = 0; i < 4; ++i) {
            if (i < highSize) {
                high.add(cards.get(i));
                continue;
            }
            low.add(cards.get(i));
        }
        Collections.shuffle(high);
        Collections.shuffle(low);
        int lowCount = 0;
        int highCount = 0;
        for (int i = 0; i < 4; ++i) {
            GroupCard gc;
            GamePlayer gp = this.gameServer.playerList.get(i);
            if (gp.getUser() != null && gp.getUser().isBot()) {
                if (isUp) {
                    gc = ((BinhGroup)high.get(highCount++)).getOrderGroupCard();
                    gp.addCards(gc, this.game.isCheat, this.gameServer.getRoom().setting.rule);
                    continue;
                }
                gc = ((BinhGroup)low.get(lowCount++)).getOrderGroupCard();
                gp.addCards(gc, this.game.isCheat, this.gameServer.getRoom().setting.rule);
                continue;
            }
            if (isUp) {
                gc = ((BinhGroup)low.get(lowCount++)).getOrderGroupCard();
                gp.addCards(gc, this.game.isCheat, this.gameServer.getRoom().setting.rule);
                continue;
            }
            gc = ((BinhGroup)high.get(highCount++)).getOrderGroupCard();
            gp.addCards(gc, this.game.isCheat, this.gameServer.getRoom().setting.rule);
        }
        this.gameServer.chiabai();
        this.gameAction = 2;
        this.countDown = 66;
    }

    public boolean canOutRoom() {
        return this.getGameState() == 0;
    }
}

