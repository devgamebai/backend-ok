/*
 * Decompiled with CFR 0.144.
 */
package game.binh.server.logic;

import game.binh.server.logic.BinhRule;
import game.binh.server.logic.Card;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

public class GroupCard {
    private static final String[] group_names = new String[]{"SR", "MB", "MH", "BT", "BS", "LPB", "NM", "BL", "TPS", "TQ", "CL", "TH", "S", "X", "HD", "D", "R", "NO", "NULL"};
    public static final int FULL_SIZE = 13;
    public static final int FIRST_SIZE = 5;
    public static final int SECOND_SIZE = 3;
    public static final int SANH_RONG = 0;
    public static final int DONG_MAU_MUOI_BA = 1;
    public static final int DONG_MAU_MUOI_HAI = 2;
    public static final int BA_THUNG = 3;
    public static final int BASANH = 4;
    public static final int LUC_PHE_BON = 5;
    public static final int EM_NORMAL = 6;
    public static final int EM_BINHLUNG = 7;
    public static final int EG_THUNGPHASANH = 8;
    public static final int EG_TUQUY = 9;
    public static final int EG_CULU = 10;
    public static final int EG_THUNG = 11;
    public static final int EG_SANH = 12;
    public static final int EG_SAMCHI = 13;
    public static final int EG_2DOIKHACNHAU = 14;
    public static final int EG_MOTDOI = 15;
    public static final int EG_RAC = 16;
    public static final int NO_GROUP = 17;
    public static final int NONE = 18;
    private GroupCard baSanh = null;
    public static final Comparator<Card> TANG_TU_HAI = new Comparator<Card>(){

        @Override
        public int compare(Card c1, Card c2) {
            return c1.compareTo(c2);
        }
    };
    public static final Comparator<Card> TANG_TU_AT = new Comparator<Card>(){

        @Override
        public int compare(Card c1, Card c2) {
            return c1.soSanhAtNhoNhat(c2);
        }
    };
    public static final Comparator<Card> GIAM = new Comparator<Card>(){

        @Override
        public int compare(Card c1, Card c2) {
            return c2.compareTo(c1);
        }
    };
    public int BO = 18;
    public List<Card> cards = new LinkedList<Card>();

    public GroupCard() {
    }

    public GroupCard(String data) {
        String[] s = data.split(":");
        if (s.length == 2) {
            this.cards = new ArrayList<Card>();
            String d = s[1];
            StringBuilder sb = new StringBuilder();
            char[] c = new char[3];
            int k = 0;
            for (int i = 0; i < d.length() - 1; ++i) {
                c[k] = d.charAt(i);
                if (c[k] == 'r' || c[k] == 'c' || c[k] == 't' || c[k] == 'b') {
                    sb.setLength(0);
                    for (int j = 0; j < k; ++j) {
                        sb.append(c[j]);
                    }
                    int so = 0;
                    String phanSo = sb.toString();
                    so = phanSo.equalsIgnoreCase("A") ? 14 : (phanSo.equalsIgnoreCase("J") ? 11 : (phanSo.equalsIgnoreCase("Q") ? 12 : (phanSo.equalsIgnoreCase("K") ? 13 : Integer.parseInt(phanSo))));
                    int phanChat = 0;
                    if (c[k] == 'r') {
                        phanChat = 2;
                    }
                    if (c[k] == 'c') {
                        phanChat = 3;
                    }
                    if (c[k] == 't') {
                        phanChat = 0;
                    }
                    if (c[k] == 'b') {
                        phanChat = 1;
                    }
                    Card card = Card.createCard(so, phanChat);
                    this.cards.add(card);
                    k = 0;
                    continue;
                }
                ++k;
            }
        }
    }

    public GroupCard(int[] ids) {
        for (int id : ids) {
            Card card = Card.createCard(id);
            this.cards.add(card);
        }
    }

    public GroupCard(byte[] ids) {
        for (byte id : ids) {
            Card card = Card.createCard(id);
            this.cards.add(card);
        }
    }

    public void xepTangTuHai() {
        Collections.sort(this.cards, TANG_TU_HAI);
    }

    public void xepTangTuAt() {
        Collections.sort(this.cards, TANG_TU_AT);
    }

    public void xepGiam() {
        Collections.sort(this.cards, GIAM);
    }

    public void xepBo() {
        int size = this.cards.size();
        if (size != 5 && size != 3 && size != 13) {
            return;
        }
        if (this.BO == 0) {
            this.xepTangTuHai();
        }
        if (this.BO == 12 || this.BO == 8) {
            this.xepSanh();
        }
        if (this.BO == 9) {
            this.xepTuQuy();
        } else if (this.BO == 10) {
            this.xepCuLu();
        } else if (this.BO == 13) {
            this.xepSam();
        } else if (this.BO == 14 || this.BO == 15) {
            this.xepThuDoi();
        }
    }

