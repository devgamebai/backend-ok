/*
 * Decompiled with CFR 0.144.
 */
package game.binh.server.logic;

import game.binh.server.logic.CardSuit;
import game.binh.server.logic.GroupCard;
import game.binh.server.logic.PlayerCard;
import java.io.PrintStream;
import java.util.List;

public class LogicTest {
    public static int count = 0;

    public static void main(String[] args) {
        for (int i = 0; i < 1000; ++i) {
            LogicTest.test(5);
        }
        System.out.println(count);
    }

    public static void test(int groupType) {
        CardSuit suit = new CardSuit();
        List<GroupCard> cards = suit.dealCards(0);
        GroupCard gc1 = cards.get(0);
        GroupCard gc2 = cards.get(1);
        GroupCard gc3 = cards.get(2);
        GroupCard gc4 = cards.get(3);
        PlayerCard pc1 = new PlayerCard();
        pc1.ApplyNewGroupCards(gc1, 0);
        pc1.autoSort2();
        PlayerCard pc2 = new PlayerCard();
        pc2.ApplyNewGroupCards(gc2, 0);
        pc2.autoSort2();
        PlayerCard pc3 = new PlayerCard();
        pc3.ApplyNewGroupCards(gc3, 0);
        pc3.autoSort2();
        PlayerCard pc4 = new PlayerCard();
        pc4.ApplyNewGroupCards(gc4, 0);
        pc4.autoSort2();
        if (pc1.GetPlayerCardsKind(1) == groupType) {
            System.out.println("===============================================");
            System.out.println(pc1.fullCard);
            System.out.println("===============================================");
            ++count;
        }
        if (pc2.GetPlayerCardsKind(1) == groupType) {
            System.out.println("===============================================");
            System.out.println(pc2.fullCard);
            System.out.println("===============================================");
            ++count;
        }
        if (pc3.GetPlayerCardsKind(1) == groupType) {
            System.out.println("===============================================");
            System.out.println(pc3.fullCard);
            System.out.println("===============================================");
            ++count;
        }
        if (pc4.GetPlayerCardsKind(1) == groupType) {
            System.out.println("===============================================");
            System.out.println(pc4.fullCard);
            System.out.println("===============================================");
            ++count;
        }
    }
}

