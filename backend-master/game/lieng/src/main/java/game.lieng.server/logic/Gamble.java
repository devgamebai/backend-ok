/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  game.modules.gameRoom.entities.GameRoomIdGenerator
 *  game.utils.GameUtils
 */
package game.lieng.server.logic;

import game.lieng.server.logic.CardSuit;
import game.lieng.server.logic.LiengGameInfo;
import game.lieng.server.logic.Round;
import game.lieng.server.logic.Turn;
import game.modules.gameRoom.entities.GameRoomIdGenerator;
import game.utils.GameUtils;
import java.util.LinkedList;

public class Gamble {
    public CardSuit suit = new CardSuit();
    public LinkedList<Round> gameRounds = new LinkedList();
    public LiengGameInfo pokerGameInfo = new LiengGameInfo();
    public int id = Gamble.getID();
    public boolean isCheat = false;
    public long logTime = System.currentTimeMillis();
    public boolean dealCard = false;
    public long[] moneyArray = new long[9];
    public int dealer = 0;

    private static int getID() {
        int id = GameRoomIdGenerator.instance().getId();
        return id;
    }

    public Round makeRound() {
        Round round = new Round();
        if (this.gameRounds.size() == 0) {
            round.roundId = 0;
        } else {
            Round lastRound = this.getLastRound();
            round.roundId = lastRound.roundId + 1;
        }
        this.gameRounds.add(round);
        return round;
    }

    public Round getCurrentRound() {
        if (this.gameRounds.size() == 0) {
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
        this.pokerGameInfo.clearNewGame();
        this.dealCard = false;
        this.gameRounds.clear();
    }

    public boolean isNewRound() {
        Round r = this.getLastRound();
        if (r != null) {
            return r.turns.size() == 0;
        }
        return true;
    }
}

