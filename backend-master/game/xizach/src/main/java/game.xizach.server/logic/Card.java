/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  game.utils.LoggerUtils
 */
package game.xizach.server.logic;

import game.utils.LoggerUtils;

public class Card {
    public static final String[][] card_name = new String[][]{{"Ab", "2b", "3b", "4b", "5b", "6b", "7b", "8b", "9b", "10b", "Jb", "Qb", "Kb"}, {"At", "2t", "3t", "4t", "5t", "6t", "7t", "8t", "9t", "10t", "Jt", "Qt", "Kt"}, {"Ar", "2r", "3r", "4r", "5r", "6r", "7r", "8r", "9r", "10r", "Jr", "Qr", "Kr"}, {"Ac", "2c", "3c", "4c", "5c", "6c", "7c", "8c", "9c", "10c", "Jc", "Qc", "Kc"}};
    public static final int eCARD_A = 0;
    public static final int eCARD_2 = 1;
    public static final int eCARD_3 = 2;
    public static final int eCARD_4 = 3;
    public static final int eCARD_5 = 4;
    public static final int eCARD_6 = 5;
    public static final int eCARD_7 = 6;
    public static final int eCARD_8 = 7;
    public static final int eCARD_9 = 8;
    public static final int eCARD_10 = 9;
    public static final int eCARD_J = 10;
    public static final int eCARD_Q = 11;
    public static final int eCARD_K = 12;
    public static final int eCARD_NONE = 0;
    public static final int ES_SPADE = 0;
    public static final int ES_CLUB = 1;
    public static final int ES_DIAMOND = 2;
    public static final int ES_HEART = 3;
    public static final int eSHAPE_NONE = 5;
    public static final int eID_MAX = 52;
    public int ID;
    public int SO;
    public int CHAT;
    public String name = "no";
    public int auxiVal;
    public static Card[] pool = new Card[53];

    public static void main(String[] args) {
        Card c = Card.createCard(0);
        Card d = Card.createCard(c.toString());
    }

    public static Card createCard(Card c) {
        return Card.createCard(c.ID);
    }

    public static Card createCard(int id) {
        Card c;
        if (id < 0 || id >= 52) {
            id = 52;
            LoggerUtils.error((String)"createCard ERROR", (Object[])new Object[]{id});
        }
        if ((c = pool[id]) != null) {
            return c;
        }
        Card.pool[id] = c = new Card(id);
        return c;
    }

    public static Card createCard(int so, int chat) {
        int id = so * 4 + chat;
        return Card.createCard(id);
    }

    public static Card createCard(String s) {
        int id = 0;
        int chat = 0;
        int num = 0;
        if (s.equalsIgnoreCase("no")) {
            id = 52;
            chat = 5;
            num = 0;
        } else {
            String so;
            int size = s.length();
            String ch = s.substring(size - 1);
            if (ch.equalsIgnoreCase("b")) {
                chat = 0;
            }
            if (ch.equalsIgnoreCase("t")) {
                chat = 1;
            }
            if (ch.equalsIgnoreCase("r")) {
                chat = 2;
            }
            if (ch.equalsIgnoreCase("c")) {
                chat = 3;
            }
            num = (so = s.substring(0, size - 1)).equalsIgnoreCase("A") ? 0 : (so.equalsIgnoreCase("J") ? 10 : (so.equalsIgnoreCase("Q") ? 11 : (so.equalsIgnoreCase("K") ? 12 : Integer.parseInt(so) - 1)));
            id = (num - 1) * 4 + chat;
        }
        return Card.createCard(id);
    }

    private Card(int id) {
        if (id >= 0 && id < 52) {
            this.ID = id;
            this.SO = this.ID / 4;
            this.CHAT = this.ID % 4;
            this.name = card_name[this.CHAT][this.SO];
        } else {
            this.ID = 52;
            this.CHAT = 5;
            this.SO = 0;
            this.name = "";
        }
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

    public int getDiem() {
        if (this.isXi().booleanValue()) {
            return -1;
        }
        if (this.SO == 9 || this.SO == 10 || this.SO == 11 || this.SO == 12) {
            return 10;
        }
        return this.SO + 1;
    }

    public int getMinDiem() {
        if (this.isXi().booleanValue()) {
            return 1;
        }
        if (this.SO == 9 || this.SO == 10 || this.SO == 11 || this.SO == 12) {
            return 10;
        }
        return this.SO + 1;
    }

    public Boolean isXi() {
        return this.SO == 0;
    }

    public int GetNumber() {
        return this.SO;
    }

    public int GetSuit() {
        return this.CHAT;
    }
}

