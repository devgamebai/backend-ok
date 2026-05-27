/*
 * Decompiled with CFR 0.144.
 */
package game.binh.server;

import game.binh.server.logic.Card;
import game.binh.server.logic.GroupCard;
import game.binh.server.logic.PlayerCard;
import java.util.List;

public class sPlayerInfo {
    public GroupCard handCards;
    public PlayerCard sorttedCard = new PlayerCard();

    public void clearInfo() {
        this.handCards = null;
        this.sorttedCard = new PlayerCard();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("sorttedCard: ").append(this.sorttedCard.toString()).append("\n");
        return sb.toString();
    }

    public int getKind(int rule) {
        return this.sorttedCard.GetPlayerCardsKind(rule);
    }

    public boolean checkSubCard(GroupCard chi, GroupCard handscard, GroupCard newHandCard) {
        int count = 0;
        block0 : for (Card c : chi.cards) {
            for (Card c1 : this.handCards.cards) {
                if (c.ID != c1.ID) continue;
                newHandCard.AddCard(c1);
                ++count;
                continue block0;
            }
        }
        return count == chi.GetNumOfCards();
    }

    public boolean checkCardValid(GroupCard chi1, GroupCard chi2, GroupCard chi3) {
        boolean result;
        if (chi1.cards.size() != 5 && chi2.cards.size() != 5 && chi3.cards.size() != 3) {
            return false;
        }
        GroupCard newHandCard = new GroupCard();
        for (Card c : chi1.cards) {
            newHandCard.AddCard(c);
        }
        for (Card c : chi2.cards) {
            newHandCard.AddCard(c);
        }
        for (Card c : chi3.cards) {
            newHandCard.AddCard(c);
        }
        int count = 0;
        block3 : for (int i = 0; i < newHandCard.cards.size(); ++i) {
            Card c = newHandCard.cards.get(i);
            for (int j = 0; j < this.handCards.cards.size(); ++j) {
                Card c1 = this.handCards.cards.get(j);
                if (c.ID != c1.ID) continue;
                ++count;
                continue block3;
            }
        }
        boolean bl = result = count == this.handCards.cards.size();
        if (result) {
            this.handCards = newHandCard;
            return true;
        }
        return false;
    }

    public int autoSort(int rule) {
        if (rule == 0) {
            this.sorttedCard.autoSort1();
        }
        if (rule == 1) {
            this.sorttedCard.autoSort2();
        }
        return this.getKind(rule);
    }

    public boolean hasTuQuyAt(int rule) {
        if (this.sorttedCard.GetPlayerCardsKind(rule) == 6 || this.sorttedCard.GetPlayerCardsKind(rule) == 7) {
            for (int i = 1; i <= 2; ++i) {
                GroupCard chi = this.sorttedCard.getChi(i);
                if (!chi.coTuQuyAt()) continue;
                return true;
            }
        }
        return false;
    }

    int demSoAt() {
        if (this.handCards != null) {
            return this.handCards.demSoAt();
        }
        return 0;
    }
}

