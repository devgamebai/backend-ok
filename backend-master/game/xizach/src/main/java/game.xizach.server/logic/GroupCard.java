/*
 * Decompiled with CFR 0.144.
 */
package game.xizach.server.logic;

import game.xizach.server.logic.Card;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class GroupCard {
    private static final String[] group_names = new String[]{"WR", "DN", "Q", "T", "21", "NL", "XR", "XB"};
    public static final int KG_WRONG = 0;
    public static final int KG_XIBANG = 7;
    public static final int KG_XIZACH = 6;
    public static final int KG_NGULINH = 5;
    public static final int KG_21DIEM = 4;
    public static final int KG_THUONG = 3;
    public static final int KG_QUAC = 2;
    public static final int KG_DANNON = 1;
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
            return gc1.soSanhBo(gc2);
        }
    };
    public static final Comparator<GroupCard> bestGroupCardComparator = new Comparator<GroupCard>(){

        @Override
        public int compare(GroupCard gc1, GroupCard gc2) {
            GroupCard bgc1 = gc1.findBestGroupCard();
            GroupCard bgc2 = gc2.findBestGroupCard();
            return bgc1.soSanhBo(bgc2);
        }
    };
    private GroupCard bestGroupCard = null;

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
                    so = phanSo.equalsIgnoreCase("A") ? 0 : (phanSo.equalsIgnoreCase("J") ? 10 : (phanSo.equalsIgnoreCase("Q") ? 11 : (phanSo.equalsIgnoreCase("K") ? 12 : Integer.parseInt(phanSo))));
                    int phanChat = 0;
                    if (c[k] == 'r') {
                        phanChat = 2;
                    }
                    if (c[k] == 'c') {
                        phanChat = 3;
                    }
                    if (c[k] == 't') {
                        phanChat = 1;
                    }
                    if (c[k] == 'b') {
                        phanChat = 0;
                    }
                    Card card = Card.createCard(so, phanChat);
                    this.cards.add(card);
                    k = 0;
                    continue;
                }
                ++k;
            }
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
                    so = phanSo.equalsIgnoreCase("A") ? 0 : (phanSo.equalsIgnoreCase("J") ? 10 : (phanSo.equalsIgnoreCase("Q") ? 10 : (phanSo.equalsIgnoreCase("K") ? 10 : Integer.parseInt(phanSo))));
                    int phanChat = 0;
                    if (c[k] == 'r') {
                        phanChat = 2;
                    }
                    if (c[k] == 'c') {
                        phanChat = 3;
                    }
                    if (c[k] == 't') {
                        phanChat = 1;
                    }
                    if (c[k] == 'b') {
                        phanChat = 0;
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

    public GroupCard() {
        this.cards = new ArrayList<Card>();
    }

    public void addCard(Card c) {
        this.cards.add(c);
        this.kiemTraBo();
    }

    public GroupCard(byte[] ids) {
        this.cards = new ArrayList<Card>(ids.length);
        for (byte id : ids) {
            Card card = Card.createCard(id);
            this.cards.add(card);
        }
        this.kiemTraBo();
    }

    public GroupCard(int[] ids) {
        this.cards = new ArrayList<Card>(ids.length);
        for (int id : ids) {
            Card card = Card.createCard(id);
            this.cards.add(card);
        }
        this.kiemTraBo();
    }

    public static void main(String[] arg) {
        Card c = Card.createCard(4, 0);
        int[] cardList1 = new int[]{0, c.ID};
        GroupCard gc1 = new GroupCard(cardList1);
        System.out.println(gc1);
        System.out.println(gc1.getMaxDiem());
    }

    public int kiemTraBo() {
        this.BO = this.tinhBo();
        return this.BO;
    }

    public int tinhBo() {
        if (this.isXiBang()) {
            return 7;
        }
        if (this.isXiZach()) {
            return 6;
        }
        if (this.isNguLinh()) {
            return 5;
        }
        if (this.getMaxDiem() == 21) {
            return 4;
        }
        if (this.isQuac()) {
            this.BO = 2;
            return 2;
        }
        if (this.cards.size() < 2) {
            return 0;
        }
        if (this.getMaxDiem() < 16) {
            return 1;
        }
        return 3;
    }

    public boolean isXiBang() {
        return this.cards.size() == 2 && this.cards.get(0).isXi() != false && this.cards.get(1).isXi() != false;
    }

    public boolean isXiZach() {
        if (this.cards.size() == 2 && this.cards.get(0).isXi().booleanValue() && this.cards.get(1).getDiem() == 10) {
            return true;
        }
        return this.cards.size() == 2 && this.cards.get(1).isXi() != false && this.cards.get(0).getDiem() == 10;
    }

    public boolean hasXi() {
        for (int i = 0; i < this.cards.size(); ++i) {
            if (!this.cards.get(i).isXi().booleanValue()) continue;
            return true;
        }
        return false;
    }

    public boolean isNguLinh() {
        return this.cards.size() >= 5 && this.getMinDiem() <= 21;
    }

    public boolean isQuac() {
        return this.getMaxDiem() > 21;
    }

    public boolean isDanNon() {
        return this.getMaxDiem() < 16;
    }

    public boolean canDanBai() {
        int value = this.getMaxDiem();
        return this.isNguLinh() || this.getMaxDiem() >= 16;
    }

    public boolean batBuocDanBai() {
        int value = this.getMaxDiem();
        return this.isNguLinh() || this.getMaxDiem() > 21;
    }

    public boolean canRutBai() {
        if (this.isXiZach() || this.isXiBang()) {
            return false;
        }
        return this.getMinDiem() < 21 && !this.isNguLinh();
    }

    public int getMinDiem() {
        int sum = 0;
        for (int i = 0; i < this.cards.size(); ++i) {
            sum += this.cards.get(i).getMinDiem();
        }
        return sum;
    }

    public int getMaxDiem() {
        if (this.isXiBang() || this.isXiZach()) {
            return 21;
        }
        int minDiem = this.getMinDiem();
        if (!this.hasXi()) {
            return minDiem;
        }
        if (!this.has2XiTroLen().booleanValue()) {
            return this.chonDiemMax(minDiem, minDiem + 10);
        }
        int a1 = minDiem;
        return a1;
    }

    public int chonDiemMax(int a, int b) {
        if (a < 16 && b > 21) {
            return a;
        }
        if (b < 16 && a > 21) {
            return b;
        }
        if (a < 16 || a > 21) {
            return b;
        }
        if (b < 16 || b > 21) {
            return a;
        }
        if (a <= b) {
            return b;
        }
        return a;
    }

    public Boolean has2XiTroLen() {
        int countXi = 0;
        for (int i = 0; i < this.cards.size(); ++i) {
            if (!this.cards.get(i).isXi().booleanValue()) continue;
            ++countXi;
        }
        return countXi >= 2;
    }

    public int soSanhBo(GroupCard gc) {
        if (gc.kiemTraBo() > this.kiemTraBo()) {
            return -1;
        }
        if (gc.kiemTraBo() < this.kiemTraBo()) {
            return 1;
        }
        if (gc.isNguLinh()) {
            if (this.getMinDiem() < gc.getMinDiem()) {
                return 1;
            }
            if (this.getMinDiem() > gc.getMinDiem()) {
                return -1;
            }
            return 0;
        }
        if (this.BO == 3) {
            if (this.getMaxDiem() > gc.getMaxDiem()) {
                return 1;
            }
            if (this.getMaxDiem() == gc.getMaxDiem()) {
                return 0;
            }
            return -1;
        }
        if (this.BO == 2) {
            return 0;
        }
        if (this.BO == 1) {
            return 0;
        }
        return 0;
    }

    public int getRate() {
        if (this.isNguLinh()) {
            return 5;
        }
        if (this.isXiBang()) {
            return 4;
        }
        if (this.isXiZach()) {
            return 3;
        }
        if (this.BO == 4) {
            return 2;
        }
        return 1;
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

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public byte[] toByteHiddenArray() {
        if (this.cards == null) {
            return new byte[0];
        }
        List<Card> list = this.cards;
        synchronized (list) {
            byte[] c = new byte[this.cards.size()];
            for (int i = 0; i < this.cards.size(); ++i) {
                c[i] = 52;
            }
            return c;
        }
    }

    public GroupCard copy() {
        GroupCard gc = new GroupCard();
        for (int i = 0; i < this.cards.size(); ++i) {
            Card c = Card.createCard(this.cards.get(i));
            gc.cards.add(c);
        }
        return gc;
    }

    public GroupCard findBestGroupCard() {
        if (this.bestGroupCard != null) {
            return this.bestGroupCard;
        }
        GroupCard handCards = new GroupCard();
        for (int k = 0; k < 2; ++k) {
            handCards.addCard(this.cards.get(k));
        }
        LinkedList<GroupCard> groupCardList = new LinkedList<GroupCard>();
        int from = handCards.cards.size();
        int to = this.cards.size();
        GroupCard previousGroup = null;
        for (int i = from; i < to; ++i) {
            GroupCard newGroup;
            if (previousGroup == null) {
                previousGroup = handCards;
                groupCardList.add(handCards);
            }
            if (previousGroup.batBuocDanBai()) break;
            previousGroup = newGroup = previousGroup.copy();
            Card c = this.cards.get(i);
            newGroup.addCard(c);
            groupCardList.add(newGroup);
        }
        Collections.sort(groupCardList, groupCardComparator);
        return (GroupCard)groupCardList.get(groupCardList.size() - 1);
    }

}

