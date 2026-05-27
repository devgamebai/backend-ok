/*
 * Decompiled with CFR 0.144.
 */
package game.sam.server.logic;

import game.sam.server.logic.Turn;
import java.util.LinkedList;

public class Round {
    public LinkedList<Turn> turns = new LinkedList();
    public int soLaPhatChatChong = 0;
    public int soLaChatChong = 0;
    public int chatchong = 0;

    public boolean biPhatChatChong() {
        return this.chatchong >= 2;
    }

    public void addTurn(Turn turn) {
        this.turns.addLast(turn);
    }

    public Turn getPrevTurn() {
        if (this.turns.size() == 0) {
            return null;
        }
        return this.turns.getLast();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.turns.size(); ++i) {
            sb.append(this.turns.get(i).toString()).append("]");
        }
        sb.append("SoLaPhat:").append(this.soLaPhatChatChong);
        return sb.toString();
    }
}

