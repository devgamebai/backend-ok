/*
 * Decompiled with CFR 0.144.
 */
package game.bacay.server;

import game.bacay.server.logic.GroupCard;

public class sPlayerInfo {
    public GroupCard handCards;

    public void clearInfo() {
        this.handCards = null;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("handCards: ").append(this.handCards.toString()).append("\n");
        return sb.toString();
    }

    boolean kiemTraNoHu() {
        if (this.handCards != null) {
            return this.handCards.isNoHu();
        }
        return false;
    }
}

