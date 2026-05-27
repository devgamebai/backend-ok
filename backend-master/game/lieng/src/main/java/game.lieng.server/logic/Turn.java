/*
 * Decompiled with CFR 0.144.
 */
package game.lieng.server.logic;

import game.lieng.server.logic.LiengPlayerInfo;

public class Turn {
    public static final int FOLD = 0;
    public static final int CHECK = 1;
    public static final int CALL = 2;
    public static final int RAISE = 3;
    public static final int ALLIN = 4;
    public LiengPlayerInfo PokerInfo;
    public int action;
    public long raiseAmount;
    public int chair = 0;

    public Turn() {
    }

    public Turn(int chair, int action, long raiseAmmount) {
        this.chair = chair;
        this.action = action;
        this.raiseAmount = raiseAmmount;
    }

    public static Turn makeTurn(LiengPlayerInfo PokerInfo, int action, long raiseAmount) {
        Turn turn = new Turn();
        turn.action = action;
        turn.raiseAmount = raiseAmount;
        turn.PokerInfo = PokerInfo;
        return turn;
    }

    public Turn(String data) {
        String[] v0;
        String[] v1;
        String[] v = data.split(">");
        if (v.length >= 1 && (v0 = v[0].split("<")).length >= 2 && (v1 = v0[1].split(";")).length == 3) {
            this.chair = Integer.parseInt(v1[0]);
            this.action = Integer.parseInt(v1[1]);
            this.raiseAmount = Integer.parseInt(v1[2]);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("chair:").append(this.chair);
        sb.append(";action:").append(this.action);
        sb.append(";raiseAmount:").append(this.raiseAmount);
        return sb.toString();
    }
}

