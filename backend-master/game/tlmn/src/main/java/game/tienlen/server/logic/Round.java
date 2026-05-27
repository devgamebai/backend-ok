/*
 * Decompiled with CFR 0.144.
 */
package game.tienlen.server.logic;

import game.tienlen.server.GamePlayer;

import java.util.LinkedList;

public class Round {
    public LinkedList<Turn> turns = new LinkedList();
    public int soLaPhatChatChong = 0;
    public int soLaChatChong = 0;
    public int phatChatChong = 0;

    public boolean biPhatChatChong() {
        return this.phatChatChong > 0 && this.soLaPhatChatChong > 0;
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

    public boolean duocDanhBonDoiThongSaiLuot(GamePlayer gp) {
        if (this.soLaChatChong == 0) {
            return false;
        }
        Turn turn = this.getPrevTurn();
        if (turn == null) {
            return false;
        }
        return turn.owner == null || turn.owner.chair != gp.chair;
    }

    public boolean isNewRound() {
        return this.turns.size() == 0;
    }
}

