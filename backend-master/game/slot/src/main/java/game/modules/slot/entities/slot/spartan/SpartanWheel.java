/*
 * Decompiled with CFR 0.144.
 */
package game.modules.slot.entities.slot.spartan;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SpartanWheel {
    private List<SpartanItem> items = new ArrayList<SpartanItem>();

    public void addItem(SpartanItem item) {
        this.items.add(item);
    }

    public SpartanItem random() {
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

