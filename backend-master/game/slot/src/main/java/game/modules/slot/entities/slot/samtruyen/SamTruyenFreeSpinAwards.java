/*
 * Decompiled with CFR 0.144.
 */
package game.modules.slot.entities.slot.samtruyen;

import java.util.ArrayList;
import java.util.List;

public class SamTruyenFreeSpinAwards {
    private static List<SamTruyenFreeSpinAward> awards = new ArrayList<SamTruyenFreeSpinAward>();

    public SamTruyenFreeSpinAwards() {
        for (SamTruyenFreeSpinAward entry : SamTruyenFreeSpinAward.values()) {
            awards.add(entry);
        }
    }

    public static List<SamTruyenFreeSpinAward> list() {
        return awards;
    }

    public static SamTruyenFreeSpinAward getAward(SamTruyenItem item, int numItems) {
        for (SamTruyenFreeSpinAward entry : SamTruyenFreeSpinAward.values()) {
            if (entry.getItem() != item || entry.getDuplicate() != numItems) continue;
            return entry;
        }
        return null;
    }
}