    private void xepSanh() {
        this.xepTangTuHai();
        Card c1 = this.cards.get(this.cards.size() - 1);
        Card c2 = this.cards.get(this.cards.size() - 2);
        if (!c2.nextTo(c1)) {
            this.xepTangTuAt();
        }
    }

    public void xepTuQuy() {
        Collections.sort(this.cards, TANG_TU_HAI);
        Card c1 = this.cards.get(0);
        Card c2 = this.cards.get(1);
        if (c1.SO != c2.SO) {
            this.cards.remove(0);
            this.cards.add(c1);
        }
    }

    public void xepCuLu() {
        Card c1 = this.cards.get(1);
        Card c2 = this.cards.get(2);
        if (c1.SO != c2.SO) {
            this.xepTangTuHai();
        }
    }

    public void swap(int i, int j) {
        Collections.swap(this.cards, i, j);
    }

    public void xepSam() {
        int from = 0;
        for (int i = 0; i < this.cards.size() - 1; ++i) {
            Card c1 = this.cards.get(i);
            Card c2 = this.cards.get(i + 1);
            if (c1.SO != c2.SO) continue;
            from = i;
            break;
        }
        if (from == 1) {
            this.swap(0, 3);
            this.swap(0, 1);
            this.swap(1, 2);
        }
        if (from == 2) {
            this.swap(0, 3);
            this.swap(1, 4);
            this.swap(0, 2);
            this.swap(1, 2);
        }
    }

    public void xepThuDoi() {
        int i;
        ArrayList<Card> list = new ArrayList<Card>();
        for (i = 0; i < this.cards.size() - 1; ++i) {
            Card card1 = this.cards.get(i);
            Card card2 = this.cards.get(i + 1);
            if (card1.SO != card2.SO) continue;
            list.add(card1);
            list.add(card2);
        }
        for (i = 0; i < this.cards.size(); ++i) {
            Card c = this.cards.get(i);
            if (list.contains(c)) continue;
            list.add(c);
        }
        this.cards.clear();
        this.cards.addAll(list);
    }

    public void kiemTraBoNoSort(int rule) {
        GroupCard gc = this.copy();
        gc.kiemtraBo(rule);
        this.BO = gc.BO;
    }

    public int kiemTraBoMauBinhBaThungChiBa() {
        if (this.thung()) {
            return 11;
        }
        return 17;
    }

    public int kiemTraBoMauBinhBaSanhChiBa() {
        if (this.sanh()) {
            return 12;
        }
        return 17;
    }

    public int kiemtraBo(int rule) {
        int size = this.cards.size();
        if (size != 13 && size != 5 && size != 3) {
            this.xepGiam();
            this.BO = 17;
            return this.BO;
        }
        if (this.BO != 18 && this.BO != 17) {
            return this.BO;
        }
        this.xepGiam();
        if (size == 13) {
            this.BO = this.sanhRong() ? 0 : (this.dongMauMuoiBa() && rule == 1 ? 1 : (this.dongMauMuoiHai() && rule == 1 ? 2 : (this.baThung() ? 3 : (this.sauDoi() ? 5 : (this.baSanh() ? 4 : 6)))));
        } else if (size == 5) {
            int bo = this.doiThuSamCuluTuQuy();
            this.BO = bo != 18 ? bo : (this.thung() ? (this.sanh() ? 8 : 11) : (this.sanh() ? 12 : 16));
        } else if (size == 3) {
            int bo = this.doiThuSamCuluTuQuy();
            this.BO = bo != 18 ? bo : 16;
        }
        this.xepBo();
        return this.BO;
    }

    public boolean sanhRong() {
        return this.cards.size() == 13 && this.sanh();
    }

    public int demLaMauDo() {
        int count = 0;
        for (int i = 0; i < this.cards.size(); ++i) {
            Card c = this.cards.get(i);
            if (!c.isRed()) continue;
            ++count;
        }
        return count;
    }

    public boolean dongMauMuoiBa() {
        return this.demLaMauDo() == 13 || this.demLaMauDo() == 0;
    }

    public boolean dongMauMuoiHai() {
        return this.demLaMauDo() == 12 || this.demLaMauDo() == 1;
    }

    public boolean baThung() {
        if (this.cards.size() != 13) {
            return false;
        }
        int[] chat = new int[4];
        for (int i = 0; i < this.cards.size(); ++i) {
            Card c = this.cards.get(i);
            int[] arrn = chat;
            int n = c.CHAT;
            arrn[n] = arrn[n] + 1;
        }
        boolean flag = false;
        for (int i = 0; i < chat.length; ++i) {
            if (chat[i] != 0 && (chat[i] != 3 || flag) && chat[i] != 5 && chat[i] != 8 && chat[i] != 10) {
                return false;
            }
            if (chat[i] != 3) continue;
            flag = true;
        }
        return true;
    }

