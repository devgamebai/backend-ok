/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.Debug
 */
package game.sam.server.logic;

import bitzero.util.common.business.Debug;
import game.sam.server.logic.Card;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GroupCard
implements Comparable {
    private static final String[] group_names = new String[]{"NO", "M", "D", "B", "TQ", "HTQ", "S"};
    public static final int NONE = 0;
    public static final int MOT = 1;
    public static final int DOI = 2;
    public static final int BA = 3;
    public static final int TU_QUY = 4;
    public static final int HAI_TU_QUY = 5;
    public static final int SANH = 6;
    public static final int SAM_DINH = 1;
    public static final int NAM_DOI = 2;
    public static final int TU_HEO = 3;
    public static final int DONG_MAU = 4;
    public int BO = 0;
    public List<Card> cards = new ArrayList<Card>();

    public GroupCard() {
    }

    public GroupCard(byte[] ids) {
        for (byte id : ids) {
            Card card = new Card(id);
            this.cards.add(card);
        }
        Collections.sort(this.cards);
        this.kiemtraBo();
    }

    public GroupCard(int[] ids) {
        for (int id : ids) {
            Card card = new Card(id);
            this.cards.add(card);
        }
        Collections.sort(this.cards);
        this.kiemtraBo();
    }

    public void init(byte[] ids) {
        for (byte id : ids) {
            Card card = new Card(id);
            this.cards.add(card);
        }
    }

    public boolean treoBai() {
        return this.cards.size() == 10;
    }

    public int demLaPhat() {
        if (this.cards.size() == 10) {
            return this.cards.size();
        }
        if (this.cards.size() - this.demLaHeoTuQuy() < 0) {
            return 0;
        }
        return this.cards.size() - this.demLaHeoTuQuy();
    }

    public int demLaHeoTuQuy() {
        int card = 0;
        int laTuQuy = 1;
        for (int i = 0; i < this.cards.size() - 1; ++i) {
            Card c1 = this.getCardAt(i);
            Card c2 = this.getCardAt(i + 1);
            if (c1.SO == 12) {
                ++card;
            }
            if (c1.SO == c2.SO) {
                if (++laTuQuy != 4) continue;
                card += 4;
                continue;
            }
            laTuQuy = 1;
        }
        Card lastCard = this.getLastCard();
        if (lastCard.SO == 12) {
            ++card;
        }
        return card;
    }

    public int demLaThoi() {
        int card = 0;
        int laTuQuy = 1;
        for (int i = 0; i < this.cards.size() - 1; ++i) {
            Card c1 = this.getCardAt(i);
            Card c2 = this.getCardAt(i + 1);
            if (c1.SO == 12) {
                card += 10;
            }
            if (c1.SO == c2.SO) {
                if (++laTuQuy != 4) continue;
                card += 20;
                continue;
            }
            laTuQuy = 1;
        }
        Card lastCard = this.getLastCard();
        if (lastCard.SO == 12) {
            card += 10;
        }
        return card;
    }

    private boolean contains(GroupCard gc) {
        if (this.cards.size() < gc.cards.size()) {
            return false;
        }
        int hit = 0;
        int currentIndex = 0;
        block0 : for (int i = 0; i < gc.cards.size(); ++i) {
            Card c = gc.cards.get(i);
            for (int j = currentIndex; j < this.cards.size(); ++j) {
                if (c.ID != this.cards.get((int)j).ID) continue;
                ++hit;
                currentIndex = j + 1;
                continue block0;
            }
        }
        return hit == gc.cards.size();
    }

    public boolean minusCard(GroupCard gc) {
        if (this.contains(gc)) {
            int j = gc.cards.size() - 1;
            for (int i = this.cards.size() - 1; i >= 0; --i) {
                if (this.cards.get((int)i).ID != gc.cards.get((int)j).ID) continue;
                this.cards.remove(i);
                if (--j < 0) break;
            }
            return true;
        }
        return false;
    }

    public String toShortString() {
        StringBuilder sb = new StringBuilder("#");
        sb.append(group_names[this.BO]).append(":");
        int i = 0;
        for (Card c : this.cards) {
            if (++i == 3) {
                sb.append(c.name).append("\n");
                continue;
            }
            sb.append(c.name);
        }
        sb.append("$");
        return sb.toString();
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

    public boolean chatDuoc(GroupCard c) {
        return this.compareTo(c) > 0;
    }

    public int compareTo(Object o) {
        if (o instanceof GroupCard) {
            GroupCard gc = (GroupCard)o;
            if (gc.BO == 1 && this.BO == 1 || gc.BO == 2 && this.BO == 2 || gc.BO == 3 && this.BO == 3 || gc.BO == 4 && this.BO == 4) {
                return this.cards.get(0).soSanhSo(gc.cards.get(0));
            }
            if (gc.BO == 6 && this.BO == 6) {
                return this.sosanhSanh(gc);
            }
            if (gc.BO == 4 && this.BO == 1 && this.cards.get((int)0).SO == 12) {
                return -1;
            }
            if (this.BO == 4 && gc.BO == 1 && gc.cards.get((int)0).SO == 12) {
                return 1;
            }
            if (gc.BO == 5 && this.BO == 2 && this.cards.get((int)0).SO == 12) {
                return -1;
            }
            if (this.BO == 5 && gc.BO == 2 && gc.cards.get((int)0).SO == 12) {
                return 1;
            }
            if (gc.BO == 5 && this.BO == 5) {
                int x = this.cards.get(3).soSanhSo(gc.cards.get(3));
                int y = this.cards.get(7).soSanhSo(gc.cards.get(7));
                if (x > 0 && y > 0) {
                    return 1;
                }
                if (x < 0 && y < 0) {
                    return -1;
                }
                return 0;
            }
            return 0;
        }
        return 0;
    }

    private void kiemtraBo() {
        this.BO = this.cards.size() == 1 ? 1 : (this.kiemtraDoi() ? 2 : (this.kiemtraBa() ? 3 : (this.kiemtraTuQuy() ? 4 : (this.kiemtraHaiTuQuy() ? 5 : (this.kiemtraSanh() ? 6 : 0)))));
    }

    private boolean kiemtraDoi() {
        if (this.cards.size() == 2) {
            return this.cards.get((int)0).SO == this.cards.get((int)1).SO;
        }
        return false;
    }

    private boolean kiemtraBa() {
        if (this.cards.size() == 3) {
            return this.cards.get((int)0).SO == this.cards.get((int)2).SO;
        }
        return false;
    }

    private boolean kiemtraTuQuy() {
        if (this.cards.size() == 4) {
            return this.cards.get((int)0).SO == this.cards.get((int)3).SO;
        }
        return false;
    }

    private boolean kiemtraHaiTuQuy() {
        if (this.cards.size() == 8) {
            return this.cards.get((int)0).SO == this.cards.get((int)3).SO && this.cards.get((int)4).SO == this.cards.get((int)7).SO;
        }
        return false;
    }

    private Card getCardAt(int index) {
        return this.cards.get(index);
    }

    public Card getLastCard() {
        if (this.cards.size() != 0) {
            return this.cards.get(this.cards.size() - 1);
        }
        return null;
    }

    public Card getAlmostLastCard() {
        if (this.cards.size() != 0) {
            return this.cards.get(this.cards.size() - 2);
        }
        return null;
    }

    private Card getFirstCard() {
        if (this.cards.size() != 0) {
            return this.cards.get(0);
        }
        return null;
    }

    private boolean kiemtraSanh() {
        if (this.cards.size() < 3) {
            return false;
        }
        if (this.getLastCard().SO != 12) {
            return this.kiemtraSanhTrongKhoang(0, this.cards.size());
        }
        return this.kiemtraSanhChuaHai();
    }

    private int sosanhSanh(GroupCard groupCard) {
        if (this.cards.size() != groupCard.cards.size()) {
            return 0;
        }
        int size = this.cards.size();
        if (this.getLastCard().SO != 12 && groupCard.getLastCard().SO != 12) {
            return this.getFirstCard().soSanhSo(groupCard.getFirstCard());
        }
        if (this.getLastCard().SO == 12 && groupCard.getLastCard().SO != 12) {
            return -1;
        }
        if (this.getLastCard().SO != 12 && groupCard.getLastCard().SO == 12) {
            return 1;
        }
        if (this.getAlmostLastCard().SO == 11 && this.getAlmostLastCard().SO == 11) {
            return this.cards.get(size - 3).soSanhSo(groupCard.cards.get(size - 3));
        }
        if (this.getAlmostLastCard().SO == 11) {
            return -1;
        }
        return 1;
    }

    public boolean kiemtraSanhTrongKhoang(int from, int to) {
        for (int i = 1; i < to - from; ++i) {
            if (this.cards.get((int)from).SO + i == this.cards.get((int)(from + i)).SO) continue;
            return false;
        }
        return true;
    }

    private boolean kiemtraSanhChuaHai() {
        if (this.getFirstCard().SO != 0) {
            return false;
        }
        int size = this.cards.size();
        if (this.cards.get((int)(size - 2)).SO == 11) {
            if (size == 3) {
                return true;
            }
            return this.kiemtraSanhTrongKhoang(0, size - 2);
        }
        return this.kiemtraSanhTrongKhoang(0, size - 1);
    }

    public int kiemTraToiTrang() {
        if (this.cards.size() < 10) {
            return 0;
        }
        if (this.BO == 6) {
            return 1;
        }
        if (this.kiemTra5Doi()) {
            return 2;
        }
        if (this.kiemTraTuHeo()) {
            return 3;
        }
        if (this.kiemTraDongMau()) {
            return 4;
        }
        return 0;
    }

    private boolean kiemTra5Doi() {
        for (int i = 0; i < this.cards.size() - 1; i += 2) {
            Card c1 = this.getCardAt(i);
            Card c2 = this.getCardAt(i + 1);
            if (c1.SO == c2.SO) continue;
            return false;
        }
        return true;
    }

    private boolean kiemTraTuHeo() {
        Card c = this.getCardAt(this.cards.size() - 4);
        return c.SO == 12;
    }

    private boolean kiemTraDongMau() {
        for (int i = 0; i < this.cards.size() - 1; ++i) {
            Card c2;
            Card c1 = this.getCardAt(i);
            if (c1.dongMau(c2 = this.getCardAt(i + 1))) continue;
            return false;
        }
        return true;
    }

    public int tinhChatChong() {
        if (this.BO == 1 && this.cards.get((int)0).SO == 12) {
            return 20;
        }
        if (this.BO == 4) {
            return 40;
        }
        if (this.BO == 2 && this.cards.get((int)0).SO == 12) {
            return 40;
        }
        if (this.BO == 5) {
            return 80;
        }
        return 0;
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

    public static boolean kiemTraLonNhat(GroupCard cards, GroupCard subCards) {
        GroupCard sub = cards.timBoLonNhat(subCards.BO, subCards.cards.size());
        if (sub == null) {
            Debug.trace((Object[])new Object[]{cards.toString(), "co bo lon nhat chat bo", subCards.toString(), "la", "null"});
            return true;
        }
        Debug.trace((Object[])new Object[]{cards.toString(), "co bo lon nhat chat bo", subCards.toString(), "la", sub.toString()});
        return GroupCard.sosanhBang(subCards, sub);
    }

    public static boolean sosanhBang(GroupCard cards1, GroupCard cards2) {
        if (cards1.cards.size() != cards2.cards.size()) {
            return false;
        }
        for (int i = 0; i < cards1.cards.size(); ++i) {
            if (cards1.getCardAt((int)i).SO == cards2.getCardAt((int)i).SO) continue;
            return false;
        }
        return true;
    }

    public GroupCard timBoLonNhat(int bo, int size) {
        if (this.cards.size() < size) {
            return null;
        }
        int[] cards = new int[size];
        if (bo == 1) {
            cards[0] = this.getLastCard().ID;
        }
        if (bo == 2 && (cards = this.doiLonNhat()) == null) {
            return null;
        }
        if (bo == 3 && (cards = this.baLonNhat()) == null) {
            return null;
        }
        if (bo == 6) {
            int from = this.sanhLonNhat(size);
            if (from < 0) {
                return null;
            }
            for (int i = 0; i < size; ++i) {
                cards[i] = (byte)this.layIDTuSo((from + i) % 13);
            }
        }
        if (bo == 4 && (cards = this.tuquyLonNhat()) == null) {
            return null;
        }
        if (bo == 5 && (cards = this.haituquyLonNhat()) == null) {
            return null;
        }
        GroupCard gc = new GroupCard(cards);
        return gc;
    }

    public GroupCard timTuQuyNhoNhat() {
        int[] tuquy = this.tuquyNhoNhat();
        if (tuquy == null) {
            return null;
        }
        GroupCard gc = new GroupCard(tuquy);
        return gc;
    }

    public int[] haituquyLonNhat() {
        int[] tuquy = new int[8];
        int soTuQuy = 0;
        for (int i = this.cards.size() - 1; i >= 3; --i) {
            if (this.getCardAt((int)i).SO != this.getCardAt((int)(i - 1)).SO || this.getCardAt((int)i).SO != this.getCardAt((int)(i - 2)).SO || this.getCardAt((int)i).SO != this.getCardAt((int)(i - 3)).SO) continue;
            if (soTuQuy == 0) {
                tuquy[0] = this.getCardAt((int)i).ID;
                tuquy[1] = this.getCardAt((int)(i - 1)).ID;
                tuquy[2] = this.getCardAt((int)(i - 2)).ID;
                tuquy[3] = this.getCardAt((int)(i - 3)).ID;
                ++soTuQuy;
            }
            if (soTuQuy != 1) continue;
            tuquy[4] = this.getCardAt((int)i).ID;
            tuquy[5] = this.getCardAt((int)(i - 1)).ID;
            tuquy[6] = this.getCardAt((int)(i - 2)).ID;
            tuquy[7] = this.getCardAt((int)(i - 3)).ID;
            return tuquy;
        }
        return null;
    }

    public int[] tuquyNhoNhat() {
        int[] tuquy = new int[4];
        for (int i = 0; i < this.cards.size() - 4; ++i) {
            if (this.getCardAt((int)i).SO != this.getCardAt((int)(i + 1)).SO || this.getCardAt((int)i).SO != this.getCardAt((int)(i + 2)).SO || this.getCardAt((int)i).SO != this.getCardAt((int)(i + 3)).SO) continue;
            tuquy[0] = this.getCardAt((int)i).ID;
            tuquy[1] = this.getCardAt((int)(i + 1)).ID;
            tuquy[2] = this.getCardAt((int)(i + 2)).ID;
            tuquy[3] = this.getCardAt((int)(i + 3)).ID;
            return tuquy;
        }
        return null;
    }

    public int[] tuquyLonNhat() {
        int[] tuquy = new int[4];
        for (int i = this.cards.size() - 1; i >= 3; --i) {
            if (this.getCardAt((int)i).SO != this.getCardAt((int)(i - 1)).SO || this.getCardAt((int)i).SO != this.getCardAt((int)(i - 2)).SO || this.getCardAt((int)i).SO != this.getCardAt((int)(i - 3)).SO) continue;
            tuquy[0] = this.getCardAt((int)i).ID;
            tuquy[1] = this.getCardAt((int)(i - 1)).ID;
            tuquy[2] = this.getCardAt((int)(i - 2)).ID;
            tuquy[3] = this.getCardAt((int)(i - 3)).ID;
            return tuquy;
        }
        return null;
    }

    public int[] baLonNhat() {
        int[] ba = new int[3];
        for (int i = this.cards.size() - 1; i >= 2; --i) {
            if (this.getCardAt((int)i).SO != this.getCardAt((int)(i - 1)).SO || this.getCardAt((int)i).SO != this.getCardAt((int)(i - 2)).SO) continue;
            ba[0] = this.getCardAt((int)i).ID;
            ba[1] = this.getCardAt((int)(i - 1)).ID;
            ba[2] = this.getCardAt((int)(i - 2)).ID;
            return ba;
        }
        return null;
    }

    public int[] doiLonNhat() {
        int[] doi = new int[2];
        for (int i = this.cards.size() - 1; i >= 1; --i) {
            if (this.getCardAt((int)i).SO != this.getCardAt((int)(i - 1)).SO) continue;
            doi[0] = this.getCardAt((int)i).ID;
            doi[1] = this.getCardAt((int)(i - 1)).ID;
            return doi;
        }
        return null;
    }

    public int layIDTuSo(int so) {
        for (int i = 0; i < this.cards.size(); ++i) {
            if (this.cards.get((int)i).SO != so) continue;
            return this.cards.get((int)i).ID;
        }
        return 52;
    }

    public boolean coLaBaiSo(int so) {
        for (int i = 0; i < this.cards.size(); ++i) {
            if (this.cards.get((int)i).SO != so) continue;
            return true;
        }
        return false;
    }

    public boolean coSanhBatDauTu(int so, int size) {
        if (so == 12) {
            so = -1;
        }
        if (so + size > 11) {
            return false;
        }
        for (int i = 0; i < size; ++i) {
            if (this.coLaBaiSo(so + i)) continue;
            return false;
        }
        return true;
    }

    public int sanhLonNhat(int size) {
        for (int i = 9; i >= -1; i = (int)((byte)(i - 1))) {
            if (!this.coSanhBatDauTu(i, size)) continue;
            if (i == -1) {
                return 12;
            }
            return i;
        }
        return -1;
    }

    private static boolean maxMot(GroupCard cards, GroupCard gc) {
        return cards.getLastCard().soSanhSo(gc.getLastCard()) == 0;
    }

    private static boolean maxDoi(GroupCard cards, GroupCard gc) {
        for (int i = cards.cards.size() - 1; i >= 1; --i) {
            if (cards.getCardAt((int)i).SO != cards.getCardAt((int)(i - 1)).SO) continue;
            return gc.getLastCard().SO == cards.getCardAt((int)i).SO;
        }
        return false;
    }

    public static boolean kiemTraChatDuoc(GroupCard groupCard, GroupCard gc) {
        GroupCard sub = null;
        sub = gc.BO == 1 && gc.getFirstCard().SO != 12 ? groupCard.timBoLonNhat(1, 1) : (gc.BO == 1 && gc.getFirstCard().SO == 12 ? groupCard.timBoLonNhat(4, 4) : (gc.BO == 2 && gc.getFirstCard().SO == 12 ? groupCard.timBoLonNhat(5, 8) : groupCard.timBoLonNhat(gc.BO, gc.cards.size())));
        if (sub != null) {
            return sub.chatDuoc(gc);
        }
        return false;
    }

    public boolean coTheChatChong() {
        return this.BO == 1 && this.getFirstCard().SO == 12 || this.BO == 4 || this.BO == 2 && this.getFirstCard().SO == 12 || this.BO == 5;
    }

    public boolean kiemTraBoBaiDanhRa(GroupCard sub) {
        if (this.cards.size() < sub.cards.size()) {
            Debug.trace((Object)"Bo danh ra co nhieu la hon");
            return false;
        }
        int j = 0;
        boolean result = false;
        boolean danhDauThoiHai = this.cards.size() != sub.cards.size();
        for (int i = 0; i < this.cards.size(); ++i) {
            Card c2 = this.cards.get(i);
            if (j < sub.cards.size()) {
                Card c1 = sub.cards.get(j);
                if (c1.ID == c2.ID) {
                    if (++j != sub.cards.size()) continue;
                    result = true;
                    continue;
                }
                if (!danhDauThoiHai || c2.SO == 12) continue;
                danhDauThoiHai = false;
                continue;
            }
            if (!danhDauThoiHai || c2.SO == 12) continue;
            danhDauThoiHai = false;
        }
        if (danhDauThoiHai) {
            Debug.trace((Object)"Bo danh ra lam thoi 2");
            return false;
        }
        return result;
    }

    public boolean isNoHu() {
        return this.kiemTraToiTrang() == 1 && this.getLastCard().SO == 11;
    }
}

