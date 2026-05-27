/*
 * Decompiled with CFR 0.144.
 */
package game.poker.server.logic;

import game.poker.server.logic.Turn;
import java.util.LinkedList;

public class Round {
    public static final int PRE_FLOP = 0;
    public static final int FLOP = 1;
    public static final int TURN = 2;
    public static final int RIVER = 3;
    public int requireBigBlindCount = 0;
    public int roundId;
    public LinkedList<Turn> turns = new LinkedList();

    public void addTurn(Turn turn) {
        this.turns.add(turn);
    }

    public boolean isNewRound() {
        return this.turns.size() == 0;
    }
}

