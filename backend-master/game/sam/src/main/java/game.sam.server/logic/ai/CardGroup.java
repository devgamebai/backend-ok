/*
 * Decompiled with CFR 0.144.
 */
package game.sam.server.logic.ai;

import game.sam.server.logic.ai.SamCard;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CardGroup {
    public static final int TYPENONE = -1;
    public static final int TYPEMOTLA = 1;
    public static final int TYPEDOI = 2;
    public static final int TYPEBALA = 3;
    public static final int TYPESANH = 4;
    public static final int TYPETUQUY = 5;
    public static final int TYPEHAITUQUY = 6;
    public List<SamCard> cards;
    public int typeGroup = -1;
    Comparator<SamCard> sortTangDan = new Comparator<SamCard>(){

        @Override
        public int compare(SamCard o1, SamCard o2) {
            if (o1.id > o2.id) {
                return 1;
            }
            if (o1.id < o2.id) {
                return -1;
            }
            return 0;
        }
    };

    public CardGroup(List<SamCard> cards) {
        this.cards = cards;
        this.typeGroup = -1;
        this.initCards();
    }

    public void initCards() {
        int i;
        if (this.cards.size() == 0) {
            return;
        }
        int size = this.cards.size();
        Collections.sort(this.cards, this.sortTangDan);
        this.typeGroup = -1;
        if (size == 1) {
            this.typeGroup = 1;
            return;
        }
        if (size == 2) {
            if (this.cards.get((int)0).so == this.cards.get((int)1).so) {
                this.typeGroup = 2;
                return;
            }
            return;
        }
        if (size == 3) {
            if (this.cards.get((int)0).so == this.cards.get((int)1).so && this.cards.get((int)0).so == this.cards.get((int)2).so) {
                this.typeGroup = 3;
                return;
            }
        } else if (size == 4) {
            if (this.cards.get((int)0).so == this.cards.get((int)1).so && this.cards.get((int)0).so == this.cards.get((int)2).so && this.cards.get((int)0).so == this.cards.get((int)3).so) {
                this.typeGroup = 5;
                return;
            }
        } else if (size == 8 && this.cards.get((int)0).so == this.cards.get((int)1).so && this.cards.get((int)0).so == this.cards.get((int)2).so && this.cards.get((int)0).so == this.cards.get((int)3).so && this.cards.get((int)4).so == this.cards.get((int)5).so && this.cards.get((int)4).so == this.cards.get((int)6).so && this.cards.get((int)4).so == this.cards.get((int)7).so) {
            this.typeGroup = 6;
            return;
        }
        Boolean sanh = false;
        int so = this.cards.get((int)0).so;
        int countSanh = 0;
        for (int i2 = 1; i2 < this.cards.size(); ++i2) {
            if (this.cards.get((int)i2).so - 1 != so) continue;
            so = this.cards.get((int)i2).so;
            ++countSanh;
        }
        if (countSanh == this.cards.size() - 1 && countSanh >= 2 && this.cards.get((int)(this.cards.size() - 1)).so != 12) {
            sanh = true;
        }
        ArrayList<SamCard> cardSortNguoc = new ArrayList<SamCard>();
        for (i = 0; i < this.cards.size(); ++i) {
            if (this.cards.get((int)i).so == 11 || this.cards.get((int)i).so == 12) continue;
            cardSortNguoc.add(this.cards.get(i));
        }
        for (i = 0; i < this.cards.size(); ++i) {
            cardSortNguoc.add(0, this.cards.get(i));
        }
        for (i = 0; i < this.cards.size(); ++i) {
            cardSortNguoc.add(0, this.cards.get(i));
        }
        countSanh = 0;
        so = ((SamCard)cardSortNguoc.get((int)0)).so;
        for (i = 1; i < cardSortNguoc.size(); ++i) {
            if (SamCard.convertSo(((SamCard)cardSortNguoc.get((int)i)).so) != SamCard.convertSo(so + 1)) continue;
            so = ((SamCard)cardSortNguoc.get((int)i)).so;
            ++countSanh;
        }
        if (countSanh == this.cards.size() - 1 && countSanh >= 2) {
            sanh = true;
        }
        if (sanh.booleanValue()) {
            this.typeGroup = 4;
            return;
        }
    }

    public Boolean isSanhNguoc() {
        for (int i = 0; i < this.cards.size(); ++i) {
            if (this.cards.get((int)i).so != 12) continue;
            return true;
        }
        return false;
    }

    public List<SamCard> makeSanhArray() {
        if (this.isSanhNguoc().booleanValue()) {
            return this.makeSanhNguocArray();
        }
        ArrayList<SamCard> res = new ArrayList<SamCard>();
        for (int i = 0; i < this.cards.size(); ++i) {
            res.add(this.cards.get(i));
        }
        return res;
    }

    public List<SamCard> makeSanhNguocArray() {
        int i;
        ArrayList<SamCard> res = new ArrayList<SamCard>();
        for (i = 0; i < this.cards.size(); ++i) {
            if (this.cards.get((int)i).so == 11 || this.cards.get((int)i).so == 12) continue;
            res.add(this.cards.get(i));
        }
        for (i = 0; i < this.cards.size(); ++i) {
            if (this.cards.get((int)i).so != 12) continue;
            res.add(0, this.cards.get(i));
        }
        for (i = 0; i < this.cards.size(); ++i) {
            if (this.cards.get((int)i).so != 11) continue;
            res.add(0, this.cards.get(i));
        }
        return res;
    }

    public Boolean isSanhToiCot() {
        if (this.typeGroup != 4) {
            return false;
        }
        List<SamCard> res = this.makeSanhArray();
        if (res.get((int)(res.size() - 1)).so == 11) {
            return true;
        }
        return false;
    }

}

