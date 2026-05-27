/*
 * Decompiled with CFR 0.144.
 */
package game.tienlen.server.logic;

public class LogicTest {
    public static void main(String[] args) {
        LogicTest.testGroupCard();
    }

    public static void testGroupCard() {
        byte[] card1 = new byte[]{0, 4, 8, 12, 16, 51, 47};
        byte[] card2 = new byte[]{0, 4, 8, 12, 16, 51, 20};
        GroupCard gc1 = new GroupCard(card1);
        System.out.println(gc1);
        GroupCard gc2 = new GroupCard(card2);
        System.out.println(gc2);
        System.out.println(gc2.chatDuoc(gc1));
    }

    public static void testCard() {
        for (int i = 0; i < 4; i = (int)((byte)(i + 1))) {
            Card c = new Card(i);
            System.out.println(c);
        }
    }
}

