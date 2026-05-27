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
package game.baicao.server;

import bitzero.server.entities.User;
import bitzero.server.extensions.data.BaseMsg;
import game.baicao.server.BaicaoGameServer;
import game.baicao.server.GameLogic;
import game.baicao.server.GamePlayer;
import game.baicao.server.cmd.send.SendUpdateAutoStart;
import game.baicao.server.logic.CardSuit;
import game.baicao.server.logic.Gamble;
import game.baicao.server.logic.GroupCard;
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
    public static final int GS_NO_START = 0;
    public static final int GS_GAME_PLAYING = 1;
    public static final int GS_GAME_END = 2;
    public static final int NO_ACTION = 0;
    public static final int CHIA_BAI = 1;
    public static final int MO_BAI = 2;
    public int roomOwnerChair = 8;
    public int roomCreatorUserId;
    public int gameState = 0;
    public int gameAction = 0;
    public int countDown = 0;
    public boolean isAutoStart = false;
    public Gamble game = new Gamble();
    public BaicaoGameServer gameServer;
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
                if (GameUtils.isBot) {
                    this.gameServer.botAutoPlay();
                }
                if (this.countDown <= 0) {
                    if (this.gameAction == 1) {
                        this.chiaBai();
                    } else if (this.gameAction == 2) {
                        this.moBai();
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
            if (this.countDown % 10 == 1) {
                this.gameServer.botJoinRoom();
            }
        }
    }

    public void moBai() {
        this.gameServer.endGame();
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

    public void chiaBaiNgauNhien() {
        long bet = this.gameServer.getMoneyBet();
        int moneyType = this.gameServer.getRoom().setting.moneyType;
        List<GroupCard> cards = this.game.suit.dealCards(true);
        for (int i = 0; i < 8; ++i) {
            GamePlayer gp = this.gameServer.playerList.get(i);
            GroupCard gc = cards.get(i);
            if (!gc.isNoHu() || this.game.suit.noHu || moneyType != 1 || bet < 1000L || gp.hasUser() && gp.getUser().isBot()) {
                gp.addCards(cards.get(i));
                continue;
            }
            gp.addCards(cards.get(8));
        }
        this.gameServer.chiabai();
        this.gameAction = 2;
        this.countDown = 20;
    }

    private GroupCard getThenRemoveRandomGroupCard(List<GroupCard> cards) {
        int size = cards.size();
        if (size == 0) {
            return null;
        }
        int index = BotManager.instance().getRandomNumber(size);
        GroupCard gc = cards.get(index);
        cards.remove(index);
        return gc;
    }

    public void chiaBaiCanBang() {
        int i;
        boolean isUp = BotManager.instance().balanceMode == 1;
        List<GroupCard> cards = this.game.suit.dealCards(false);
        Collections.sort(cards, GroupCard.groupCardComparator);
        LinkedList<GroupCard> highGroups = new LinkedList<GroupCard>();
        LinkedList<GroupCard> lowGroups = new LinkedList<GroupCard>();
        for (i = 0; i < 4; ++i) {
            highGroups.add(cards.get(i));
        }
        for (i = 4; i < 8; ++i) {
            lowGroups.add(cards.get(i));
        }
        for (i = 0; i < 8; ++i) {
            GroupCard gc;
            GamePlayer gp = this.gameServer.playerList.get(i);
            if (gp.hasUser() && gp.getUser().isBot()) {
                if (isUp) {
                    gc = this.getThenRemoveRandomGroupCard(highGroups);
                    if (gc == null) {
                        gc = this.getThenRemoveRandomGroupCard(lowGroups);
                    }
                    gp.addCards(gc);
                    continue;
                }
                gc = this.getThenRemoveRandomGroupCard(lowGroups);
                if (gc == null) {
                    gc = this.getThenRemoveRandomGroupCard(highGroups);
                }
                gp.addCards(gc);
                continue;
            }
            if (isUp) {
                gc = this.getThenRemoveRandomGroupCard(lowGroups);
                if (gc == null) {
                    gc = this.getThenRemoveRandomGroupCard(highGroups);
                }
                gp.addCards(gc);
                continue;
            }
            gc = this.getThenRemoveRandomGroupCard(highGroups);
            if (gc == null) {
                gc = this.getThenRemoveRandomGroupCard(lowGroups);
            }
            gp.addCards(gc);
        }
        this.gameServer.chiabai();
        this.gameAction = 2;
        this.countDown = 20;
    }

    public void chiaBai() {
        if (BotManager.instance().balanceMode == 0) {
            this.chiaBaiNgauNhien();
        } else {
            boolean isUp = BotManager.instance().balanceMode == 1;
            int x = BotManager.instance().getRandomNumber(3);
            if (x == 0 || isUp) {
                this.chiaBaiCanBang();
            } else {
                this.chiaBaiNgauNhien();
            }
        }
    }

    public boolean canOutRoom() {
        return this.getGameState() == 0;
    }
}

