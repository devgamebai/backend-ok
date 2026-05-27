package game.modules.slot.entities.slot.ndv;

import java.util.ArrayList;
import java.util.List;

public class NDVAwards {
     private static List awards = new ArrayList();

     public NDVAwards() {
          NDVAward[] values;
          int length = (values = NDVAward.values()).length;

          for(int i = 0; i < length; ++i) {
               NDVAward entry = values[i];
               awards.add(entry);
          }

     }

     public static List list() {
          return awards;
     }

     public static NDVAward getAward(NDVItem item, int numItems) {
          NDVAward[] values;
          int length = (values = NDVAward.values()).length;

          for(int i = 0; i < length; ++i) {
               NDVAward entry = values[i];
               if (entry.getItem() == item && entry.getDuplicate() == numItems) {
                    return entry;
               }
          }

          return null;
     }
}
