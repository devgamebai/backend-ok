/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  bitzero.server.extensions.data.BaseMsg
 *  game.modules.bot.BotManager
 *  game.utils.GameUtils
 */
package game.xizach.server;

import bitzero.server.entities.User;
import bitzero.server.extensions.data.BaseMsg;
import game.modules.bot.BotManager;
import game.utils.GameUtils;
import game.xizach.server.GameLogic;
import game.xizach.server.GamePlayer;
import game.xizach.server.XiZachGameServer;
import game.xizach.server.cmd.send.SendNotifyNoChuong;
import game.xizach.server.cmd.send.SendUpdateAutoStart;
import game.xizach.server.logic.Card;
import game.xizach.server.logic.CardSuit;
import game.xizach.server.logic.Gamble;
import game.xizach.server.logic.GroupCard;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

public class GameManager {
    public static final int GS_NO_START = 0;
    public static final int GS_GAME_PLAYING = 1;
    public static final int GS_GAME_END = 2;
    public static final int NO_ACTION = 0;
    public static final int GIAI_DOAN_1 = 1;
    public static final int GIAI_DOAN_2 = 2;
    public static final int KET_THUC_GIAI_DOAN_2 = 3;
    public static final int GIAI_DOAN_3 = 4;
    public static final int KET_THUC_GIAI_DOAN_3 = 5;
    public int roomOwnerChair = 6;
    public int roomCreatorUserId;
    public int gameState = 0;
    public int gameAction = 0;
    public int countDown = 0;
    public boolean isAutoStart = false;
    public Gamble game = new Gamble();
    public XiZachGameServer gameServer;
    public GameLogic logic = new GameLogic();

    public int getGameState() {
        return this.gameState;
    }

    public void prepareNewGame() {
        this.game.reset();
        this.isAutoStart = false;
    }

    public void gameLoop() {
        if (this.gameState == 0 && this.isAutoStart) {
            --this.countDown;
            if (this.countDown <= 0) {
                this.gameState = 1;
                this.gameServer.start();
            }
        } else if (this.gameState == 1) {
            --this.countDown;
            if (GameUtils.isBot) {
                this.gameServer.botAutoPlay();
            }
            if (this.gameAction == 2) {
                boolean hasDanBaiAll = true;
                for (int i = 0; i < 6; ++i) {
                    GamePlayer gp = this.gameServer.getPlayerByChair(i);
                    if (gp == null || !gp.isPlaying() || gp.camChuong || gp.hasDanBai) continue;
                    hasDanBaiAll = false;
                }
                if (hasDanBaiAll) {
                    this.chuyenGiaiDoan3();
                    return;
                }
            }
            if (this.gameAction == 4 && this.gameServer.needSoBaiEndGame()) {
                this.gameServer.soBaiEndGame();
                return;
            }
            if (this.countDown <= 0) {
                boolean needMoreTime;
                if (this.gameAction == 1) {
                    this.gameServer.xetBaiXiZach();
                    if (this.gameAction == 1) {
                        this.chuyenGiaiDoan2();
                    }
                } else if (this.gameAction == 2) {
                    needMoreTime = this.gameServer.tuDongRutBai();
                    if (needMoreTime) {
                        this.countDown = 2;
                        this.gameAction = 3;
                    } else {
                        this.chuyenGiaiDoan3();
                    }
                } else if (this.gameAction == 3) {
                    this.chuyenGiaiDoan3();
                } else if (this.gameAction == 4) {
                    needMoreTime = this.gameServer.tuRutBaiChuong();
                    if (needMoreTime) {
                        this.countDown = 2;
                        this.gameAction = 5;
                    } else {
                        this.gameServer.endGame();
                    }
                } else if (this.gameAction == 5) {
                    this.gameServer.endGame();
                }
            }
        } else if (this.gameState == 2) {
            --this.countDown;
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

    public void moBai() {
        this.gameServer.endGame();
    }

    public void notifyAutoStartToUsers(int after) {
        SendUpdateAutoStart msg = new SendUpdateAutoStart();
        msg.isAutoStart = this.isAutoStart;
        msg.autoStartTime = (byte)after;
        msg.chuongChair = (byte)this.gameServer.chuongChair;
        this.gameServer.send(msg);
    }

    public void cancelAutoStart() {
        this.isAutoStart = false;
        this.notifyAutoStartToUsers(0);
    }

    public void notifyKhongCoChuong() {
        SendNotifyNoChuong msg = new SendNotifyNoChuong();
        this.gameServer.send(msg);
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
        if (BotManager.instance().balanceMode == 0) {
            this.chiaBaiNgauNhien();
        } else {
            boolean isUp = BotManager.instance().balanceMode == 1;
            int x = BotManager.instance().getRandomNumber(3);
            if (x == 0 || isUp) {
                this.chiaBaiCanBang(isUp);
            } else {
                this.chiaBaiNgauNhien();
            }
        }
    }

    public void chiaBaiCanBang(boolean isUp) {
        boolean isChuongBot = true;
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.gameServer.playerList.get(i);
            if (!gp.camChuong || gp.getUser() == null || gp.getUser().isBot()) continue;
            isChuongBot = false;
            break;
        }
        if (!isChuongBot) {
            this.chiaBaiNgauNhien();
            return;
        }
        List<GroupCard> cards = this.game.suit.dealCards();
        Collections.sort(cards, GroupCard.bestGroupCardComparator);
        GroupCard chuongCard = cards.get(cards.size() - 1);
        if (isChuongBot && !isUp || !isChuongBot && isUp) {
            chuongCard = cards.get(0);
        }
        cards.remove(chuongCard);
        Collections.shuffle(cards);
        int index = 0;
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.gameServer.playerList.get(i);
            GroupCard gc = null;
            gc = gp.camChuong ? chuongCard : cards.get(index++);
            GroupCard gc1 = new GroupCard();
            for (int k = 0; k < 2; ++k) {
                gc1.addCard(gc.cards.get(k));
            }
            gp.addCards(gc1);
            gp.addStorageCard(gc);
        }
        this.gameServer.chiabai();
        this.gameAction = 1;
        this.countDown = 20;
    }

    public void chiaBaiNgauNhien() {
        List<GroupCard> cards = this.game.suit.dealCards();
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.gameServer.playerList.get(i);
            GroupCard gc = cards.get(i);
            GroupCard gc1 = new GroupCard();
            for (int k = 0; k < 2; ++k) {
                gc1.addCard(gc.cards.get(k));
            }
            gp.addCards(gc1);
            gp.addStorageCard(gc);
        }
        this.gameServer.chiabai();
        this.gameAction = 1;
        this.countDown = 20;
    }

    public void chuyenGiaiDoan2() {
        this.gameServer.chuyenGiaiDoan2();
        this.gameAction = 2;
        this.countDown = 20;
    }

    public void chuyenGiaiDoan3() {
        this.gameServer.chuyenGiaiDoan3();
        this.gameAction = 4;
        this.countDown = 30;
    }

    public boolean canOutRoom() {
        return this.getGameState() == 0;
    }
}

