/*
 * Decompiled with CFR 0.144.
 */
package game.binh.server.logic.ai;

import game.binh.server.logic.Card;
import game.binh.server.logic.GroupCard;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class BinhGroup {
    public static final Comparator<BinhGroup> SORT_COMPARATOR = new Comparator<BinhGroup>(){

        @Override
        public int compare(BinhGroup c1, BinhGroup c2) {
            if (c1.getScore() < c2.getScore()) {
                return 1;
            }
            if (c1.getScore() > c2.getScore()) {
                return -1;
            }
            return 0;
        }
    };
    private GroupCard chi1;
    private GroupCard chi2;
    private GroupCard chi3;
    private int score;

    public static void main(String[] args) {
        String x = "#S:10rJrQrKbAb$|#D:8c8t9c7b2r$|#D:3t3b10t$|327";
        BinhGroup group = new BinhGroup(x);
        System.out.println(group);
        GroupCard gc1 = new GroupCard(group.getOrder());
        gc1.DecreaseSort();
        System.out.println(gc1);
        GroupCard gc = new GroupCard(group.getRandom());
        gc.DecreaseSort();
        System.out.println(gc);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.chi1).append("|");
        sb.append(this.chi2).append("|");
        sb.append(this.chi3).append("|");
        sb.append(this.score).append("|");
        return sb.toString();
    }

    public BinhGroup(String input) {
        String[] xx = input.split("\\|");
        this.chi1 = new GroupCard(xx[0]);
        this.chi2 = new GroupCard(xx[1]);
        this.chi3 = new GroupCard(xx[2]);
        this.score = Integer.parseInt(xx[3]);
    }

    public BinhGroup(GroupCard gc, int score) {
        int i;
        this.chi1 = new GroupCard();
        this.chi2 = new GroupCard();
        this.chi3 = new GroupCard();
        int index = 0;
        for (i = 0; i < 5; ++i) {
            this.chi1.AddCard(gc.Cards().get(index++));
        }
        for (i = 0; i < 5; ++i) {
            this.chi1.AddCard(gc.Cards().get(index++));
        }
        for (i = 0; i < 3; ++i) {
            this.chi1.AddCard(gc.Cards().get(index++));
        }
        this.score = score;
    }

    public int getScore() {
        return this.score;
    }

    public int[] getOrder() {
        int i;
        Card c;
        int index = 0;
        int[] order = new int[13];
        for (i = 0; i < this.chi1.GetNumOfCards(); ++i) {
            c = this.chi1.Cards().get(i);
            order[index++] = c.ID;
        }
        for (i = 0; i < this.chi2.GetNumOfCards(); ++i) {
            c = this.chi2.Cards().get(i);
            order[index++] = c.ID;
        }
        for (i = 0; i < this.chi3.GetNumOfCards(); ++i) {
            c = this.chi3.Cards().get(i);
            order[index++] = c.ID;
        }
        return order;
    }

    public int[] getRandom() {
        int[] order = this.getOrder();
        LinkedList<Integer> ids = new LinkedList<Integer>();
        for (int i = 0; i < order.length; ++i) {
            ids.add(order[i]);
        }
        Collections.shuffle(ids);
        int[] random = new int[13];
        for (int i = 0; i < random.length; ++i) {
            random[i] = (Integer)ids.get(i);
        }
        return random;
    }

    public boolean isJackpot() {
        return this.score == 1005;
    }

    public GroupCard getRandomGroupCard() {
        GroupCard gc = new GroupCard(this.getRandom());
        return gc;
    }

    public GroupCard getOrderGroupCard() {
        GroupCard gc = new GroupCard(this.getOrder());
        return gc;
    }

}

