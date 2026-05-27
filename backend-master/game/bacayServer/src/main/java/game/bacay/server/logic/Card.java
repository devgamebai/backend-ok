/*
 * Decompiled with CFR 0.144.
 */
package game.bacay.server.logic;

import java.io.PrintStream;

public class Card {
    public static final String[][] card_name = new String[][]{{"Ab", "2b", "3b", "4b", "5b", "6b", "7b", "8b", "9b", "10b", "Jb", "Qb", "Kb"}, {"At", "2t", "3t", "4t", "5t", "6t", "7t", "8t", "9t", "10t", "Jt", "Qt", "Kt"}, {"Ac", "2c", "3c", "4c", "5c", "6c", "7c", "8c", "9c", "10c", "Jc", "Qc", "Kc"}, {"Ar", "2r", "3r", "4r", "5r", "6r", "7r", "8r", "9r", "10r", "Jr", "Qr", "Kr"}};
    public static final int eCARD_A = 1;
    public static final int eCARD_2 = 2;
    public static final int eCARD_3 = 3;
    public static final int eCARD_4 = 4;
    public static final int eCARD_5 = 5;
    public static final int eCARD_6 = 6;
    public static final int eCARD_7 = 7;
    public static final int eCARD_8 = 8;
    public static final int eCARD_9 = 9;
    public static final int eCARD_10 = 10;
    public static final int eCARD_J = 11;
    public static final int eCARD_Q = 12;
    public static final int eCARD_K = 13;
    public static final int eCARD_NONE = 0;
    public static final int ES_SPADE = 0;
    public static final int ES_CLUB = 1;
    public static final int ES_HEART = 2;
    public static final int ES_DIAMOND = 3;
    public static final int eSHAPE_NONE = 5;
    public static final int eID_NONE = 36;
    public int ID;
    public int SO;
    public int CHAT;
    public String name = "no";
    public int auxiVal;

    public static void main(String[] args) {
        Card c = new Card(0);
        Card d = new Card(c.toString());
        System.out.println(d);
        System.out.println(c);
    }

    public Card(String s) {
        if (s.equalsIgnoreCase("no")) {
            this.ID = 36;
            this.CHAT = 5;
            this.SO = 0;
        } else {
            String so;
            int size = s.length();
            String ch = s.substring(size - 1);
            if (ch.equalsIgnoreCase("b")) {
                this.SO = 0;
            }
            if (ch.equalsIgnoreCase("t")) {
                this.SO = 1;
            }
            if (ch.equalsIgnoreCase("c")) {
                this.SO = 2;
            }
            if (ch.equalsIgnoreCase("r")) {
                this.SO = 3;
            }
            this.SO = (so = s.substring(0, size - 1)).equalsIgnoreCase("A") ? 1 : (so.equalsIgnoreCase("J") ? 11 : (so.equalsIgnoreCase("Q") ? 12 : (so.equalsIgnoreCase("K") ? 13 : Integer.parseInt(so))));
            this.ID = (this.SO - 1) * 4 + this.CHAT;
            this.name = card_name[this.CHAT][this.SO - 1];
        }
    }

    public Card(int id) {
        if (id >= 0 && id < 36) {
            this.ID = id;
            this.SO = this.ID / 4 + 1;
            this.CHAT = this.ID % 4;
            this.name = card_name[this.CHAT][this.SO - 1];
        } else {
            this.ID = 36;
            this.CHAT = 5;
            this.SO = 0;
        }
    }

    public Card(Card c) {
        this(c.GetNumber(), c.GetSuit());
    }

    public Card(int so, int chat) {
        this.SO = so;
        this.CHAT = chat;
        this.ID = (so - 1) * 4 + chat;
        this.name = card_name[this.CHAT][this.SO - 1];
    }

    public String toString() {
        return this.name;
    }

    public int soSanhSo(Object card) {
        if (card instanceof Card) {
            Card other = (Card)card;
            return this.SO - other.SO;
        }
        return 0;
    }

    public int GetNumber() {
        return this.SO;
    }

    public int GetSuit() {
        return this.CHAT;
    }

    public int lonHonTinhDiemVaSap(Card card) {
        if (this.CHAT > card.CHAT) {
            return 1;
        }
        if (this.CHAT < card.CHAT) {
            return -1;
        }
        if (this.isAtRo() || card.isAtRo()) {
            return this.soSanhSoCoAtRo(card);
        }
        if (this.SO > card.SO) {
            return 1;
        }
        if (this.SO < card.SO) {
            return -1;
        }
        return 0;
    }

    public int lonHonTinhDay(Card card) {
        if (this.SO > card.SO) {
            return 1;
        }
        if (this.SO < card.SO) {
            return -1;
        }
        if (this.CHAT > card.CHAT) {
            return 1;
        }
        if (this.CHAT < card.CHAT) {
            return -1;
        }
        return 0;
    }

    public boolean isAtRo() {
        return this.SO == 1 && this.CHAT == 3;
    }

    private int soSanhSoCoAtRo(Card c) {
        if (this.isAtRo()) {
            return 1;
        }
        return -1;
    }

    public boolean dongChatTren(Card card) {
        return this.CHAT == card.CHAT && this.SO + 1 == card.SO;
    }

    public boolean dongChatDuoi(Card card) {
        return this.CHAT == card.CHAT && this.SO - 1 == card.SO;
    }

    public boolean dongChat(Card card) {
        return this.CHAT == card.CHAT;
    }
}

