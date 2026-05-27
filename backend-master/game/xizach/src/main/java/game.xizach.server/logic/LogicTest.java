/*
 * Decompiled with CFR 0.144.
 */
package game.xizach.server.logic;

import game.xizach.server.logic.Card;
import game.xizach.server.logic.GroupCard;
import game.xizach.server.logic.XiZachRule;
import java.io.PrintStream;

public class LogicTest {
    public static void main(String[] args) {
        LogicTest.testBo();
    }

    public static void testBo() {
        GroupCard gc1 = new GroupCard();
        gc1.addCard(Card.createCard(1, 2));
        gc1.addCard(Card.createCard(6, 1));
        gc1.addCard(Card.createCard(6, 2));
        System.out.println(gc1);
        GroupCard gc5 = new GroupCard();
        gc5.addCard(Card.createCard(9, 3));
        gc5.addCard(Card.createCard(8, 0));
        gc5.addCard(Card.createCard(4, 1));
        System.out.println(gc5);
        GroupCard gc2 = new GroupCard();
        gc2.addCard(Card.createCard(10, 37));
        gc2.addCard(Card.createCard(5, 3));
        gc2.addCard(Card.createCard(2, 2));
        System.out.println(gc2);
        GroupCard gc6 = new GroupCard();
        gc6.addCard(Card.createCard(7, 2));
        gc6.addCard(Card.createCard(7, 1));
        gc6.addCard(Card.createCard(6, 2));
        System.out.println(gc6);
        GroupCard gc3 = new GroupCard();
        gc3.addCard(Card.createCard(9, 1));
        gc3.addCard(Card.createCard(7, 1));
        gc3.addCard(Card.createCard(8, 1));
        System.out.println(gc3);
        GroupCard gc7 = new GroupCard();
        gc7.addCard(Card.createCard(2, 3));
        gc7.addCard(Card.createCard(4, 3));
        gc7.addCard(Card.createCard(3, 3));
        System.out.println(gc7);
        GroupCard gc4 = new GroupCard();
        gc4.addCard(Card.createCard(3, 1));
        gc4.addCard(Card.createCard(3, 2));
        gc4.addCard(Card.createCard(3, 3));
        System.out.println(gc4);
        GroupCard gc8 = new GroupCard();
        gc8.addCard(Card.createCard(9, 1));
        gc8.addCard(Card.createCard(9, 3));
        gc8.addCard(Card.createCard(9, 0));
        System.out.println(gc8);
        System.out.println("So sap voi day: " + gc8.soSanhBo(gc3));
        System.out.println("So sap voi muoi: " + gc8.soSanhBo(gc2));
        System.out.println("So sap voi thuong: " + gc8.soSanhBo(gc1));
        System.out.println("So sap voi sap: " + gc4.soSanhBo(gc8) + " " + gc4 + " " + gc8);
        System.out.println("So day voi muoi: " + gc7.soSanhBo(gc2));
        System.out.println("So day voi thuong: " + gc7.soSanhBo(gc1));
        System.out.println("So day voi day: " + gc7.soSanhBo(gc3) + " " + gc7 + " " + gc3);
        System.out.println("So muoi voi thuong: " + gc2.soSanhBo(gc1));
        System.out.println("So muoi voi muoi: " + gc2.soSanhBo(gc6) + " " + gc2 + " " + gc6);
        System.out.println("So thuong voi thuong: " + gc1 + " " + gc5);
        System.out.println("So sap voi day: " + XiZachRule.soSanhBai(gc3, gc8) + " " + gc3 + " " + gc8);
        System.out.println("So sap voi muoi: " + XiZachRule.soSanhBai(gc8, gc2) + " " + gc8 + " " + gc2);
        System.out.println("So sap voi thuong: " + XiZachRule.soSanhBai(gc8, gc1) + " " + gc8 + " " + gc1);
        System.out.println("So sap voi sap: " + XiZachRule.soSanhBai(gc4, gc8) + " " + gc4 + " " + gc8);
        System.out.println("So day voi muoi: " + XiZachRule.soSanhBai(gc2, gc7) + " " + gc2 + " " + gc7);
        System.out.println("So day voi thuong: " + XiZachRule.soSanhBai(gc1, gc7) + " " + gc1 + " " + gc7);
        System.out.println("So day voi day: " + XiZachRule.soSanhBai(gc7, gc3) + " " + gc7 + " " + gc3);
        System.out.println("So muoi voi thuong: " + XiZachRule.soSanhBai(gc2, gc1) + " " + gc2 + " " + gc1);
        System.out.println("So muoi voi muoi: " + XiZachRule.soSanhBai(gc2, gc6) + " " + gc2 + " " + gc6);
        System.out.println("So thuong voi thuong: " + XiZachRule.soSanhBai(gc1, gc5) + " " + gc1 + " " + gc5);
        System.out.println("So thuong voi thuong: " + XiZachRule.soSanhBai(gc5, gc1) + " " + gc5 + " " + gc1);
    }

    public static void testCard() {
        for (int i = 0; i < 52; i = (int)((byte)(i + 1))) {
            Card c = Card.createCard(i);
            if (i % 4 != 2) continue;
        }
    }
}

