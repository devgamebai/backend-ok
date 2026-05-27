package game.modules.slot.entities.slot.thanden;

import java.util.ArrayList;
import java.util.List;

public class Awards {
    private static List<ThanDenAward> awards = new ArrayList<>();

    static {
        ThanDenAward[] values;
        int length = (values = ThanDenAward.values()).length;
        for (int i = 0; i < length; ++i) {
            ThanDenAward entry = values[i];
            awards.add(entry);
        }
    }

    public static List<ThanDenAward> list() {
        return awards;
    }

    public static ThanDenAward getAward(ThanDenItem item, int numItems) {
        ThanDenAward[] values;
        int length = (values = ThanDenAward.values()).length;

        for (int i = 0; i < length; ++i) {
            ThanDenAward entry = values[i];
            if (entry.getItem() == item && entry.getDuplicate() == numItems) {
                return entry;
            }
        }
        return null;
    }
}
