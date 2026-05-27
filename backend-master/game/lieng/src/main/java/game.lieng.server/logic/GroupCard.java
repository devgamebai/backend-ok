/*
 * Decompiled with CFR 0.144.
 */
package game.lieng.server.logic;

import game.lieng.server.logic.Card;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GroupCard {
    private static final String[] group_names = new String[]{"D", "C", "A", "L", "S", "ER"};
    public static final int EG_DIEM = 0;
    public static final int EG_CHIN = 1;
    public static final int EG_ANH = 2;
    public static final int EG_LIENG = 3;
    public static final int EG_SAP = 4;
    public static final int EG_WRONG = 5;
    public int BO = -1;
    public List<Card> cards;
    private static final Comparator<Card> cardComparator = new Comparator<Card>(){

        @Override
        public int compare(Card c1, Card c2) {
            if (c1.SO > c2.SO) {
                return 1;
            }
            if (c1.SO < c2.SO) {
                return -1;
            }
            if (c1.CHAT > c2.CHAT) {
                return 1;
            }
            if (c2.CHAT < c1.CHAT) {
                return -1;
            }
            return 0;
        }
    };

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
                    so = phanSo.equalsIgnoreCase("A") ? 1 : (phanSo.equalsIgnoreCase("J") ? 11 : (phanSo.equalsIgnoreCase("Q") ? 12 : (phanSo.equalsIgnoreCase("K") ? 13 : Integer.parseInt(phanSo))));
                    int phanChat = 0;
                    if (c[k] == 'r') {
                        phanChat = 3;
                    }
                    if (c[k] == 'c') {
                        phanChat = 2;
                    }
                    if (c[k] == 't') {
                        phanChat = 1;
                    }
                    if (c[k] == 'b') {
                        phanChat = 0;
                    }
                    Card card = new Card(so, phanChat);
                    this.cards.add(card);
                    k = 0;
                    continue;
                }
                ++k;
            }
            Collections.sort(this.cards, cardComparator);
            this.kiemTraBo();
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
                    so = phanSo.equalsIgnoreCase("A") ? 1 : (phanSo.equalsIgnoreCase("J") ? 11 : (phanSo.equalsIgnoreCase("Q") ? 11 : (phanSo.equalsIgnoreCase("K") ? 11 : Integer.parseInt(phanSo))));
                    int phanChat = 0;
                    if (c[k] == 'r') {
                        phanChat = 3;
                    }
                    if (c[k] == 'c') {
                        phanChat = 2;
                    }
                    if (c[k] == 't') {
                        phanChat = 1;
                    }
                    if (c[k] == 'b') {
                        phanChat = 0;
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

    public GroupCard() {
        this.cards = new ArrayList<Card>();
    }

    public void addCard(Card c) {
        this.cards.add(c);
        Collections.sort(this.cards, cardComparator);
        this.kiemTraBo();
    }

    public GroupCard(int[] ids) {
        this.cards = new ArrayList<Card>(ids.length);
        for (int id : ids) {
            Card card = new Card(id);
            this.cards.add(card);
        }
        Collections.sort(this.cards, cardComparator);
    }

    public GroupCard(byte[] ids) {
        this.cards = new ArrayList<Card>(ids.length);
        for (byte id : ids) {
            Card card = new Card(id);
            this.cards.add(card);
        }
        Collections.sort(this.cards, cardComparator);
    }

    public int kiemTraBo() {
        if (this.BO == -1 || this.BO == 5) {
            return this.tinhBo();
        }
        return this.BO;
    }

    public int tinhChoDiemBoChoClient() {
        if (this.BO == -1 || this.BO == 5) {
            this.tinhBo();
        }
        if (this.BO == 0) {
            return this.tinhDiem();
        }
        if (this.BO == 4) {
            Card c = this.cards.get(0);
            return 20 + c.SO;
        }
        return 10 + this.BO;
    }

    public int tinhBo() {
        if (this.cards.size() != 3) {
            this.BO = 5;
            return this.BO;
        }
        this.BO = this.sap() ? 4 : (this.lieng() ? 3 : (this.anh() ? 2 : (this.chin() ? 1 : 0)));
        return this.BO;
    }

    public boolean lieng() {
        if (this.cards.size() != 3) {
            return false;
        }
        Card c1 = this.cards.get(0);
        Card c2 = this.cards.get(1);
        Card c3 = this.cards.get(2);
        if (c1.SO != 1) {
            return c1.SO + 1 == c2.SO && c2.SO + 1 == c3.SO;
        }
        return c1.SO + 1 == c2.SO && c2.SO + 1 == c3.SO || c2.SO == 12 && c3.SO == 13;
    }

    public boolean sap() {
        if (this.cards.size() != 3) {
            return false;
        }
        Card c1 = this.cards.get(0);
        Card c2 = this.cards.get(1);
        if (c1.SO != c2.SO) {
            return false;
        }
        Card c3 = this.cards.get(2);
        return c2.SO == c3.SO;
    }

    public boolean chin() {
        return this.tinhDiem() == 9;
    }

    public boolean anh() {
        if (this.cards.size() != 3) {
            return false;
        }
        for (int i = 0; i < this.cards.size(); ++i) {
            Card c = this.cards.get(i);
            if (c.isAnh()) continue;
            return false;
        }
        return true;
    }

    public int soSanhBo(GroupCard gc) {
        if (gc.kiemTraBo() > this.kiemTraBo()) {
            return -1;
        }
        if (gc.kiemTraBo() < this.kiemTraBo()) {
            return 1;
        }
        if (this.kiemTraBo() == 0 || this.kiemTraBo() == 1) {
            return this.soSanhThuong(gc);
        }
        if (this.kiemTraBo() == 3) {
            return this.soSanhLieng(gc);
        }
        if (this.kiemTraBo() == 2) {
            return this.soSanhAnh(gc);
        }
        return this.soSanhSap(gc);
    }

    public int tinhDiem() {
        Card c1 = this.cards.get(0);
        Card c2 = this.cards.get(1);
        Card c3 = this.cards.get(2);
        return (c1.laySoTinhDiem() + c2.laySoTinhDiem() + c3.laySoTinhDiem()) % 10;
    }

    private int soSanhThuong(GroupCard gc) {
        if (this.tinhDiem() > gc.tinhDiem()) {
            return 1;
        }
        if (this.tinhDiem() < gc.tinhDiem()) {
            return -1;
        }
        return this.soSanhUuTienChat(gc);
    }

    private int soSanhUuTienChat(GroupCard gc) {
        Card c1 = this.timCardLonNhatUuTienChat();
        Card c2 = gc.timCardLonNhatUuTienChat();
        return c1.lonHonUuTienChat(c2);
    }

    private int soSanhUuTienSo(GroupCard gc) {
        Card c1 = this.timCardLonNhatUuTienSo();
        Card c2 = gc.timCardLonNhatUuTienSo();
        return c1.lonHonUuTienSo(c2);
    }

    private Card timCardLonNhatUuTienSo() {
        Card max = this.cards.get(0);
        for (int i = 1; i < this.cards.size(); ++i) {
            Card c = this.cards.get(i);
            if (c.lonHonUuTienSo(max) <= 0) continue;
            max = c;
        }
        return max;
    }

    private Card timCardLonNhatUuTienChat() {
        Card max = this.cards.get(0);
        for (int i = 1; i < this.cards.size(); ++i) {
            Card c = this.cards.get(i);
            if (c.lonHonUuTienChat(max) <= 0) continue;
            max = c;
        }
        return max;
    }

    private int soSanhAnh(GroupCard gc) {
        return this.soSanhUuTienChat(gc);
    }

    private int soSanhLieng(GroupCard gc) {
        int size = this.cards.size();
        int sum1 = 0;
        int sum2 = 0;
        for (int i = 0; i < size; ++i) {
            sum1 += this.cards.get((int)i).SO;
            sum2 += gc.cards.get((int)i).SO;
        }
        if (sum1 % 3 == 0 && sum2 % 3 == 0) {
            if (sum1 > sum2) {
                return 1;
            }
            if (sum1 < sum2) {
                return -1;
            }
            Card c1 = this.timCardLonNhatUuTienChat();
            Card c2 = gc.timCardLonNhatUuTienChat();
            return c1.lonHonUuTienChat(c2);
        }
        if (sum1 % 3 != 0 && sum2 % 3 != 0) {
            Card c1 = this.timCardLonNhatUuTienChat();
            Card c2 = gc.timCardLonNhatUuTienChat();
            return c1.lonHonUuTienChat(c2);
        }
        if (sum1 % 3 != 0) {
            return 1;
        }
        return -1;
    }

    private int soSanhSap(GroupCard gc) {
        return this.soSanhUuTienSo(gc);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("#");
        sb.append(group_names[this.kiemTraBo()]).append(":");
        for (int i = 0; i < this.cards.size(); ++i) {
            Card c = this.cards.get(i);
            sb.append(c.name);
        }
        sb.append("$");
        return sb.toString();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public byte[] toByteArray() {
        if (this.cards == null) {
            return new byte[0];
        }
        List<Card> list = this.cards;
        synchronized (list) {
            byte[] c = new byte[this.cards.size()];
            for (int i = 0; i < this.cards.size(); ++i) {
                c[i] = (byte)this.cards.get((int)i).ID;
            }
            return c;
        }
    }

    public boolean isNoHu() {
        boolean res = this.lieng();
        if (res) {
            Card c1 = this.cards.get(0);
            Card c2 = this.cards.get(1);
            Card c3 = this.cards.get(2);
            return c1.SO == 11 && c1.CHAT == 3 && c2.SO == 12 && c2.CHAT == 3 && c3.SO == 13 && c3.CHAT == 3;
        }
        return false;
    }

}