    public boolean sauDoi() {
        if (this.cards.size() != 13) {
            return false;
        }
        int count = 0;
        int doi = 0;
        Card prev = null;
        for (int i = 0; i < this.cards.size(); ++i) {
            Card c = this.cards.get(i);
            if (prev != null && c.SO == prev.SO) {
                if (doi == 0 || doi == 2) {
                    ++count;
                }
                ++doi;
            } else {
                doi = 0;
            }
            prev = c;
        }
        return count == 6;
    }

    public boolean baSanh() {
        if (this.cards.size() != 13) {
            return false;
        }
        this.baSanh = BinhRule.timBaCaiSanh(this);
        return this.baSanh != null;
    }

    public int doiThuSamCuluTuQuy() {
        int count = 1;
        int max1 = 1;
        int max2 = 0;
        Card prev = null;
        int size = this.cards.size();
        for (int i = 0; i < size; ++i) {
            Card c = this.cards.get(i);
            if (prev != null) {
                if (prev.SO == c.SO) {
                    ++count;
                }
                if (i == size - 1 || prev.SO != c.SO) {
                    if (count >= max1) {
                        max2 = max1;
                        max1 = count;
                    } else if (count >= max2) {
                        max2 = count;
                    }
                    if (prev.SO != c.SO && i != size - 1) {
                        count = 1;
                    }
                }
            }
            prev = c;
        }
        if (max1 == 4) {
            return 9;
        }
        if (max1 == 3 && max2 == 2) {
            return 10;
        }
        if (max1 == 3) {
            return 13;
        }
        if (max1 == 2 && max2 == 2) {
            return 14;
        }
        if (max1 == 2) {
            return 15;
        }
        return 18;
    }

    public boolean sam() {
        return false;
    }

    public boolean thu() {
        return false;
    }

    public boolean tuquy() {
        return false;
    }

    public boolean sanh() {
        Card prev = null;
        Card firstCard = null;
        for (int i = this.cards.size() - 1; i >= 0; --i) {
            Card c = this.cards.get(i);
            if (prev == null) {
                prev = c;
                firstCard = c;
                continue;
            }
            if (!prev.nextTo(c)) {
                if (i == 0) {
                    return c.nextTo(firstCard);
                }
                return false;
            }
            prev = c;
        }
        return true;
    }

    public boolean thung() {
        Card prev = null;
        for (int i = 0; i < this.cards.size(); ++i) {
            Card c = this.cards.get(i);
            if (prev != null && !prev.dongChat(c)) {
                return false;
            }
            prev = c;
        }
        return true;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("#");
        sb.append(group_names[this.BO]).append(":");
        for (Card c : this.cards) {
            sb.append(c.name);
        }
        sb.append("$");
        return sb.toString();
    }

    public GroupCard getGroup(int chi) {
        GroupCard gc = null;
        if (this.hasThungPhaSanh()) {
            gc = this.getThungPhaSanh();
            this.RemoveGroupCards(gc);
        } else if (this.hasTuQuy() != -1) {
            int n = this.hasTuQuy();
            gc = this.getCardsSameNumber(n);
            this.RemoveGroupCards(gc);
            if (this.GetNumOfCards() > 1) {
                GroupCard gCards = this.copy();
                if (gCards.hasCulu() && gCards.GetNumOfCards() > 5) {
                    gCards.RemoveGroupCards(gCards.getCulu());
                }
                if (gCards.hasThung() && gCards.GetNumOfCards() > 5) {
                    gCards.RemoveGroupCards(gCards.getThung());
                }
                if (gCards.hasSanh() && gCards.GetNumOfCards() > 5) {
                    gCards.RemoveGroupCards(gCards.getSanh());
                }
                if (gCards.hasXamChi() && gCards.GetNumOfCards() > 3) {
                    gCards.RemoveGroupCards(gCards.getXamChi());
                }
                if (gCards.hasThu() && gCards.GetNumOfCards() > 4) {
                    gCards.RemoveGroupCards(gCards.getThu());
                }
                if (gCards.hasPair() && gCards.GetNumOfCards() > 2) {
                    gCards.RemoveGroupCards(gCards.getPair());
                }
                gc.AddCard(gCards.Cards().get(0));
                this.RemoveCard(gCards.Cards().get(0));
            }
        } else if (this.hasCulu()) {
            gc = this.getCulu();
            this.RemoveGroupCards(gc);
        } else if (this.hasThung()) {
            gc = this.getThung();
            this.RemoveGroupCards(gc);
        } else if (this.hasSanh()) {
            gc = this.getSanh();
            this.RemoveGroupCards(gc);
        } else if (this.hasXamChi()) {
            gc = this.getXamChi();
            this.RemoveGroupCards(gc);
            if (this.GetNumOfCards() >= 2) {
                int j;
                GroupCard gc1 = this.copy();
                for (j = 0; j < 2; ++j) {
                    if (gc1.hasThu()) {
                        gc1.RemoveGroupCards(gc1.getThu());
                        continue;
                    }
                    if (!gc1.hasPair()) continue;
                    gc1.RemoveGroupCards(gc1.getPair());
                }
                int m = gc1.GetNumOfCards() < 5 - gc.GetNumOfCards() ? gc1.GetNumOfCards() : 5 - gc.GetNumOfCards();
                for (j = 0; j < m; ++j) {
                    gc.AddCard(gc1.Cards().get(0));
                    this.RemoveCard(gc1.Cards().get(0));
                    gc1.RemoveCard(gc1.Cards().get(0));
                }
            }
        } else if (this.hasThu()) {
            gc = this.getThu();
            this.RemoveGroupCards(gc);
            if (this.GetNumOfCards() > 0) {
                GroupCard gc1 = this.copy();
                if (gc1.hasPair()) {
                    gc1.RemoveGroupCards(gc1.getPair());
                }
                gc.AddCard(gc1.Cards().get(0));
                this.RemoveCard(gc1.Cards().get(0));
            }
        } else if (this.hasPair()) {
            gc = this.getPair();
            this.RemoveGroupCards(gc);
            int m = this.GetNumOfCards() < 5 - gc.GetNumOfCards() ? this.GetNumOfCards() : 5 - gc.GetNumOfCards();
            for (int j = 0; j < m; ++j) {
                gc.AddCard(this.cards.get(0));
                this.RemoveCard(this.cards.get(0));
            }
        } else {
            gc = new GroupCard();
            this.DecreaseSort();
            for (int i = 0; i < this.GetNumOfCards() && i < 5; ++i) {
                gc.AddCard(this.cards.get(i));
            }
            this.RemoveGroupCards(gc);
        }
        return gc;
    }

