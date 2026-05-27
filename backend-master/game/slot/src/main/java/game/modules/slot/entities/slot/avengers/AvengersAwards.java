package game.modules.slot.entities.slot.avengers;

import java.util.ArrayList;
import java.util.List;

public class AvengersAwards {
     private static List awards = new ArrayList();

     public AvengersAwards() {
          AvengersAward[] values;
          int length = (values = AvengersAward.values()).length;

          for(int i = 0; i < length; ++i) {
               AvengersAward entry = values[i];
               awards.add(entry);
          }

     }

     public static List list() {
          return awards;
     }

     public static AvengersAward getAward(AvengersItem item, int numItems) {
          AvengersAward[] values;
          int length = (values = AvengersAward.values()).length;

          for(int i = 0; i < length; ++i) {
               AvengersAward entry = values[i];
               if (entry.getItem() == item && entry.getDuplicate() == numItems) {
                    return entry;
               }
          }

          return null;
     }
}
