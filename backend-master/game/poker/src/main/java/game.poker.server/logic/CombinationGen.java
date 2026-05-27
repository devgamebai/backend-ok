/*
 * Decompiled with CFR 0.144.
 */
package game.poker.server.logic;

import java.util.LinkedList;
import java.util.List;

public class CombinationGen {
    public int size = 0;
    public List<Integer> fullList = null;
    private List<Integer[]> combinations = new LinkedList<Integer[]>();

    public CombinationGen(List<Integer> full, int size) {
        this.fullList = full;
        this.size = size;
        this.combinations2(size, 0, new Integer[size]);
    }

    public void combinations2(int len, int startPosition, Integer[] result) {
        if (len == 0) {
            this.combinations.add((Integer[])result.clone());
            return;
        }
        for (int i = startPosition; i <= this.fullList.size() - len; ++i) {
            result[result.length - len] = this.fullList.get(i);
            this.combinations2(len - 1, i + 1, result);
        }
    }

    public List<Integer[]> getCombinations() {
        return this.combinations;
    }
}

