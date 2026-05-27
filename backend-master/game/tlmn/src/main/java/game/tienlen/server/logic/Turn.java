/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  game.entities.PlayerInfo
 */
package game.tienlen.server.logic;

import game.tienlen.server.GamePlayer;

public class Turn {
    public static final int INVALID_TURN = 1;
    public static final int BEGIN_TURN = 1;
    public static final int MID_TURN = 2;
    public static final int LAST_TURN = 3;
    public GamePlayer owner;
    public GroupCard throwCard;
    public int turnType = 1;

    public boolean isFinish() {
        return this.turnType == 3;
    }

    public boolean isBegin() {
        return this.turnType == 1;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.owner.pInfo != null && this.throwCard != null) {
            sb.append(this.owner.pInfo.nickName);
            sb.append(this.throwCard.toString());
        }
        return sb.toString();
    }
}

