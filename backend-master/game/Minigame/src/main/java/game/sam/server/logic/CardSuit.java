/*
 * Decompiled with CFR 0.144.
 */
package game.sam.server.logic;

import game.sam.server.logic.Card;
import game.sam.server.logic.GroupCard;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CardSuit {
    public static final int MAX_PLAYERS = 5;
    public static final int SOLO_PLAYERS = 2;
    public static final int MAX_NUMBER_OF_CARDS = 10;
    public List<Byte> ids = new ArrayList<Byte>();
    public List<Byte> chat = new ArrayList<Byte>();

    public CardSuit() {
        for (byte i = 0; i < 52; i = (byte)(i + 1)) {
            this.ids.add(i);
        }
        Collections.shuffle(this.ids);
    }

    public boolean setOrder(byte[] cards) {
        if (cards.length != 52) {
            return false;
        }
        this.ids.clear();
        for (int i = 0; i < cards.length; ++i) {
            this.ids.add(cards[i]);
        }
        return true;
    }

    public void setRandomFirstTurn() {
        Collections.shuffle(this.ids);
    }

    public void setRandom() {
        Collections.shuffle(this.ids);
    }

    public void removeRandom() {
        Collections.sort(this.ids);
    }

    public List<GroupCard> dealCards() {
        ArrayList<GroupCard> groupCards = new ArrayList<GroupCard>();
        byte[] cards = new byte[10];
        int curentIndex = 0;
        for (int i = 0; i < 5; ++i) {
            for (int j = 0; j < 10; ++j) {
                cards[j] = this.ids.get(curentIndex++);
            }
            GroupCard gc = new GroupCard(cards);
            groupCards.add(gc);
        }
        return groupCards;
    }

    public String toCardString(int size) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; ++i) {
            Card c = new Card(this.ids.get(i));
            sb.append(c.toString());
        }
        return sb.toString();
    }
}