    private boolean hasPair() {
        int i;
        int[] num = new int[15];
        for (i = 0; i < 15; ++i) {
            num[i] = 0;
        }
        for (i = 0; i < this.GetNumOfCards(); ++i) {
            int[] arrn = num;
            int n = this.cards.get(i).GetNumber();
            arrn[n] = arrn[n] + 1;
        }
        for (i = 0; i < 15; ++i) {
            if (num[i] < 2) continue;
            return true;
        }
        return false;
    }

    private GroupCard getPair() {
        int i;
        int[] num = new int[15];
        for (i = 0; i < 15; ++i) {
            num[i] = 0;
        }
        for (i = 0; i < this.GetNumOfCards(); ++i) {
            int[] arrn = num;
            int n = this.cards.get(i).GetNumber();
            arrn[n] = arrn[n] + 1;
        }
        for (i = 14; i >= 0; --i) {
            if (num[i] < 2) continue;
            GroupCard gc = new GroupCard();
            for (int j = 0; j < this.GetNumOfCards(); ++j) {
                if (this.cards.get(j).GetNumber() == i) {
                    gc.AddCard(this.cards.get(j));
                }
                if (gc.GetNumOfCards() != 2) continue;
                return gc;
            }
        }
        return null;
    }

    private boolean hasThu() {
        int i;
        int[] num = new int[15];
        for (i = 0; i < 15; ++i) {
            num[i] = 0;
        }
        for (i = 0; i < this.GetNumOfCards(); ++i) {
            int[] arrn = num;
            int n = this.cards.get(i).GetNumber();
            arrn[n] = arrn[n] + 1;
        }
        int k = 0;
        for (i = 0; i < 15; ++i) {
            if (num[i] < 2) continue;
            ++k;
        }
        return k >= 2;
    }

    private GroupCard getThu() {
        int i;
        int[] num = new int[15];
        for (i = 0; i < 15; ++i) {
            num[i] = 0;
        }
        for (i = 0; i < this.GetNumOfCards(); ++i) {
            int[] arrn = num;
            int n = this.cards.get(i).GetNumber();
            arrn[n] = arrn[n] + 1;
        }
        int k = 0;
        GroupCard gc = new GroupCard();
        for (i = 14; i >= 0; --i) {
            if (num[i] >= 2) {
                for (int j = 0; j < this.GetNumOfCards(); ++j) {
                    if (this.cards.get(j).GetNumber() == i) {
                        gc.AddCard(this.cards.get(j));
                    }
                    if (gc.GetNumOfCards() == 4) break;
                }
                ++k;
            }
            if (k < 2) continue;
            return gc;
        }
        return null;
    }

    private boolean hasXamChi() {
        int i;
        int[] num = new int[15];
        for (i = 0; i < 15; ++i) {
            num[i] = 0;
        }
        for (i = 0; i < this.GetNumOfCards(); ++i) {
            int[] arrn = num;
            int n = this.cards.get(i).GetNumber();
            arrn[n] = arrn[n] + 1;
        }
        for (i = 0; i < 15; ++i) {
            if (num[i] < 3) continue;
            return true;
        }
        return false;
    }

