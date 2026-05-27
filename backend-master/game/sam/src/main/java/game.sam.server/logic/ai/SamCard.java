/*
 * Decompiled with CFR 0.144.
 */
package game.sam.server.logic.ai;

import game.sam.server.logic.Card;

public class SamCard {
    public static final int kQuanbai3 = 0;
    public static final int kQuanbai4 = 1;
    public static final int kQuanbai5 = 2;
    public static final int kQuanbai6 = 3;
    public static final int kQuanbai7 = 4;
    public static final int kQuanbai8 = 5;
    public static final int kQuanbai9 = 6;
    public static final int kQuanbai10 = 7;
    public static final int kQuanbaiJ = 8;
    public static final int kQuanbaiQ = 9;
    public static final int kQuanbaiK = 10;
    public static final int kQuanbaiA = 11;
    public static final int kQuanbai2 = 12;
    public static final int kChatBICH = 0;
    public static final int kChatCHUON = 1;
    public static final int kChatRO = 2;
    public static final int kChatCO = 3;
    public int id;
    public int chat;
    public int so;
    public Boolean mark;
    public int group = -1;

    public SamCard(Card c) {
        this(c.ID);
    }

    public static int convertSo(int id) {
        return (id + 13) % 13;
    }

    public SamCard(int id) {
        this.id = id;
        this.chat = this.getChatById(this.id);
        this.so = this.getSoById(this.id);
        this.mark = false;
    }

    public int getChatById(int id) {
        return id % 4;
    }

    public int getSoById(int id) {
        return id / 4;
    }

    public int compare(SamCard anotherCard) {
        if (this.so > anotherCard.so) {
            return 1;
        }
        if (this.so == anotherCard.so) {
            return 0;
        }
        return -1;
    }

    public int convertToServerCard(int idex) {
        return idex;
    }

    public static Boolean isGreater(SamCard a, SamCard b) {
        return SamCard.isSoGreater(a.so, b.so);
    }

    public static Boolean isSoGreater(int a, int b) {
        return a > b;
    }
}

