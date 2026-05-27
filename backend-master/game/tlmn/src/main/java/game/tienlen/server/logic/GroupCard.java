/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.Debug
 */
package game.tienlen.server.logic;

import bitzero.util.common.business.Debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GroupCard
implements Comparable {
    private static final String[] group_names = new String[]{"NO", "M", "D", "B", "BT", "TT", "TQ", "S", "HTQ"};
    public static final int NONE = 0;
    public static final int MOT = 1;
    public static final int DOI = 2;
    public static final int BA = 3;
    public static final int BA_DOI_THONG = 4;
    public static final int BON_DOI_THONG = 5;
    public static final int TU_QUY = 6;
    public static final int SANH = 7;
    public static final int HAI_TU_QUY = 8;
    public static final int SANH_RONG = 1;
    public static final int NAM_DOI_THONG = 2;
    public static final int SAU_DOI = 3;
    public static final int TU_HEO = 4;
    public static final int DONG_MAU_13 = 5;
    public static final int DONG_MAU_12 = 6;
    public int BO = 0;
    public List<Card> cards = new ArrayList<Card>();

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

    public boolean treoBai() {
        return this.cards.size() == 13;
    }

    public DemLaThoi demLaThoi() {
        DemLaThoi kq = new DemLaThoi();
        GroupCard gc = this.clone();
        GroupCard sub = gc.layRaBonDoiThong();
        if (sub != null) {
            kq.soLaThoi += 8;
            kq.soLaPhat += 16;
        }
        if ((sub = gc.layRaTuQuy()) != null) {
            kq.soLaThoi += 4;
            kq.soLaPhat += 12;
        }
        if ((sub = gc.layRaTuQuy()) != null) {
            kq.soLaThoi += 4;
            kq.soLaPhat += 12;
        }
        if ((sub = gc.layRaBaDoiThong()) != null) {
            kq.soLaThoi += 6;
            kq.soLaPhat += 12;
        }
        for (int i = 0; i < gc.cards.size(); ++i) {
            Card c = gc.getCardAt(i);
            int soLaPhat = c.demSoLaPhat();
            if (soLaPhat <= 0) continue;
            ++kq.soLaThoi;
            kq.soLaPhat += soLaPhat;
        }
        return kq;
    }

    public GroupCard clone() {
        GroupCard gc = new GroupCard();
        for (int i = 0; i < this.cards.size(); ++i) {
            Card c = this.cards.get(i);
            gc.addCardNoSort(c);
        }
        return gc;
    }

    public GroupCard layRaBo(int size, int bo) {
        int i;
        if (this.cards.size() < size) {
            return null;
        }
        int from = -1;
        GroupCard gc = null;
        for (i = 0; i <= this.cards.size() - size; ++i) {
            gc = this.getSubCard(i, i + size);
            gc.kiemtraBo();
            if (gc.BO != bo) continue;
            from = i;
            break;
        }
        if (from == -1) {
            return null;
        }
        for (i = from + size - 1; i >= from; --i) {
            this.cards.remove(from);
        }
        return gc;
    }

    public GroupCard layRaBonDoiThong() {
        return this.layRaDoiThong(4, true);
    }

    public GroupCard layRaBaDoiThong() {
        return this.layRaDoiThong(3, true);
    }

    public GroupCard layRaTuQuy() {
        return this.layRaBo(4, 6);
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
            if (gc.BO == 1 && this.BO == 1 || gc.BO == 2 && this.BO == 2 || gc.BO == 3 && this.BO == 3 || gc.BO == 6 && this.BO == 6 || gc.BO == 4 && this.BO == 4 || gc.BO == 5 && this.BO == 5) {
                return this.getLastCard().compareTo(gc.getLastCard());
            }
            if (gc.BO == 7 && this.BO == 7) {
                return this.sosanhSanh(gc);
            }
            if (gc.BO == 4 && this.BO == 1 && this.cards.get((int)0).SO == 12) {
                return -1;
            }
            if (this.BO == 4 && gc.BO == 1 && gc.cards.get((int)0).SO == 12) {
                return 1;
            }
            if (gc.BO == 6 && this.BO == 1 && this.cards.get((int)0).SO == 12) {
                return -1;
            }
            if (this.BO == 6 && gc.BO == 1 && gc.cards.get((int)0).SO == 12) {
                return 1;
            }
            if (gc.BO == 6 && this.BO == 2 && this.cards.get((int)0).SO == 12) {
                return -1;
            }
            if (this.BO == 6 && gc.BO == 2 && gc.cards.get((int)0).SO == 12) {
                return 1;
            }
            if (gc.BO == 5 && this.BO == 1 && this.cards.get((int)0).SO == 12) {
                return -1;
            }
            if (this.BO == 5 && gc.BO == 1 && gc.cards.get((int)0).SO == 12) {
                return 1;
            }
            if (gc.BO == 5 && this.BO == 2 && this.cards.get((int)0).SO == 12) {
                return -1;
            }
            if (this.BO == 5 && gc.BO == 2 && gc.cards.get((int)0).SO == 12) {
                return 1;
            }
            if (gc.BO == 6 && this.BO == 4) {
                return -1;
            }
            if (this.BO == 6 && gc.BO == 4) {
                return 1;
            }
            if (gc.BO == 5 && this.BO == 4) {
                return -1;
            }
            if (this.BO == 5 && gc.BO == 4) {
                return 1;
            }
            if (gc.BO == 5 && this.BO == 6) {
                return -1;
            }
            if ((this.BO == 5) && (gc.BO == 6))
                return 1;
        }
        return 0;
    }

    private void kiemtraBo() {
        if (this.BO != 0) {
            return;
        }
        this.BO = this.cards.size() == 1 ? 1 : (this.isDoi() ? 2 : (this.isBa() ? 3 : (this.isTuQuy() ? 6 : (this.isBaDoiThong() ? 4 : (this.isBonDoiThong() ? 5 : (this.isSanh() ? 7 : 0))))));
    }

    private boolean isDoi() {
        if (this.cards.size() == 2) {
            return this.cards.get((int)0).SO == this.cards.get((int)1).SO;
        }
        return false;
    }

    private boolean isBa() {
        if (this.cards.size() == 3) {
            return this.cards.get((int)0).SO == this.cards.get((int)2).SO;
        }
        return false;
    }

    private boolean isTuQuy() {
        if (this.cards.size() == 4) {
            return this.cards.get((int)0).SO == this.cards.get((int)3).SO;
        }
        return false;
    }

    private boolean isBaDoiThong() {
        return this.isDoiThong(3);
    }

    private boolean isDoiThong(int soDoiThong) {
        if (this.cards.size() != 2 * soDoiThong || this.getLastCard().SO == 12) {
            return false;
        }
        Card prev = null;
        int count = 0;
        int doi = 0;
        for (int i = 0; i < this.cards.size(); ++i) {
            Card c = this.getCardAt(i);
            if (prev != null) {
                if (c.SO == prev.SO) {
                    if (doi == 0) {
                        ++count;
                    } else if (doi > 1) {
                        return false;
                    }
                    ++doi;
                } else if (c.SO == prev.SO + 1) {
                    doi = 0;
                } else {
                    return false;
                }
            }
            prev = c;
        }
        return count == soDoiThong;
    }

    private boolean isBonDoiThong() {
        return this.isDoiThong(4);
    }

    private GroupCard layRaDoiThong(int size, boolean remove) {
        int count = 0;
        int doi = 0;
        Card prev = null;
        GroupCard gc = new GroupCard();
        for (int i = 0; i < this.cards.size(); ++i) {
            Card c = this.getCardAt(i);
            if (c.SO == 12) {
                return null;
            }
            if (prev != null && c.SO == prev.SO) {
                if (doi == 0) {
                    gc.addCardNoSort(prev);
                    gc.addCardNoSort(c);
                    if (++count == size) {
                        if (remove) {
                            this.minusCard(gc);
                        }
                        return gc;
                    }
                }
                ++doi;
            } else if (prev != null && c.SO == prev.SO + 1 && doi > 0) {
                doi = 0;
            } else {
                doi = 0;
                count = 0;
                gc.removeAllCard();
            }
            prev = c;
        }
        return null;
    }

    private void removeAllCard() {
        this.cards.clear();
    }

    private boolean kiemTraNamDoiThong() {
        GroupCard gc = this.layRaDoiThong(5, false);
        return gc != null;
    }

    public Card getCardAt(int index) {
        return this.cards.get(index);
    }

    public Card getLastCard() {
        if (this.cards.size() != 0) {
            return this.cards.get(this.cards.size() - 1);
        }
        return null;
    }

    private Card getFirstCard() {
        if (this.cards.size() != 0) {
            return this.cards.get(0);
        }
        return null;
    }

    private boolean isSanh() {
        if (this.cards.size() < 3) {
            return false;
        }
        if (this.getLastCard().SO == 12) {
            return false;
        }
        return this.kiemtraSanhTrongKhoang(0, this.cards.size());
    }

    private int sosanhSanh(GroupCard groupCard) {
        if (this.cards.size() != groupCard.cards.size()) {
            return 0;
        }
        return this.getLastCard().compareTo(groupCard.getLastCard());
    }

    public boolean kiemtraSanhTrongKhoang(int from, int to) {
        for (int i = 1; i < to - from; ++i) {
            if (this.cards.get((int)from).SO + i == this.cards.get((int)(from + i)).SO) continue;
            return false;
        }
        return true;
    }

    public int kiemTraToiTrang() {
        if (this.cards.size() != 13) {
            return 0;
        }
        if (this.kiemTraSanhRong()) {
            return 1;
        }
        if (this.kiemTra6Doi()) {
            return 3;
        }
        if (this.kiemTraTuHeo()) {
            return 4;
        }
        if (this.kiemTraNamDoiThong()) {
            return 2;
        }
        return this.kiemTraDongMau();
    }

    public GroupCard() {
    }

    public void addCardNoSort(Card c) {
        this.cards.add(c);
    }

    private GroupCard getSubCard(int from, int to) {
        GroupCard gc = new GroupCard();
        for (int i = from; i < to; ++i) {
            Card c = this.getCardAt(i);
            gc.addCardNoSort(c);
        }
        return gc;
    }

    private boolean kiemTraSanhRong() {
        GroupCard gc = this.clone();
        for (int i = 0; i < gc.cards.size(); ++i) {
            Card c = gc.getCardAt(i);
            gc.cards.remove(c);
            if (gc.isSanh()) {
                return true;
            }
            gc.cards.add(i, c);
        }
        return false;
    }

    private boolean kiemTra6Doi() {
        int count = 0;
        int doi = 0;
        Card prev = null;
        for (int i = 0; i < this.cards.size(); ++i) {
            Card c = this.getCardAt(i);
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

    private boolean kiemTraTuHeo() {
        Card c = this.getCardAt(this.cards.size() - 4);
        return c.SO == 12;
    }

    private int kiemTraDongMau() {
        int count = 0;
        for (int i = 0; i < this.cards.size(); ++i) {
            Card c = this.getCardAt(i);
            if (!c.isRed()) continue;
            ++count;
        }
        if (count == 0 || count == 13) {
            return 5;
        }
        if (count == 1 || count == 12) {
            return 6;
        }
        return 0;
    }

    public boolean phatDuocChatChong() {
        if (this.BO == 4) {
            return true;
        }
        if (this.BO == 6) {
            return true;
        }
        return this.BO == 5;
    }

    public int tinhChatChong() {
        if (this.BO == 1 && this.cards.get((int)0).SO == 12) {
            return this.cards.get(0).demSoLaPhat();
        }
        if (this.BO == 2 && this.cards.get((int)0).SO == 12) {
            return this.cards.get(0).demSoLaPhat() + this.cards.get(1).demSoLaPhat();
        }
        if (this.BO == 4) {
            return 12;
        }
        if (this.BO == 6) {
            return 12;
        }
        if (this.BO == 5) {
            return 16;
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
        if (bo == 7) {
            int from = this.sanhLonNhat(size);
            if (from < 0) {
                return null;
            }
            for (int i = 0; i < size; ++i) {
                cards[i] = (byte)this.layIDTuSo((from + i) % 13);
            }
        }
        if (bo == 6 && (cards = this.tuquyLonNhat()) == null) {
            return null;
        }
        if (bo == 8 && (cards = this.haituquyLonNhat()) == null) {
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

    public static boolean kiemTraChatDuoc(GroupCard groupCard, GroupCard gc) {
        GroupCard sub = null;
        sub = gc.BO == 1 && gc.getFirstCard().SO != 12 ? groupCard.timBoLonNhat(1, 1) : (gc.BO == 1 && gc.getFirstCard().SO == 12 ? groupCard.timBoLonNhat(6, 4) : (gc.BO == 2 && gc.getFirstCard().SO == 12 ? groupCard.timBoLonNhat(8, 8) : groupCard.timBoLonNhat(gc.BO, gc.cards.size())));
        if (sub != null) {
            return sub.chatDuoc(gc);
        }
        return false;
    }

    public boolean coTheChatChong() {
        return this.BO == 1 && this.getFirstCard().SO == 12 || this.BO == 6 || this.BO == 2 && this.getFirstCard().SO == 12 || this.BO == 4 || this.BO == 5;
    }

    public boolean kiemTraBoBaiDanhRa(GroupCard sub) {
        if (this.cards.size() < sub.cards.size()) {
            Debug.trace((Object)"Bo danh ra co nhieu la hon");
            return false;
        }
        int j = 0;
        boolean result = false;
        for (int i = 0; i < this.cards.size(); ++i) {
            Card c2 = this.cards.get(i);
            if (j >= sub.cards.size()) continue;
            Card c1 = sub.cards.get(j);
            if (c1.ID != c2.ID || ++j != sub.cards.size()) continue;
            result = true;
        }
        return result;
    }

    public boolean isNoHu() {
        return this.kiemTraToiTrang() == 1 && this.getLastCard().SO == 12;
    }
}