    private GroupCard getXamChi() {
        int i;
        int[] num = new int[15];
        for (i = 0; i < 15; ++i) {
            num[i] = 0;
        }
        for (i = 0; i < this.GetNumOfCards(); ++i) {
            int[] arrn = num;
            int n = this.cards.get(i).GetNumber();
            arrn[n] = arrn[n] + 1;
        }
        for (i = 14; i >= 0; --i) {
            if (num[i] < 3) continue;
            GroupCard gc = new GroupCard();
            for (int j = 0; j < this.GetNumOfCards(); ++j) {
                if (this.cards.get(j).GetNumber() == i) {
                    gc.AddCard(this.cards.get(j));
                }
                if (gc.GetNumOfCards() == 3) break;
            }
            return gc;
        }
        return null;
    }

    private boolean hasSanh() {
        if (this.GetNumOfCards() < 5) {
            return false;
        }
        this.IncreaseSort();
        for (int i = 0; i < this.GetNumOfCards() - 4; ++i) {
            int j;
            int index = 1;
            int num = this.cards.get(i).GetNumber();
            if (this.cards.get(i).GetNumber() == 2 && this.cards.get(this.GetNumOfCards() - 1).GetNumber() == 14) {
                for (j = i + 1; j < this.GetNumOfCards(); ++j) {
                    int num1 = this.cards.get(j).GetNumber();
                    if (num + index != num1) continue;
                    ++index;
                }
                if (index < 4) continue;
                return true;
            }
            for (j = i + 1; j < this.GetNumOfCards(); ++j) {
                if (num + index != this.cards.get(j).GetNumber()) continue;
                ++index;
            }
            if (index < 5) continue;
            return true;
        }
        return false;
    }

    private GroupCard getSanh() {
        if (this.GetNumOfCards() < 5) {
            return null;
        }
        this.IncreaseSort();
        for (int i = 0; i < this.GetNumOfCards() - 4; ++i) {
            int j;
            int index = 1;
            int num = this.cards.get(i).GetNumber();
            GroupCard gc = new GroupCard();
            gc.AddCard(this.cards.get(i));
            if (this.cards.get(i).GetNumber() == 2 && this.cards.get(this.GetNumOfCards() - 1).GetNumber() == 14) {
                gc.AddCard(this.cards.get(this.GetNumOfCards() - 1));
                for (j = i + 1; j < this.GetNumOfCards(); ++j) {
                    int num1 = this.cards.get(j).GetNumber();
                    if (num + index != num1) continue;
                    ++index;
                    gc.AddCard(this.cards.get(j));
                    if (gc.GetNumOfCards() == 5) break;
                }
                if (gc.GetNumOfCards() != 5) continue;
                return gc;
            }
            for (j = i + 1; j < this.GetNumOfCards(); ++j) {
                if (num + index != this.cards.get(j).GetNumber()) continue;
                ++index;
                gc.AddCard(this.cards.get(j));
                if (gc.GetNumOfCards() == 5) break;
            }
            if (gc.GetNumOfCards() != 5) continue;
            return gc;
        }
        return null;
    }

    private boolean hasThung() {
        if (this.GetNumOfCards() < 5) {
            return false;
        }
        for (int i = 3; i >= 0; --i) {
            if (this.getNumCardSuit(i) < 5) continue;
            return true;
        }
        return false;
    }

    private GroupCard getThung() {
        if (this.GetNumOfCards() < 5) {
            return null;
        }
        GroupCard result = new GroupCard();
        for (int i = 3; i >= 0; --i) {
            if (this.getNumCardSuit(i) < 5) continue;
            GroupCard gc = this.getCardsSameSuit(i);
            gc.DecreaseSort();
            for (int j = 0; j < gc.GetNumOfCards() && j < 5; ++j) {
                result.AddCard(gc.Cards().get(j));
            }
            return result;
        }
        return null;
    }

    private GroupCard getCardsSameSuit(int suit) {
        GroupCard gc = new GroupCard();
        for (int i = 0; i < this.GetNumOfCards(); ++i) {
            if (this.cards.get(i).GetSuit() != suit) continue;
            gc.AddCard(this.cards.get(i));
        }
        return gc;
    }

    private boolean hasCulu() {
        int i;
        if (this.GetNumOfCards() < 5) {
            return false;
        }
        int[] num = new int[15];
        for (i = 0; i < 15; ++i) {
            num[i] = 0;
        }
        for (i = 0; i < this.GetNumOfCards(); ++i) {
            int n = this.cards.get(i).GetNumber();
            int[] arrn = num;
            int n2 = n;
            arrn[n2] = arrn[n2] + 1;
        }
        int k = 0;
        for (i = 14; i >= 0; --i) {
            if (num[i] < 3) continue;
            ++k;
            num[i] = 0;
            break;
        }
        for (i = 14; i >= 0; --i) {
            if (num[i] < 2) continue;
            ++k;
            num[i] = 0;
            break;
        }
        return k >= 2;
    }

