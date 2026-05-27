/*
 * Decompiled with CFR 0.144.
 */
package game.tour.server;

import game.tour.server.logic.GroupCard;
import game.tour.server.logic.PokerPlayerInfo;
import game.tour.server.logic.Turn;
import java.util.LinkedList;
import java.util.List;

public class sPlayerInfo {
    public GroupCard handCards;
    public GroupCard maxCards;
    public PokerPlayerInfo pokerInfo;
    public List<Turn> turns = new LinkedList<Turn>();

    public void clear() {
        this.turns.clear();
        this.handCards = null;
        this.maxCards = null;
    }

    public boolean kiemTraNoHu() {
        return false;
    }

    public long getTotalMoneyBet() {
        long x = 0L;
        for (Turn turn : this.turns) {
            x += turn.raiseAmount;
        }
        return x;
    }
}

