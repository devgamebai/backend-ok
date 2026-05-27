/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.Debug
 */
package game.sam.server.logic;

import bitzero.util.common.business.Debug;
import game.sam.server.logic.CardSuit;
import game.sam.server.logic.Round;
import game.utils.GameUtils;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

public class Gamble {
    public static final AtomicInteger autoGen = new AtomicInteger(0);
    public CardSuit suit = new CardSuit();
    public LinkedList<Round> gameRounds = new LinkedList();
    public boolean baosam = false;
    public boolean toitrang = false;
    public int id = Gamble.getID();
    public boolean isCheat = false;

    private static int getID() {
        int id = autoGen.getAndIncrement();
        return id;
    }

    public void makeRound() {
        Round round = new Round();
        this.gameRounds.add(round);
    }

    public Round getCurrentRound() {
        if (this.gameRounds.size() == 0) {
            Debug.trace("Make first round in game");
            this.makeRound();
        }
        return this.gameRounds.getLast();
    }

    public Round getLastRound() {
        if (this.gameRounds.size() == 0) {
            return null;
        }
        return this.gameRounds.getLast();
    }

    public void reset() {
        this.id = Gamble.getID();
        if (!this.isCheat || !GameUtils.isCheat) {
            this.suit.setRandom();
        }
        this.gameRounds.clear();
        this.baosam = false;
        this.toitrang = false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.id).append("/");
        sb.append("Baosam").append(this.baosam).append("/");
        sb.append("Toitrang").append(this.toitrang).append("/");
        for (int i = 0; i < this.gameRounds.size(); ++i) {
            Round r = this.gameRounds.get(i);
            sb.append(r.toString()).append("/");
        }
        return sb.toString();
    }
}

