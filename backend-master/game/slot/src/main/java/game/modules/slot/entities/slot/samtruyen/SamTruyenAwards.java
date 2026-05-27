/*
 * Decompiled with CFR 0.144.
 */
package game.modules.slot.entities.slot.samtruyen;

import java.util.ArrayList;
import java.util.List;

public class SamTruyenAwards {
    private static List<SamTruyenAward> awards = new ArrayList<SamTruyenAward>();

    public SamTruyenAwards() {
        for (SamTruyenAward entry : SamTruyenAward.values()) {
            awards.add(entry);
        }
    }

    public static List<SamTruyenAward> list() {
        return awards;
    }

    public static SamTruyenAward getAward(SamTruyenItem item, int numItems) {
        for (SamTruyenAward entry : SamTruyenAward.values()) {
            if (entry.getItem() != item || entry.getDuplicate() != numItems) continue;
            return entry;
        }
        return null;
    }
}

