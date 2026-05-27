/*
 * Decompiled with CFR 0.144.
 */
package game.binh.server.logic;

import game.binh.server.logic.BinhRule;
import game.binh.server.logic.Card;
import game.binh.server.logic.GroupCard;
import java.util.Collection;
import java.util.List;

public class PlayerCard {
    public GroupCard fullCard;
    public GroupCard chi1 = new GroupCard();
    public GroupCard chi2 = new GroupCard();
    public GroupCard chi3 = new GroupCard();

    public synchronized int GetPlayerCardsKind(int roomType) {
        if (this.fullCard.kiemtraBo(roomType) == 0) {
            return this.fullCard.kiemtraBo(roomType);
        }
        if (roomType == 1 && (this.fullCard.kiemtraBo(roomType) == 1 || this.fullCard.kiemtraBo(roomType) == 2)) {
            return this.fullCard.kiemtraBo(roomType);
        }
        if (this.isBaThung(roomType)) {
            return 3;
        }
        if (this.isBaSanh(roomType)) {
            return 4;
        }
        if (this.isLucPheBon(roomType)) {
            return 5;
        }
        if (roomType == 0) {
            if (BinhRule.SoSanhChi1(this.chi1, this.chi2) < 0 || BinhRule.SoSanhChi1(this.chi2, this.chi3) < 0) {
                return 7;
            }
            return 6;
        }
        if (BinhRule.SoSanhChi2(this.chi1, this.chi2) < 0 || BinhRule.SoSanhChi2(this.chi2, this.chi3) < 0) {
            return 7;
        }
        return 6;
    }

    private boolean isBaThung(int roomType) {
        return !(this.chi1.kiemtraBo(roomType) != 11 && this.chi1.kiemtraBo(roomType) != 8 || this.chi2.kiemtraBo(roomType) != 11 && this.chi2.kiemtraBo(roomType) != 8 || this.chi3.kiemTraBoMauBinhBaThungChiBa() != 11);
    }

    private boolean isBaSanh(int roomType) {
        return !(this.chi1.kiemtraBo(roomType) != 12 && this.chi1.kiemtraBo(roomType) != 8 || this.chi2.kiemtraBo(roomType) != 12 && this.chi2.kiemtraBo(roomType) != 8 || this.chi3.kiemTraBoMauBinhBaSanhChiBa() != 12);
    }

    private boolean isLucPheBon(int roomType) {
        return this.fullCard.kiemtraBo(roomType) == 5 && this.chi1.kiemtraBo(roomType) == 14 && this.chi2.kiemtraBo(roomType) == 14 && this.chi3.kiemtraBo(roomType) == 15;
    }

    public GroupCard ChiBa() {
        return this.chi3;
    }

    public GroupCard ChiHai() {
        return this.chi2;
    }

    public GroupCard getChi(int chi) {
        switch (chi) {
            case 1: {
                return this.chi1;
            }
            case 2: {
                return this.chi2;
            }
            case 3: {
                return this.chi3;
            }
        }
        return null;
    }

    public GroupCard ChiMot() {
        return this.chi1;
    }

    public void autoSort1() {
        if (this.fullCard.kiemtraBo(0) == 0) {
            this.fullCard.xepTangTuHai();
        }
        if (this.fullCard.kiemtraBo(0) == 5) {
            this.sortLucPheBon(0);
        } else if (this.fullCard.kiemtraBo(0) == 3) {
            this.sortBaThung(0);
        } else if (this.fullCard.kiemtraBo(0) == 4) {
            this.sortBaSanh(0);
        } else {
            this.sortChi(0);
        }
    }

    public void autoSort2() {
        if (this.fullCard.kiemtraBo(1) == 0 || this.fullCard.kiemtraBo(1) == 1 || this.fullCard.kiemtraBo(1) == 2) {
            this.fullCard.xepTangTuHai();
        }
        if (this.fullCard.kiemtraBo(1) == 5) {
            this.sortLucPheBon(1);
        } else if (this.fullCard.kiemtraBo(1) == 3) {
            this.sortBaThung(1);
        } else if (this.fullCard.kiemtraBo(1) == 4) {
            this.sortBaSanh(1);
        } else {
            this.sortChi(1);
        }
    }

    private void sortLucPheBon(int rule) {
        this.fullCard.sortLucPheBon();
        this.ApplyNewGroupCards(this.fullCard, rule);
    }

    public void ApplyNewGroupCards(GroupCard gc, int rule) {
        int i;
        this.fullCard = gc;
        this.chi1.reset();
        this.chi2.reset();
        this.chi3.reset();
        for (i = 0; i < 5; ++i) {
            this.chi1.AddCard(gc.cards.get(i));
        }
        for (i = 5; i < 10; ++i) {
            this.chi2.AddCard(gc.cards.get(i));
        }
        for (i = 10; i < 13; ++i) {
            this.chi3.AddCard(gc.cards.get(i));
        }
        this.chi1.kiemtraBo(rule);
        this.chi2.kiemtraBo(rule);
        this.chi3.kiemtraBo(rule);
    }

    private void sortBaThung(int rule) {
        this.fullCard.sortBaThung();
        this.ApplyNewGroupCards(this.fullCard, rule);
    }

    private void sortBaSanh(int rule) {
        this.fullCard.sortBaSanh();
        this.ApplyNewGroupCards(this.fullCard, rule);
    }

    public boolean kiemTraBinhLung(int rule) {
        if (rule == 0) {
            return BinhRule.SoSanhChi1(this.chi1, this.chi2) < 0 || BinhRule.SoSanhChi1(this.chi2, this.chi3) < 0;
        }
        return BinhRule.SoSanhChi2(this.chi1, this.chi2) < 0 || BinhRule.SoSanhChi2(this.chi2, this.chi3) < 0;
    }

    public void ApplyNew3GroupCards(GroupCard chi1, GroupCard chi2, GroupCard chi3, int rule) {
        this.chi1 = chi1;
        this.chi2 = chi2;
        this.chi3 = chi3;
        this.fullCard.cards.clear();
        this.fullCard.cards.addAll(this.chi1.cards);
        this.fullCard.cards.addAll(this.chi2.cards);
        this.fullCard.cards.addAll(this.chi3.cards);
        this.fullCard.kiemTraBoNoSort(rule);
    }

    private void sortChi(int rule) {
        GroupCard gc = this.fullCard.copy();
        GroupCard chi1 = gc.getGroup(1);
        GroupCard chi2 = gc.getGroup(2);
        GroupCard chi3 = gc.getGroup(3);
        chi1.kiemtraBo(rule);
        chi2.kiemtraBo(rule);
        chi3.kiemtraBo(rule);
        if (chi3.GetNumOfCards() == 3 && chi2.GetNumOfCards() == 5 && chi1.GetNumOfCards() == 5) {
            this.ApplyNew3GroupCards(chi1, chi2, chi3, rule);
            if (this.kiemTraBinhLung(rule)) {
                this.ApplyNew3GroupCards(chi2, chi1, chi3, rule);
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.chi1).append("|");
        sb.append(this.chi2).append("|");
        sb.append(this.chi3).append("|");
        return sb.toString();
    }
}

