package game.modules.slot.entities.slot.khobau;

import java.util.ArrayList;
import java.util.List;

public class Awards {
     private static List awards = new ArrayList();

     public Awards() {
          KhoBauAward[] values;
          int length = (values = KhoBauAward.values()).length;

          for(int i = 0; i < length; ++i) {
               KhoBauAward entry = values[i];
               awards.add(entry);
          }

     }

     public static List list() {
          return awards;
     }

     public static KhoBauAward getAward(KhoBauItem item, int numItems) {
          KhoBauAward[] values;
          int length = (values = KhoBauAward.values()).length;

          for(int i = 0; i < length; ++i) {
               KhoBauAward entry = values[i];
               if (entry.getItem() == item && entry.getDuplicate() == numItems) {
                    return entry;
               }
          }

          return null;
     }
}
