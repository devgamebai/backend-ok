/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.tour.server;

import game.tour.server.cmd.send.SendUpdateAutoStart;
import game.tour.server.logic.ComboGroupCard;
import game.tour.server.logic.Gamble;
import game.tour.server.logic.GroupCard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class GameManager {
    public static final int DEM_LA = 1;
    public static final int GS_NO_START = 0;
    public static final int GS_GAME_PLAYING = 1;
    public static final int GS_GAME_END = 3;
    public static final int NO_ACTION = -1;
    public static final int SELECT_DEALER = 0;
    public static final int DEAL_PRIVATE_CARD = 1;
    public static final int DEAL_COMMUNITY_CARD = 2;
    public static final int CHANGE_ROUND = 3;
    public static final int IN_ROUND = 4;
    public static final int CHANGE_TURN = 5;
    public int roomOwnerChair = 6;
    public int roomCreatorUserId;
    public int rCuocLon = 1;
    public volatile int gameState = 0;
    public volatile int gameAction = -1;
    public volatile int botCountDown = 0;
    public volatile int countDown = 0;
    public volatile boolean isAutoStart = false;
    public volatile int currentChair = -1;
    public volatile int lastRaiseChair = -1;
    public Gamble game = new Gamble();
    public PokerTourGameServer gameServer;
    public GameLogic logic = new GameLogic();

    public int getGameState() {
        return this.gameState;
    }

    public void prepareNewGame() {
        this.game.reset();
        this.isAutoStart = false;
        this.botCountDown = 0;
        this.gameServer.kiemTraTuDongBatDau(2);
    }

    public void gameLoop() {
        if (this.gameState == 0 && this.isAutoStart) {
            --this.countDown;
            if (this.countDown <= 0) {
                this.gameState = 1;
                this.gameServer.start();
            }
        } else if (this.gameState == 0 && !this.isAutoStart) {
            this.gameServer.kiemTraTuDongBatDau(2);
        } else if (this.gameState == 1) {
            --this.countDown;
            if (this.countDown <= 0) {
                if (this.gameAction == 0) {
                    this.gameServer.selectDealer();
                } else if (this.gameAction == 1) {
                    this.chiaBai();
                } else if (this.gameAction == 3) {
                    this.newRound();
                } else if (this.gameAction == 5) {
                    this.changeTurn();
                } else if (this.gameAction == 4) {
                    this.tudongChoi();
                }
            } else if (this.gameAction == 4 && this.countDown <= 10 - this.botCountDown) {
                this.botPlay();
            }
        } else if (this.gameState == 3) {
            --this.countDown;
            if (this.countDown <= 0) {
                this.gameServer.pPrepareNewGame();
            }
        }
    }

    public void notifyAutoStartToUsers(int after) {
        SendUpdateAutoStart msg = new SendUpdateAutoStart();
        msg.isAutoStart = this.isAutoStart;
        msg.autoStartTime = (byte)after;
        this.gameServer.send(msg);
    }

    public void newRound() {
        this.gameServer.newRound();
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

    private void chiaBai() {
        GamePlayer gp;
        int i;
        List<GroupCard> cards = this.game.suit.dealCards();
        LinkedList<ComboGroupCard> sorted = new LinkedList<ComboGroupCard>();
        for (int i2 = 0; i2 < 6; ++i2) {
            ComboGroupCard cb = new ComboGroupCard();
            cb.handCards = cards.get(i2);
            cb.communityCards = cards.get(6);
            sorted.add(cb);
        }
        Collections.sort(sorted, ComboGroupCard.TANG);
        int to = 5;
        ComboGroupCard bestGroup = (ComboGroupCard)sorted.get(to);
        Collections.shuffle(sorted);
        int chair = -1;
        if (this.gameServer.getTicket() > 10000) {
            ArrayList<Integer> listBot = new ArrayList<Integer>();
            for (i = 0; i < 6; ++i) {
                gp = this.gameServer.playerList.get(i);
                if (gp.getUser() == null || !gp.getUser().isBot()) continue;
                listBot.add(i);
            }
            if (listBot.size() > 0) {
                Random rd = new Random();
                chair = (Integer)listBot.get(rd.nextInt(listBot.size()));
                gp = this.gameServer.playerList.get(chair);
                gp.addCards(bestGroup.handCards);
            }
        }
        if (chair >= 0) {
            sorted.remove(bestGroup);
        }
        int from = 0;
        for (i = 0; i < 6; ++i) {
            gp = this.gameServer.playerList.get(i);
            if (i == chair) continue;
            gp.addCards(((ComboGroupCard)sorted.get((int)from++)).handCards);
        }
        this.game.pokerGameInfo.addPublicCard(cards.get(6));
        this.gameServer.chiabai();
        this.gameAction = 5;
        this.countDown = (int)Math.ceil(0.5 * (double)this.gameServer.playingCount) + 2;
    }

    private void chiaBaiFair() {
        List<GroupCard> cards = this.game.suit.dealCards();
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.gameServer.playerList.get(i);
            gp.addCards(cards.get(i));
        }
        this.game.pokerGameInfo.addPublicCard(cards.get(6));
        this.gameServer.chiabai();
        this.gameAction = 5;
        this.countDown = (int)Math.ceil(0.5 * (double)this.gameServer.playingCount) + 2;
    }

    private void tudongChoi() {
        this.gameServer.tudongChoi();
    }

    private void botPlay() {
        this.gameServer.botAutoPlay();
    }

    private void changeTurn() {
        this.gameServer.changeTurn();
    }

    public int currentChair() {
        return this.currentChair;
    }

    public boolean canOutRoom() {
        return this.getGameState() == 0;
    }
}

