/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.Debug
 */
package game.poker.server.logic;

import bitzero.util.common.business.Debug;
import game.poker.server.logic.Card;
import game.poker.server.logic.GroupCard;
import game.poker.server.logic.PokerResult;
import java.util.List;

public class PokerGameInfo {
    public PokerResult resultPoker;
    public long potMoney;
    public long lastRaise;
    public long maxBetMoney;
    public boolean raiseBlock;
    public GroupCard publicCard = null;
    public long bigBlindMoney;
    public int bigBlind;
    public int smallBlind;
    public int dealer = -1;

    public void clearNewGame() {
        this.potMoney = 0L;
        this.lastRaise = 0L;
        this.maxBetMoney = 0L;
        this.raiseBlock = false;
        this.publicCard = null;
        this.resultPoker = new PokerResult();
    }

    public void clearNewRound() {
        this.lastRaise = 0L;
        this.maxBetMoney = 0L;
        this.raiseBlock = false;
    }

    public void addPublicCard(GroupCard groupCard) {
        Debug.trace((Object[])new Object[]{"addPublicCard", groupCard});
        this.publicCard = groupCard;
    }

    public byte[] getPublicCard(int roundId) {
        Debug.trace((Object[])new Object[]{"getPublicCard roundId=", roundId});
        if (roundId == 0) {
            return new byte[0];
        }
        if (roundId == 1) {
            byte[] cards = new byte[3];
            for (int i = 0; i < 3; ++i) {
                Card c = this.publicCard.cards.get(i);
                cards[i] = (byte)c.ID;
            }
            return cards;
        }
        byte[] cards = new byte[1];
        int index = roundId + 1;
        Card c = this.publicCard.cards.get(index);
        cards[0] = (byte)c.ID;
        return cards;
    }

    public byte[] getCurrentPublicCard(int roundId) {
        Debug.trace((Object[])new Object[]{"getPublicCard roundId=", roundId});
        if (roundId == 0) {
            return new byte[0];
        }
        int size = 2 + roundId;
        byte[] cards = new byte[size];
        for (int i = 0; i < size; ++i) {
            Card c = this.publicCard.cards.get(i);
            cards[i] = (byte)c.ID;
        }
        return cards;
    }

    public GroupCard getGroupCardPublic(int roundId) {
        return new GroupCard(this.getCurrentPublicCard(roundId));
    }
}

