/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.sam.server;

import bitzero.server.extensions.data.BaseMsg;
import game.sam.server.GameLogic;
import game.sam.server.GamePlayer;
import game.sam.server.SamGameServer;
import game.sam.server.cmd.send.SendChonSam;
import game.sam.server.cmd.send.SendFirstTurnDecision;
import game.sam.server.cmd.send.SendToiTrang;
import game.sam.server.cmd.send.SendUpdateAutoStart;
import game.sam.server.logic.Card;
import game.sam.server.logic.CardSuit;
import game.sam.server.logic.Gamble;
import game.sam.server.logic.GroupCard;
import java.util.List;
import java.util.Vector;

public class GameManager {
    public static final int DEM_LA = 1;
    public static final int GS_NO_START = 0;
    public static final int GS_GAME_PLAYING = 1;
    public static final int GS_GAME_END = 3;
    public static final int NO_ACTION = -1;
    public static final int KIEM_TRA_DI_DAU = 0;
    public static final int CHIA_BAI = 1;
    public static final int BAO_SAM = 2;
    public static final int TOI_TRANG = 3;
    public static final int CHOI_BAI = 4;
    public static final int MOI_DI_DAU = 5;
    public int roomOwnerChair = 5;
    public int roomCreatorUserId;
    public int rCuocLon = 1;
    public int gameState = 0;
    public int gameAction = -1;
    public int countDown = 0;
    public boolean isAutoStart = false;
    public volatile int currentChair;
    public volatile int prevChair;
    public volatile int nextChair;
    public Gamble game = new Gamble();
    public SamGameServer gameServer;
    public GameLogic logic = new GameLogic();

    public int getGameState() {
        return this.gameState;
    }

    public void prepareNewGame() {
        this.game.reset();
        this.isAutoStart = false;
        this.gameServer.kiemTraTuDongBatDau(5);
        this.gameServer.botJoinRoom();
    }

    public void gameLoop() {
        if (this.gameState == 0 && this.isAutoStart) {
            --this.countDown;
            if (this.countDown <= 0) {
                this.gameState = 1;
                this.gameServer.start();
            }
        } else if (this.gameState == 1) {
            if (this.gameAction != -1) {
                --this.countDown;
                if (this.gameAction == 2 && this.countDown == 5) {
                    this.gameServer.botBaoSam();
                }
                if (this.gameAction == 4 && this.countDown == 17) {
                    this.gameServer.botAutoPlay();
                }
                if (this.countDown <= 0) {
                    if (this.gameAction == 0) {
                        this.kiemTraDiDau();
                    } else if (this.gameAction == 1) {
                        this.chiaBai();
                    } else if (this.gameAction == 2) {
                        if (this.countDown == 5) {
                            this.gameServer.botBaoSam();
                        }
                        this.chonSam();
                    } else if (this.gameAction == 5) {
                        this.moididau(20);
                    } else if (this.gameAction == 3) {
                        this.toitrang();
                    } else if (this.gameAction == 4) {
                        this.tudongChoi();
                    }
                }
            }
        } else if (this.gameState == 3) {
            --this.countDown;
            if (this.countDown == 5) {
                this.gameServer.notifyNoHu();
            }
            if (this.countDown <= 0) {
                this.gameServer.pPrepareNewGame();
            }
        } else {
            this.gameServer.botJoinRoom();
        }
    }

    public void notifyAutoStartToUsers(int after) {
        SendUpdateAutoStart msg = new SendUpdateAutoStart();
        msg.isAutoStart = this.isAutoStart;
        msg.autoStartTime = (byte)after;
        this.gameServer.sendMsg(msg);
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

    private void kiemTraDiDau() {
        SendFirstTurnDecision msg = new SendFirstTurnDecision();
        int firstChair = this.gameServer.isNeedRandomFirstTurn();
        if (firstChair >= 0) {
            msg.isRandom = false;
            this.countDown = 0;
        } else {
            msg.isRandom = true;
            msg.cards = this.logic.genFirstTurn();
            this.xacDinhDiDau(msg.cards);
            this.countDown = 3;
        }
        this.currentChair = this.logic.firstTurn;
        msg.chair = (byte)this.logic.firstTurn;
        this.gameServer.logQuyetDinhDiDau(msg);
        this.gameServer.sendMsgToPlayingUser(msg);
        this.gameAction = 1;
    }

    private void xacDinhDiDau(byte[] cards) {
        boolean flag = false;
        for (int i = 0; i < 5; ++i) {
            GamePlayer gp = this.gameServer.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            if (!flag) {
                this.logic.firstTurn = i;
                flag = true;
                continue;
            }
            if (Card.sosanhLabai(cards[i], cards[this.logic.firstTurn]) <= 0) continue;
            this.logic.firstTurn = i;
        }
    }

    private void chiaBai() {
        List<GroupCard> cards = this.game.suit.dealCards();
        for (int i = 0; i < 5; ++i) {
            GamePlayer gp = this.gameServer.playerList.get(i);
            gp.addCards(cards.get(i));
        }
        this.gameServer.chiabai();
        this.gameAction = 2;
        this.countDown = 12;
    }

    public void moididau(int countdown) {
        this.gameServer.notifyChangeTurn(true);
        this.gameAction = 4;
        this.countDown = countdown;
    }

    private void chonSam() {
        byte chair = this.gameServer.getBaoSam();
        if (chair >= 0 && chair < 5) {
            GamePlayer gp = this.gameServer.getPlayerByChair(chair);
            if (gp.kiemTraToiTrang() > 0) {
                this.game.toitrang = true;
                this.gameServer.endGame();
            } else {
                SendChonSam msg = new SendChonSam();
                msg.baosam = true;
                msg.chair = chair;
                this.gameServer.sendMsg(msg);
                this.gameAction = 5;
                this.countDown = 3;
            }
        } else {
            SendChonSam msg = new SendChonSam();
            msg.baosam = false;
            msg.chair = (byte)this.currentChair;
            this.gameServer.sendMsg(msg);
            this.gameAction = 5;
            this.countDown = 0;
        }
    }

    private void toitrang() {
        byte chair = this.gameServer.getToiTrang();
        if (chair >= 0) {
            SendToiTrang msg = new SendToiTrang();
            msg.chair = chair;
            this.gameServer.sendMsg(msg);
            this.game.toitrang = true;
            this.gameServer.endGame();
        } else {
            this.moididau(20);
        }
    }

    private void tudongChoi() {
        this.gameServer.tudongChoi();
    }

    public int currentChair() {
        return this.currentChair;
    }

    public boolean canOutRoom() {
        return this.getGameState() == 0;
    }
}

