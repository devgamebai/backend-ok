package game.modules.slot.entities.slot.avengers;

import java.util.ArrayList;
import java.util.List;

public class AvengersFreeSpinAwards {
     private static List awards = new ArrayList();

     public AvengersFreeSpinAwards() {
          AvengersFreeSpinAward[] values;
          int length = (values = AvengersFreeSpinAward.values()).length;

          for(int i = 0; i < length; ++i) {
               AvengersFreeSpinAward entry = values[i];
               awards.add(entry);
          }

     }

     public static List list() {
          return awards;
     }

     public static AvengersFreeSpinAward getAward(AvengersItem item, int numItems) {
          AvengersFreeSpinAward[] values;
          int length = (values = AvengersFreeSpinAward.values()).length;

          for(int i = 0; i < length; ++i) {
               AvengersFreeSpinAward entry = values[i];
               if (entry.getItem() == item && entry.getDuplicate() == numItems) {
                    return entry;
               }
          }

          return null;
     }
}
