/*
 * Decompiled with CFR 0.144.
 */
package game.modules.slot.entities.slot.spartan;

import java.util.ArrayList;
import java.util.List;

public class SpartanFreeSpinAwards {
    private static List<SpartanFreeSpinAward> awards = new ArrayList<SpartanFreeSpinAward>();

    public SpartanFreeSpinAwards() {
        for (SpartanFreeSpinAward entry : SpartanFreeSpinAward.values()) {
            awards.add(entry);
        }
    }

    public static List<SpartanFreeSpinAward> list() {
        return awards;
    }

    public static SpartanFreeSpinAward getAward(SpartanItem item, int numItems) {
        for (SpartanFreeSpinAward entry : SpartanFreeSpinAward.values()) {
            if (entry.getItem() != item || entry.getDuplicate() != numItems) continue;
            return entry;
        }
        return null;
    }
}

