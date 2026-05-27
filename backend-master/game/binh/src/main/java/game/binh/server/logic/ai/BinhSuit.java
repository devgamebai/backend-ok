/*
 * Decompiled with CFR 0.144.
 */
package game.binh.server.logic.ai;

import game.binh.server.logic.ai.BinhGroup;
import java.util.LinkedList;
import java.util.List;

public class BinhSuit {
    private List<BinhGroup> listGroup = new LinkedList<BinhGroup>();

    public BinhSuit(String[] lines) {
        for (int i = 0; i < lines.length; ++i) {
            String line = lines[i];
            BinhGroup group = new BinhGroup(line);
            this.listGroup.add(group);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.listGroup.size(); ++i) {
            BinhGroup g = this.listGroup.get(i);
            sb.append(g).append("\n");
        }
        return sb.toString();
    }

    public boolean canJackpot() {
        for (BinhGroup g : this.listGroup) {
            if (g.getScore() != 1005) continue;
            return true;
        }
        return false;
    }

    public int getMaxScore() {
        int max = 0;
        for (BinhGroup g : this.listGroup) {
            if (g.getScore() <= max) continue;
            max = g.getScore();
        }
        return max;
    }

    public int getMinScore() {
        int min = Integer.MAX_VALUE;
        for (BinhGroup g : this.listGroup) {
            if (g.getScore() >= min) continue;
            min = g.getScore();
        }
        return min;
    }

    public int getAverageScore() {
        int sum = 0;
        for (BinhGroup g : this.listGroup) {
            sum += g.getScore();
        }
        return sum / 4;
    }

    public List<BinhGroup> getListGroup() {
        return this.listGroup;
    }
}

