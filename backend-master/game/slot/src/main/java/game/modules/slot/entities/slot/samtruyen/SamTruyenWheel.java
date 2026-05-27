/*
 * Decompiled with CFR 0.144.
 */
package game.modules.slot.entities.slot.samtruyen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SamTruyenWheel {
    private List<SamTruyenItem> items = new ArrayList<SamTruyenItem>();

    public void addItem(SamTruyenItem item) {
        this.items.add(item);
    }

    public SamTruyenItem random() {
        Random rd = new Random();
        int n = rd.nextInt(this.items.size());
        return this.items.remove(n);
    }

    public void remove(int n) {
        if (n >= 0 && n < this.items.size()) {
            this.items.remove(n);
        }
    }
}

