/*
 * Decompiled with CFR 0.144.
 */
package game.sam.server.logic;

import game.entities.PlayerInfo;
import game.sam.server.GamePlayer;
import game.sam.server.logic.GroupCard;

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

