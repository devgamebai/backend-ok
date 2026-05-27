/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  org.apache.log4j.PropertyConfigurator
 */
package game.tour.server.logic;

import java.util.List;

import com.vinplay.vbee.common.config.VBeePath;
import org.apache.log4j.PropertyConfigurator;

public class PokerRule {

    private static String basePath = VBeePath.basePath;

    public static void main(String[] args) {
        PropertyConfigurator.configure(basePath.concat("config/log4j.properties"));
        CardSuit suit = new CardSuit();
        int c = 0;
        do {
            suit.setRandom();
            List<GroupCard> cards = suit.dealCards();
            GroupCard communityCard = cards.get(6);
            for (int i = 0; i < 6; ++i) {
                GroupCard playerCard = cards.get(i);
                GroupCard groupCard = PokerRule.findMaxGroup(playerCard, communityCard);
            }
        } while (++c != 100);
    }

    public static GroupCard findMaxGroup(GroupCard playerCard, GroupCard communityCard) {
        if (communityCard.cards.size() == 0) {
            return playerCard;
        }
        GroupCard totalCard = playerCard.addCards(communityCard);
        GroupCard result = PokerRule.findMaxBruceForce(totalCard, 5);
        return result;
    }

    public static GroupCard findMaxBruceForce(GroupCard fullSize, int size) {
        if (fullSize.cards.size() < size) {
            return fullSize;
        }
        List<Integer> idList = fullSize.toIntegerList();
        CombinationGen gen = new CombinationGen(idList, size);
        List<Integer[]> allGroup = gen.getCombinations();
        GroupCard maxCard = new GroupCard(allGroup.get(0));
        for (int i = 1; i < allGroup.size(); ++i) {
            GroupCard nextCard = new GroupCard(allGroup.get(i));
            if (PokerRule.soSanhBoBai(nextCard, maxCard) <= 0) continue;
            maxCard = nextCard;
        }
        return maxCard;
    }

    public static synchronized int soSanhBoBai(GroupCard gc1, GroupCard gc2) {
        if (gc1.kiemtraBo() < gc2.kiemtraBo()) {
            return 1;
        }
        if (gc1.kiemtraBo() > gc2.kiemtraBo()) {
            return -1;
        }
        if (gc1.kiemtraBo() == gc2.kiemtraBo()) {
            switch (gc1.kiemtraBo()) {
                case 1: 
                case 5: {
                    for (int i = gc1.cards.size() - 1; i >= 0; --i) {
                        Card c1 = gc1.cards.get(i);
                        Card c2 = gc2.cards.get(i);
                        if (c1.SO > c2.SO) {
                            return 1;
                        }
                        if (c1.SO >= c2.SO) continue;
                        return -1;
                    }
                }
                case 2: 
                case 3: 
                case 4: 
                case 6: 
                case 7: 
                case 8: 
                case 9: {
                    int size = gc1.cards.size();
                    for (int i = 0; i < size; ++i) {
                        Card c1 = gc1.cards.get(i);
                        Card c2 = gc2.cards.get(i);
                        if (c1.SO > c2.SO) {
                            return 1;
                        }
                        if (c1.SO >= c2.SO) continue;
                        return -1;
                    }
                }
            }
        }
        return 0;
    }
}