    private GroupCard getCulu() {
        int i;
        int count;
        int j;
        if (this.GetNumOfCards() < 5) {
            return null;
        }
        GroupCard Result = new GroupCard();
        int[] num = new int[15];
        for (i = 0; i < 15; ++i) {
            num[i] = 0;
        }
        for (i = 0; i < this.GetNumOfCards(); ++i) {
            int n = this.cards.get(i).GetNumber();
            int[] arrn = num;
            int n2 = n;
            arrn[n2] = arrn[n2] + 1;
        }
        int k = 0;
        block2 : for (i = 14; i >= 0; --i) {
            if (num[i] < 3) continue;
            ++k;
            num[i] = 0;
            count = 0;
            for (j = 0; j < this.GetNumOfCards(); ++j) {
                if (this.cards.get(j).GetNumber() != i) continue;
                Result.AddCard(this.cards.get(j));
                if (++count == 3) break block2;
            }
            break;
        }
        block4 : for (i = 0; i < 15; ++i) {
            if (num[i] < 2) continue;
            ++k;
            num[i] = 0;
            count = 0;
            for (j = 0; j < this.GetNumOfCards(); ++j) {
                if (this.cards.get(j).GetNumber() != i) continue;
                Result.AddCard(this.cards.get(j));
                if (++count == 2) break block4;
            }
            break;
        }
        if (k == 2) {
            return Result;
        }
        return null;
    }

    private boolean hasThungPhaSanh() {
        if (this.GetNumOfCards() < 5) {
            return false;
        }
        for (int i = 3; i >= 0; --i) {
            int nSuit = this.getNumCardSuit(i);
            if (nSuit < 5) continue;
            GroupCard gc = this.getGroupCardBySuit(i);
            gc.IncreaseSort();
            for (int z = 0; z < gc.GetNumOfCards(); ++z) {
                int j;
                int num1;
                int index = 1;
                int k = 1;
                int num = gc.Cards().get(z).GetNumber();
                if (gc.Cards().get(z).GetNumber() == 2 && gc.Cards().get(gc.GetNumOfCards() - 1).GetNumber() == 14) {
                    for (j = 1 + z; j < gc.GetNumOfCards() - 1 && k < 4; ++j) {
                        num1 = gc.Cards().get(j).GetNumber();
                        if (num + index != num1) continue;
                        ++k;
                        ++index;
                    }
                    if (k < 4) continue;
                    return true;
                }
                for (j = 1 + z; j < gc.GetNumOfCards() && k < 5; ++j) {
                    num1 = gc.Cards().get(j).GetNumber();
                    if (num + index != num1) continue;
                    ++k;
                    ++index;
                }
                if (k < 5) continue;
                return true;
            }
        }
        return false;
    }

    public synchronized void IncreaseSort() {
        this.xepTangTuHai();
    }

    public synchronized void DecreaseSort() {
        this.xepGiam();
    }

    private GroupCard getThungPhaSanh() {
        if (this.GetNumOfCards() < 5) {
            return null;
        }
        for (int i = 3; i >= 0; --i) {
            int nSuit = this.getNumCardSuit(i);
            if (nSuit < 5) continue;
            GroupCard gc = this.getGroupCardBySuit(i);
            gc.IncreaseSort();
            for (int z = 0; z < gc.GetNumOfCards(); ++z) {
                int j;
                GroupCard Result = new GroupCard();
                int index = 1;
                int k = 1;
                int num = gc.Cards().get(z).GetNumber();
                Result.AddCard(gc.Cards().get(z));
                if (gc.Cards().get(z).GetNumber() == 2) {
                    for (j = 1 + z; j < gc.GetNumOfCards() && k < 5; ++j) {
                        if (num + index != gc.Cards().get(j).GetNumber() && gc.Cards().get(j).GetNumber() != 14) continue;
                        Result.AddCard(gc.Cards().get(j));
                        ++k;
                        ++index;
                    }
                    if (k != 5) continue;
                    return Result;
                }
                for (j = 1 + z; j < gc.GetNumOfCards() && k < 5; ++j) {
                    if (num + index != gc.Cards().get(j).GetNumber()) continue;
                    Result.AddCard(gc.Cards().get(j));
                    ++k;
                    ++index;
                }
                if (k != 5) continue;
                return Result;
            }
        }
        return null;
    }

    private GroupCard getGroupCardBySuit(int suit) {
        GroupCard gc = new GroupCard();
        for (int i = 0; i < this.GetNumOfCards(); ++i) {
            if (this.cards.get(i).GetSuit() != suit) continue;
            gc.AddCard(this.cards.get(i));
        }
        return gc;
    }

    public synchronized int GetNumOfCards() {
        return this.cards.size();
    }

    public int getNumCardSuit(int suit) {
        int nCount = 0;
        for (int i = 0; i < this.GetNumOfCards(); ++i) {
            if (this.cards.get(i).GetSuit() != suit) continue;
            ++nCount;
        }
        return nCount;
    }

    private void RemoveGroupCards(GroupCard gc) {
        for (int i = 0; i < gc.GetNumOfCards(); ++i) {
            if (!this.searchCard(gc.Cards().get(i))) continue;
            this.RemoveCard(gc.Cards().get(i));
        }
    }

