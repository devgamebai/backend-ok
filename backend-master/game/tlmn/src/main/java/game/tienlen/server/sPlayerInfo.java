/*
 * Decompiled with CFR 0.144.
 */
package game.tienlen.server;

import game.tienlen.server.logic.Card;
import game.tienlen.server.logic.GroupCard;
import java.util.ArrayList;
import java.util.List;

public class sPlayerInfo {
    public GroupCard handCards;
    public long moneyWin = 0L;
    public List<GroupCard> thrownCards = new ArrayList<GroupCard>();

    public boolean isEndCards() {
        return this.handCards.cards.size() == 0;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Trentay: ").append(this.handCards.toString()).append("\n");
        sb.append("DanhRa: ");
        for (GroupCard gc : this.thrownCards) {
            sb.append(gc.toString()).append("\n");
        }
        return sb.toString();
    }

    public boolean kiemTraNoHu() {
        return this.handCards.isNoHu();
    }
}

