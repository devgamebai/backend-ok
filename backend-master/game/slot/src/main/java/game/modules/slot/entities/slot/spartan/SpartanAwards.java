/*
 * Decompiled with CFR 0.144.
 */
package game.modules.slot.entities.slot.spartan;

import java.util.ArrayList;
import java.util.List;

public class SpartanAwards {
    private static List<SpartanAward> awards = new ArrayList<SpartanAward>();

    public SpartanAwards() {
        for (SpartanAward entry : SpartanAward.values()) {
            awards.add(entry);
        }
    }

    public static List<SpartanAward> list() {
        return awards;
    }

    public static SpartanAward getAward(SpartanItem item, int numItems) {
        for (SpartanAward entry : SpartanAward.values()) {
            if (entry.getItem() != item || entry.getDuplicate() != numItems) continue;
            return entry;
        }
        return null;
    }
}