    public synchronized void RemoveCard(Card card) {
        if (this.cards.size() == 0) {
            return;
        }
        for (int i = 0; i < this.cards.size(); ++i) {
            if (this.cards.get((int)i).ID != card.ID) continue;
            this.cards.remove(i);
        }
    }

    private boolean searchCard(Card c) {
        for (int i = 0; i < this.GetNumOfCards(); ++i) {
            if (this.cards.get((int)i).ID != c.ID) continue;
            return true;
        }
        return false;
    }

    private int hasTuQuy() {
        int i;
        if (this.GetNumOfCards() < 4) {
            return -1;
        }
        int[] num = new int[15];
        for (i = 14; i >= 0; --i) {
            num[i] = 0;
        }
        for (i = 0; i < this.GetNumOfCards(); ++i) {
            int n = this.cards.get(i).GetNumber();
            int[] arrn = num;
            int n2 = n;
            arrn[n2] = arrn[n2] + 1;
            if (num[n] != 4) continue;
            return n;
        }
        return -1;
    }

    private GroupCard getCardsSameNumber(int num) {
        GroupCard Result = new GroupCard();
        for (int i = 0; i < this.GetNumOfCards(); ++i) {
            if (this.cards.get(i).GetNumber() != num) continue;
            Result.AddCard(this.cards.get(i));
        }
        return Result;
    }

    public GroupCard copy() {
        GroupCard gc = new GroupCard();
        for (int i = 0; i < this.GetNumOfCards(); ++i) {
            Card c = Card.createCard(this.cards.get(i));
            gc.AddCard(c);
        }
        return gc;
    }

    public synchronized void AddCard(Card card) {
        this.cards.add(card);
    }

