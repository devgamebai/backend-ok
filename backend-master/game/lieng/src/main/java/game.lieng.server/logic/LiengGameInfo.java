/*
 * Decompiled with CFR 0.144.
 */
package game.lieng.server.logic;

import game.lieng.server.logic.GroupCard;
import game.lieng.server.logic.LiengResult;

public class LiengGameInfo {
    public LiengResult resultPoker;
    public long potMoney;
    public long lastRaise;
    public long maxBetMoney;
    public boolean raiseBlock;
    public GroupCard publicCard = null;
    public long bigBlindMoney;
    public int bigBlind;
    public int smallBlind;
    public int dealer = 0;

    public void clearNewGame() {
        this.potMoney = 0L;
        this.lastRaise = 0L;
        this.maxBetMoney = 0L;
        this.raiseBlock = false;
        this.publicCard = null;
        this.resultPoker = new LiengResult();
    }

    public void clearNewRound() {
        this.lastRaise = 0L;
        this.maxBetMoney = 0L;
        this.raiseBlock = false;
    }
}

