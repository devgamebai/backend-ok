/*
 * Decompiled with CFR 0.144.
 */
package game.tour.server.logic;

import java.util.Comparator;

public class ComboGroupCard {
    public GroupCard handCards;
    public GroupCard communityCards;
    public static final Comparator<ComboGroupCard> TANG = new Comparator<ComboGroupCard>(){

        @Override
        public int compare(ComboGroupCard cb1, ComboGroupCard cb2) {
            return PokerRule.soSanhBoBai(cb1.getBest(), cb2.getBest());
        }
    };

    public GroupCard getBest() {
        return PokerRule.findMaxGroup(this.handCards, this.communityCards);
    }

}

