package game.modules.slot.entities.slot.vqv;

import java.util.ArrayList;
import java.util.List;

public class Awards {
     private static List awards = new ArrayList();

     public Awards() {
          VQVAward[] values;
          int length = (values = VQVAward.values()).length;

          for(int i = 0; i < length; ++i) {
               VQVAward entry = values[i];
               awards.add(entry);
          }

     }

     public static List list() {
          return awards;
     }

     public static VQVAward getAward(VQVItem item, int numItems) {
          VQVAward[] values;
          int length = (values = VQVAward.values()).length;

          for(int i = 0; i < length; ++i) {
               VQVAward entry = values[i];
               if (entry.getItem() == item && entry.getDuplicate() == numItems) {
                    return entry;
               }
          }

          return null;
     }
}
