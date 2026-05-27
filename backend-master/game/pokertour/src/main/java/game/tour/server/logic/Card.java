/*
 * Decompiled with CFR 0.144.
 */
package game.tour.server.logic;

public class Card
implements Comparable {
    public static final String[][] card_name = new String[][]{{"2b", "3b", "4b", "5b", "6b", "7b", "8b", "9b", "10b", "Jb", "Qb", "Kb", "Ab"}, {"2t", "3t", "4t", "5t", "6t", "7t", "8t", "9t", "10t", "Jt", "Qt", "Kt", "At"}, {"2r", "3r", "4r", "5r", "6r", "7r", "8r", "9r", "10r", "Jr", "Qr", "Kr", "Ar"}, {"2c", "3c", "4c", "5c", "6c", "7c", "8c", "9c", "10c", "Jc", "Qc", "Kc", "Ac"}};
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
    public static final int eCARD_A = 14;
    public static final int eCARD_NONE = 15;
    public static final byte eBICH = 0;
    public static final byte eCHUON = 1;
    public static final byte eRO = 2;
    public static final byte eCO = 3;
    public static final byte eSHAPE_NONE = 4;
    public static final int eDO = 0;
    public static final int eDEN = 1;
    public static final int eNONE = 2;
    public static final int eID_NONE = 52;
    public int ID;
    public int SO;
    public int CHAT;
    public String name = "no";

    public Card(int id) {
        if (id >= 0 && id < 52) {
            this.ID = id;
            this.SO = this.ID / 4 + 2;
            this.CHAT = this.ID % 4;
            this.name = card_name[this.CHAT][this.SO - 2];
        } else {
            this.ID = 52;
            this.CHAT = 4;
            this.SO = 15;
        }
    }

    public Card(Card c) {
        this(c.GetNumber(), c.GetSuit());
    }

    public void init(int so, int chat) {
        this.SO = so;
        this.CHAT = chat;
        this.ID = (so - 2) * 4 + chat;
        this.name = card_name[this.CHAT][this.SO - 2];
    }

    public Card(int so, int chat) {
        this.SO = so;
        this.CHAT = chat;
        this.ID = (so - 2) * 4 + chat;
        this.name = card_name[this.CHAT][this.SO - 2];
    }

    public int compareTo(Object card) {
        if (card instanceof Card) {
            Card other = (Card)card;
            if (this.SO != other.SO) {
                return this.SO - other.SO;
            }
            return this.CHAT - other.CHAT;
        }
        return 0;
    }

    public String toString() {
        return this.name;
    }

    public boolean nextTo(Card next) {
        if (this.SO == 14) {
            return next.SO == 2;
        }
        return this.SO + 1 == next.SO;
    }

    public int soSanhSo(Card card) {
        if (this.SO > card.SO) {
            return 1;
        }
        if (this.SO < card.SO) {
            return -1;
        }
        return 0;
    }

    public boolean isRed() {
        return this.CHAT == 2 || this.CHAT == 3;
    }

    public boolean dongSo(Card card) {
        return this.SO == card.SO;
    }

    public boolean dongChat(Card card) {
        return this.CHAT == card.CHAT;
    }

    public boolean dongMau(Card card) {
        if (this.CHAT == 0 || this.CHAT == 1) {
            return card.CHAT == 0 || card.CHAT == 1;
        }
        return card.CHAT == 2 || card.CHAT == 3;
    }

    public int GetSuit() {
        return this.CHAT;
    }

    public int GetNumber() {
        return this.SO;
    }

    int soSanhAtNhoNhat(Card c) {
        if (this.SO == 14 && c.SO != 14) {
            return -1;
        }
        if (this.SO != 14 && c.SO == 14) {
            return 1;
        }
        return this.compareTo(c);
    }
}

