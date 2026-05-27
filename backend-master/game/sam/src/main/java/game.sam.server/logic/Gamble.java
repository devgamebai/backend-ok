/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.Debug
 *  game.modules.gameRoom.entities.GameRoomIdGenerator
 *  game.utils.GameUtils
 */
package game.sam.server.logic;

import bitzero.util.common.business.Debug;
import game.modules.gameRoom.entities.GameRoomIdGenerator;
import game.sam.server.logic.CardSuit;
import game.sam.server.logic.Round;
import game.sam.server.logic.Turn;
import game.utils.GameUtils;
import java.util.LinkedList;

public class Gamble {
    public CardSuit suit = new CardSuit();
    public LinkedList<Round> gameRounds = new LinkedList();
    public boolean baosam = false;
    public boolean toitrang = false;
    public int id = Gamble.getID();
    public boolean isCheat = false;
    public long logTime = System.currentTimeMillis();

    private static int getID() {
        int id = GameRoomIdGenerator.instance().getId();
        return id;
    }

    public void makeRound() {
        Round round = new Round();
        this.gameRounds.add(round);
    }

    public Round getCurrentRound() {
        if (this.gameRounds.size() == 0) {
            Debug.trace((Object)"Make first round in game");
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
        this.logTime = System.currentTimeMillis();
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

    public boolean isNewRound() {
        Round r = this.getLastRound();
        if (r != null) {
            return r.turns.size() == 0;
        }
        return true;
    }
}

