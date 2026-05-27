/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.tienlen.server;

import game.tienlen.server.cmd.send.SendFirstTurnDecision;
import game.tienlen.server.cmd.send.SendToiTrang;
import game.tienlen.server.cmd.send.SendUpdateAutoStart;
import game.tienlen.server.logic.Card;
import game.tienlen.server.logic.Gamble;
import game.tienlen.server.logic.GroupCard;
import java.util.List;

public class GameManager {
    public static final int DEM_LA = 1;
    public static final int GS_NO_START = 0;
    public static final int GS_GAME_PLAYING = 1;
    public static final int GS_GAME_END = 3;
    public static final int NO_ACTION = -1;
    public static final int KIEM_TRA_DI_DAU = 0;
    public static final int CHIA_BAI = 1;
    public static final int TOI_TRANG = 2;
    public static final int CHOI_BAI = 3;
    public static final int MOI_DI_DAU = 4;
    public static final int CHO_BON_DOI_THONG = 5;
    public static final int DANH_BON_DOI_THONG = 6;
    public int roomOwnerChair = 4;
    public int roomCreatorUserId;
    public int rCuocLon = 1;
    public int gameState = 0;
    public int gameAction = -1;
    public int countDown = 0;
    public boolean isAutoStart = false;
    public volatile int currentChair;
    public volatile int prevChair;
    public volatile int prevChairThrowCard = 4;
    public volatile int nextChair;
    public Gamble game = new Gamble();
    public TienlenGameServer gameServer;
    public GameLogic logic = new GameLogic();

    public int getGameState() {
        return this.gameState;
    }

    public void prepareNewGame() {
        this.game.reset();
        this.isAutoStart = false;
        this.prevChairThrowCard = 4;
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
            if (this.gameAction != -1) {
                --this.countDown;
                if (this.countDown <= 0) {
                    if (this.gameAction == 0) {
                        this.kiemTraDiDau();
                    } else if (this.gameAction == 1) {
                        this.chiaBai();
                    } else if (this.gameAction == 2) {
                        this.toitrang();
                    } else if (this.gameAction == 3) {
                        this.tudongChoi();
                    } else if (this.gameAction == 5) {
                        this.gameServer.changeTurnNewRound();
                    } else if (this.gameAction == 6) {
                        this.gameServer.ketThucDanhBonDoiThong();
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
        for (int i = 0; i < 4; ++i) {
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
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.gameServer.playerList.get(i);
            gp.addCards(cards.get(i));
        }
        this.gameServer.chiabai();
        this.gameAction = 2;
        this.countDown = 2;
    }

    public void moididau(int countdown) {
        this.gameServer.notifyChangeTurn(true);
        this.gameAction = 3;
        this.countDown = countdown;
    }

    private void toitrang() {
        byte chair = this.gameServer.getToiTrang();
        if (chair >= 0) {
            SendToiTrang msg = new SendToiTrang();
            msg.chair = chair;
            this.gameServer.send(msg);
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

