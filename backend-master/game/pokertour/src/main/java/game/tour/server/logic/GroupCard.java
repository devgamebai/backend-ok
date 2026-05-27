/*
 * Decompiled with CFR 0.144.
 */
package game.tour.server.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class GroupCard {
    private static final String[] group_names = new String[]{"SV", "TPS", "TQ", "CL", "TH", "S", "X", "HD", "D", "R", "NO", "NULL"};
    public static final int GROUP_SIZE = 5;
    public static final int HAND_SIZE = 2;
    public static final int EG_SANH_VUA = 0;
    public static final int EG_THUNGPHASANH = 1;
    public static final int EG_TUQUY = 2;
    public static final int EG_CULU = 3;
    public static final int EG_THUNG = 4;
    public static final int EG_SANH = 5;
    public static final int EG_XAMCHI = 6;
    public static final int EG_2DOIKHACNHAU = 7;
    public static final int EG_MOTDOI = 8;
    public static final int EG_RAC = 9;
    public static final int NO_GROUP = 10;
    public static final int NONE = 11;
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
    private int BO = 11;
    public List<Card> cards = new ArrayList<Card>();

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
                    Card card = new Card(so, phanChat);
                    this.cards.add(card);
                    k = 0;
                    continue;
                }
                ++k;
            }
        }
    }

    public void fromNotSort(String data) {
        this.cards.clear();
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
                    so = phanSo.equalsIgnoreCase("A") ? 14 : (phanSo.equalsIgnoreCase("J") ? 11 : (phanSo.equalsIgnoreCase("Q") ? 11 : (phanSo.equalsIgnoreCase("K") ? 11 : Integer.parseInt(phanSo))));
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
                    Card card = new Card(so, phanChat);
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
            Card card = new Card(id);
            this.cards.add(card);
        }
    }

    public GroupCard(Integer[] ids) {
        Integer[] arrinteger = ids;
        int n = arrinteger.length;
        for (int i = 0; i < n; ++i) {
            int id = arrinteger[i];
            Card card = new Card(id);
            this.cards.add(card);
        }
    }

    public GroupCard(byte[] ids) {
        for (byte id : ids) {
            Card card = new Card(id);
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
        if (this.BO == 5 || this.BO == 1) {
            this.xepSanh();
        }
        if (this.BO == 2) {
            this.xepTuQuy();
        } else if (this.BO == 3) {
            this.xepCuLu();
        } else if (this.BO == 6) {
            this.xepSam();
        } else if (this.BO == 7 || this.BO == 8) {
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
        Card c1 = this.cards.get(i);
        int so1 = c1.SO;
        int chat1 = c1.CHAT;
        Card c2 = this.cards.get(j);
        c1.init(c2.SO, c2.CHAT);
        c2.init(so1, chat1);
    }

    public void xepSam() {
        int from = 0;
        int i = 0;
        for (i = 0; i < this.cards.size() - 1; ++i) {
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

    public void kiemTraBoNoSort() {
        GroupCard gc = this.copy();
        gc.kiemtraBo();
        this.BO = gc.BO;
    }

    public int kiemTraBoHaiLa() {
        Card c2;
        if (this.BO != 11) {
            return this.BO;
        }
        Card c1 = this.cards.get(0);
        this.BO = c1.dongSo(c2 = this.cards.get(1)) ? 8 : 9;
        return this.BO;
    }

    public int kiemtraBo() {
        int size = this.cards.size();
        if (size != 5 && size != 2) {
            this.xepGiam();
            this.BO = 10;
            return this.BO;
        }
        if (size == 2) {
            return this.kiemTraBoHaiLa();
        }
        this.xepGiam();
        int bo = this.doiThuSamCuluTuQuy();
        this.BO = bo != 11 ? bo : (this.thung() ? (this.sanh() ? (this.cards.get((int)0).SO == 14 && this.cards.get((int)1).SO == 13 ? 0 : 1) : 4) : (this.sanh() ? 5 : 9));
        this.xepBo();
        return this.BO;
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
            return 2;
        }
        if (max1 == 3 && max2 == 2) {
            return 3;
        }
        if (max1 == 3) {
            return 6;
        }
        if (max1 == 2 && max2 == 2) {
            return 7;
        }
        if (max1 == 2) {
            return 8;
        }
        return 11;
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

    public GroupCard getGroup() {
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
        int count;
        int j;
        int i;
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
                int num1;
                int j;
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
            gc.AddCard(this.cards.get(i));
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

    public void reset() {
        this.cards.clear();
        this.BO = 11;
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

    public GroupCard addCards(GroupCard communityCard) {
        GroupCard gc = new GroupCard();
        gc.cards.addAll(this.cards);
        gc.cards.addAll(communityCard.cards);
        return gc;
    }

    public boolean isNoHu() {
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public List<Integer> toIntegerList() {
        List<Card> list = this.cards;
        synchronized (list) {
            LinkedList<Integer> c = new LinkedList<Integer>();
            for (int i = 0; i < this.cards.size(); ++i) {
                c.add(this.cards.get((int)i).ID);
            }
            return c;
        }
    }

}

