/*
 * Decompiled with CFR 0.144.
 */
package game.sam.server.logic;

public class Card
implements Comparable {
    public static final String[][] card_name = new String[][]{{"3b", "4b", "5b", "6b", "7b", "8b", "9b", "10b", "Jb", "Qb", "Kb", "Ab", "2b"}, {"3t", "4t", "5t", "6t", "7t", "8t", "9t", "10t", "Jt", "Qt", "Kt", "At", "2t"}, {"3r", "4r", "5r", "6r", "7r", "8r", "9r", "10r", "Jr", "Qr", "Kr", "Ar", "2r"}, {"3c", "4c", "5c", "6c", "7c", "8c", "9c", "10c", "Jc", "Qc", "Kc", "Ac", "2c"}};
    public static final int SO_TU_QUY = 13;
    public static final byte eCARD_3 = 0;
    public static final byte eCARD_4 = 1;
    public static final byte eCARD_5 = 2;
    public static final byte eCARD_6 = 3;
    public static final byte eCARD_7 = 4;
    public static final byte eCARD_8 = 5;
    public static final byte eCARD_9 = 6;
    public static final byte eCARD_10 = 7;
    public static final byte eCARD_J = 8;
    public static final byte eCARD_Q = 9;
    public static final byte eCARD_K = 10;
    public static final byte eCARD_A = 11;
    public static final byte eCARD_2 = 12;
    public static final byte eCARD_NONE = 13;
    public static final byte eBICH = 0;
    public static final byte eCHUON = 1;
    public static final byte eRO = 2;
    public static final byte eCO = 3;
    public static final byte eSHAPE_NONE = 4;
    public static final byte eID_NONE = 52;
    public byte ID;
    public byte SO;
    public byte CHAT;
    public String name = "no";

    public static int sosanhLabai(byte id1, byte id2) {
        if (id1 == id2) {
            return 0;
        }
        byte so1 = (byte)(id1 / 4);
        byte so2 = (byte)(id2 / 4);
        if (so1 == so2) {
            byte chat1 = (byte)(id1 % 4);
            byte chat2 = (byte)(id2 % 4);
            return chat1 - chat2;
        }
        return so1 - so2;
    }

    public Card(byte id) {
        if (id >= 0 && id < 52) {
            this.ID = id;
            this.SO = (byte)(this.ID / 4);
            this.CHAT = (byte)(this.ID % 4);
            this.name = card_name[this.CHAT][this.SO];
        } else {
            this.ID = (byte)52;
            this.CHAT = (byte)4;
            this.SO = (byte)13;
        }
    }

    public String toString() {
        return this.name;
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

    public int soSanhSo(Object card) {
        if (card instanceof Card) {
            Card other = (Card)card;
            return this.SO - other.SO;
        }
        return 0;
    }

    public boolean dongMau(Card card) {
        if (this.CHAT == 0 || this.CHAT == 1) {
            return card.CHAT == 0 || card.CHAT == 1;
        }
        return card.CHAT == 2 || card.CHAT == 3;
    }
}

