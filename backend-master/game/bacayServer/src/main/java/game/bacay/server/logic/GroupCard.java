/*
 * Decompiled with CFR 0.144.
 */
package game.bacay.server.logic;

import game.bacay.server.logic.Card;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GroupCard {
    private static final String[] group_names = new String[]{"T", "M", "D", "S", "ER"};
    public static final int EG_NONE = 0;
    public static final int EG_MUOI = 1;
    public static final int EG_DAY = 2;
    public static final int EG_SAP = 3;
    public static final int EG_WRONG = 4;
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
    public static final Comparator<GroupCard> groupCardComparator = new Comparator<GroupCard>(){

        @Override
        public int compare(GroupCard gc1, GroupCard gc2) {
            return gc2.soSanhBo(gc1);
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

    public GroupCard(byte[] ids) {
        this.cards = new ArrayList<Card>(ids.length);
        for (byte id : ids) {
            Card card = new Card(id);
            this.cards.add(card);
        }
        Collections.sort(this.cards, cardComparator);
    }

    public GroupCard(int[] ids) {
        this.cards = new ArrayList<Card>(ids.length);
        for (int id : ids) {
            Card card = new Card(id);
            this.cards.add(card);
        }
        Collections.sort(this.cards, cardComparator);
    }

    public int kiemTraBo() {
        if (this.BO == -1 || this.BO == 4) {
            return this.tinhBo();
        }
        return this.BO;
    }

    public int tinhBo() {
        if (this.cards.size() != 3) {
            this.BO = 4;
            return this.BO;
        }
        this.BO = this.sap() ? 3 : (this.day() ? 2 : (this.muoi() ? 1 : 0));
        return this.BO;
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

    public boolean muoi() {
        return this.tinhDiem() == 0;
    }

    public boolean day() {
        if (this.cards.size() != 3) {
            return false;
        }
        Card c1 = this.cards.get(0);
        Card c2 = this.cards.get(1);
        if (!c2.dongChatDuoi(c1)) {
            return false;
        }
        Card c3 = this.cards.get(2);
        return c2.dongChatTren(c3);
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
        if (this.kiemTraBo() == 2) {
            return this.soSanhDay(gc);
        }
        return this.soSanhSap(gc);
    }

    public int tinhDiem() {
        Card c1 = this.cards.get(0);
        Card c2 = this.cards.get(1);
        Card c3 = this.cards.get(2);
        return (c1.SO + c2.SO + c3.SO) % 10;
    }

    private int soSanhThuong(GroupCard gc) {
        if (this.tinhDiem() > gc.tinhDiem()) {
            return 1;
        }
        if (this.tinhDiem() < gc.tinhDiem()) {
            return -1;
        }
        return this.soSanhTinhDiemVaSap(gc);
    }

    private int soSanhTinhDiemVaSap(GroupCard gc) {
        Card c1 = this.timCardLonNhatTinhDiemVaSap();
        Card c2 = gc.timCardLonNhatTinhDiemVaSap();
        return c1.lonHonTinhDiemVaSap(c2);
    }

    private int soSanhTinhDay(GroupCard gc) {
        Card c1 = this.timCardLonNhatTinhDay();
        Card c2 = gc.timCardLonNhatTinhDay();
        return c1.lonHonTinhDay(c2);
    }

    private Card timCardLonNhatTinhDay() {
        Card max = this.cards.get(0);
        for (int i = 1; i < this.cards.size(); ++i) {
            Card c = this.cards.get(i);
            if (c.lonHonTinhDay(max) <= 0) continue;
            max = c;
        }
        return max;
    }

    private Card timCardLonNhatTinhDiemVaSap() {
        Card max = this.cards.get(0);
        for (int i = 1; i < this.cards.size(); ++i) {
            Card c = this.cards.get(i);
            if (c.lonHonTinhDiemVaSap(max) <= 0) continue;
            max = c;
        }
        return max;
    }

    private int soSanhDay(GroupCard gc) {
        return this.soSanhTinhDay(gc);
    }

    private int soSanhSap(GroupCard gc) {
        Card c1 = this.timCardLonNhatTinhDiemVaSap();
        Card c2 = gc.timCardLonNhatTinhDiemVaSap();
        if (c1.isAtRo()) {
            return 1;
        }
        if (c2.isAtRo()) {
            return -1;
        }
        if (c1.SO > c2.SO) {
            return 1;
        }
        if (c1.SO < c2.SO) {
            return -1;
        }
        return 0;
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
        boolean res = this.day();
        if (res) {
            Card last = this.cards.get(2);
            return last.SO == 9 && last.CHAT == 3;
        }
        return false;
    }

}

