/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  game.modules.bot.BotManager
 */
package game.xizach.server;

import game.modules.bot.BotManager;
import game.xizach.server.logic.Card;
import game.xizach.server.logic.GroupCard;
import java.util.List;

public class sPlayerInfo {
    public GroupCard handCards;
    public GroupCard storageCards = null;

    public void clearInfo() {
        this.handCards = null;
        this.storageCards = null;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("handCards: ").append(this.handCards.toString()).append("\n");
        sb.append("storageCards: ").append(this.storageCards.toString()).append("\n");
        return sb.toString();
    }

    public Card rutCard() {
        int curIndex = this.handCards.cards.size();
        this.handCards.addCard(this.storageCards.cards.get(curIndex));
        return this.storageCards.cards.get(curIndex);
    }

    public int kiemTraBotCanRutBai(boolean camChuong) {
        int random = -1;
        boolean stop = this.checkStop(camChuong);
        if (stop) {
            return random;
        }
        GroupCard best = this.storageCards.findBestGroupCard();
        int remain = best.cards.size() - this.handCards.cards.size();
        if (remain > 0) {
            boolean checkDannon = this.handCards.isDanNon();
            random = checkDannon ? (camChuong ? 2 : 1) : BotManager.instance().getRandomNumber(4) + 3;
        }
        return random;
    }

    private boolean checkStop(boolean camChuong) {
        if (!camChuong && this.handCards.cards.size() == 2 && this.handCards.getMaxDiem() >= 16 && this.handCards.getMaxDiem() <= 18) {
            Card c1 = this.handCards.cards.get(0);
            Card c2 = this.handCards.cards.get(0);
            int random = BotManager.instance().getRandomNumber(10);
            return (c1.isXi() != false || c2.isXi() != false) && random >= 7;
        }
        if (!camChuong && this.handCards.getMaxDiem() >= 16) {
            return true;
        }
        return camChuong && this.handCards.getMaxDiem() >= 18;
    }
}