    public List<Card> Cards() {
        return this.cards;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public byte[] toByteArray() {
        List<Card> list = this.cards;
        synchronized (list) {
            byte[] c = new byte[this.cards.size()];
            for (int i = 0; i < this.cards.size(); ++i) {
                c[i] = (byte)this.cards.get((int)i).ID;
            }
            return c;
        }
    }

    public void sortLucPheBon() {
        int i;
        if (this.cards.size() != 13) {
            return;
        }
        Vector<ArrayList<Card>> bo = new Vector<ArrayList<Card>>(13);
        for (int i2 = 0; i2 < 13; ++i2) {
            bo.add(i2, null);
        }
        int count = 0;
        for (int i3 = 0; i3 < this.cards.size(); ++i3) {
            Card c = this.cards.get(i3);
            ArrayList<Card> bai = (ArrayList<Card>)bo.get(c.SO - 2);
            if (bai == null) {
                bai = new ArrayList<Card>();
                bo.set(c.SO - 2, bai);
                ++count;
            }
            bai.add(c);
        }
        List tuquy1 = null;
        List tuquy2 = null;
        List tuquy3 = null;
        List sam = null;
        ArrayList doi = new ArrayList();
        int tq = 0;
        Card le = null;
        for (i = 0; i < bo.size(); ++i) {
            List bai = (List)bo.get(i);
            if (bai == null) continue;
            if (bai.size() == 1) {
                le = (Card)bai.get(0);
                continue;
            }
            if (bai.size() == 2) {
                doi.add(bai.get(0));
                doi.add(bai.get(1));
                continue;
            }
            if (bai.size() == 3) {
                sam = bai;
                continue;
            }
            if (tq == 0) {
                tuquy1 = bai;
                ++tq;
                continue;
            }
            if (tq == 1) {
                tuquy2 = bai;
                ++tq;
                continue;
            }
            if (tq != 2) continue;
            tuquy3 = bai;
        }
        for (i = 0; i < 13; ++i) {
            this.cards.set(i, null);
        }
        if (sam != null) {
            this.cards.set(10, (Card)sam.get(0));
            this.cards.set(11, (Card)sam.get(1));
            this.cards.set(4, (Card)sam.get(2));
        }
        if (tuquy1 != null) {
            this.cards.set(0, (Card)tuquy1.get(0));
            this.cards.set(1, (Card)tuquy1.get(1));
            this.cards.set(5, (Card)tuquy1.get(2));
            this.cards.set(6, (Card)tuquy1.get(3));
        }
        if (tuquy2 != null) {
            this.cards.set(2, (Card)tuquy2.get(0));
            this.cards.set(3, (Card)tuquy2.get(1));
            this.cards.set(7, (Card)tuquy2.get(2));
            this.cards.set(8, (Card)tuquy2.get(3));
        }
        if (tuquy3 != null) {
            this.cards.set(10, (Card)tuquy3.get(0));
            this.cards.set(11, (Card)tuquy3.get(1));
            this.cards.set(9, (Card)tuquy3.get(2));
            this.cards.set(4, (Card)tuquy3.get(3));
        }
        if (le != null) {
            this.cards.set(12, le);
        }
        if (doi != null) {
            block4 : for (i = 0; i < doi.size(); ++i) {
                Card c = (Card)doi.get(i);
                for (int j = this.cards.size() - 1; j >= 0; --j) {
                    if (this.cards.get(j) != null) continue;
                    this.cards.set(j, c);
                    continue block4;
                }
            }
        }
    }

    public void sortBaSanh() {
        this.cards.clear();
        if (this.baSanh != null) {
            this.cards.addAll(this.baSanh.cards);
        }
    }

    public void sortBaThung() {
        int i;
        ArrayList<Card> bich = new ArrayList<Card>();
        ArrayList<Card> tep = new ArrayList<Card>();
        ArrayList<Card> ro = new ArrayList<Card>();
        ArrayList<Card> co = new ArrayList<Card>();
        for (i = 0; i < this.cards.size(); ++i) {
            Card c = this.cards.get(i);
            if (c.CHAT == 0) {
                bich.add(c);
                continue;
            }
            if (c.CHAT == 1) {
                tep.add(c);
                continue;
            }
            if (c.CHAT == 2) {
                ro.add(c);
                continue;
            }
            if (c.CHAT != 3) continue;
            co.add(c);
        }
        for (i = 0; i < this.cards.size(); ++i) {
            this.cards.set(i, null);
        }
        this.chenBaCaiThung(bich);
        this.chenBaCaiThung(tep);
        this.chenBaCaiThung(ro);
        this.chenBaCaiThung(co);
    }

    private void chenBaCaiThung(List<Card> listCard) {
        int i;
        int from;
        if (listCard.size() == 3) {
            from = 10;
            for (i = 0; i < listCard.size(); ++i) {
                this.cards.set(from + i, listCard.get(i));
            }
        }
        if (listCard.size() == 5) {
            from = 0;
            if (this.cards.get(0) != null) {
                from = 5;
            }
            for (i = 0; i < listCard.size(); ++i) {
                this.cards.set(i + from, listCard.get(i));
            }
        }
        if (listCard.size() == 8) {
            int from1 = 0;
            if (this.cards.get(3) != null) {
                from1 = 5;
            }
            for (i = 0; i < 5; ++i) {
                this.cards.set(i + from1, listCard.get(i));
            }
            int from2 = 10;
            for (int i2 = 0; i2 < 3; ++i2) {
                this.cards.set(from2 + i2, listCard.get(5 + i2));
            }
        }
        if (listCard.size() == 10) {
            for (int i3 = 0; i3 < listCard.size(); ++i3) {
                this.cards.set(i3, listCard.get(i3));
            }
        }
    }

    public void reset() {
        this.cards.clear();
        this.BO = 18;
    }

    public int GetMaxNumber() {
        int max = -1;
        for (int i = 0; i < this.cards.size(); ++i) {
            Card c = this.cards.get(i);
            if (c.SO <= max) continue;
            max = c.SO;
        }
        return max;
    }

    public int GetMaxNumberByBo(int bo, int rule) {
        if (bo == 8) {
            if (rule == 0) {
                return this.cards.get((int)(this.cards.size() - 1)).SO;
            }
            return this.GetMaxNumber();
        }
        if (bo == 12) {
            return this.cards.get((int)(this.cards.size() - 1)).SO;
        }
        return this.cards.get((int)0).SO;
    }

    public Card GetMaxCard() {
        int max = -1;
        Card maxCard = null;
        for (int i = 0; i < this.cards.size(); ++i) {
            Card c = this.cards.get(i);
            if (c.SO <= max) continue;
            max = c.SO;
            maxCard = c;
        }
        return maxCard;
    }

    public int GetSecondMaxNumber() {
        int max1 = -1;
        int max2 = -1;
        for (int i = 0; i < this.cards.size(); ++i) {
            Card c = this.cards.get(i);
            if (c.SO > max1) {
                max2 = max1;
                max1 = c.SO;
                continue;
            }
            if (c.SO <= max2) continue;
            max2 = c.SO;
        }
        return max2;
    }

    public boolean hasA() {
        return this.demSoAt() > 0;
    }

    public int demSoAt() {
        int count = 0;
        for (int i = 0; i < this.cards.size(); ++i) {
            Card c = this.cards.get(i);
            if (c.SO != 14) continue;
            ++count;
        }
        return count;
    }

    public boolean coTuQuyAt() {
        return this.demSoAt() == 4;
    }

    public boolean isNoHu(int rule) {
        return this.kiemtraBo(rule) == 0;
    }

    public int hashCode() {
        int product = 1;
        for (Card c : this.cards) {
            product *= c.ID;
        }
        return product;
    }

    public boolean equals(Object o) {
        if (o instanceof GroupCard) {
            int i;
            Card c;
            GroupCard gc = (GroupCard)o;
            for (i = 0; i < this.cards.size(); ++i) {
                c = this.cards.get(i);
                if (gc.cards.contains(c)) continue;
                return false;
            }
            for (i = 0; i < gc.cards.size(); ++i) {
                c = gc.cards.get(i);
                if (this.cards.contains(c)) continue;
                return false;
            }
            return true;
        }
        return false;
    }

}

