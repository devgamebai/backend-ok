package game.modules.slot.entities.slot.ndv;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NDVItems {
     private static int[] config = new int[]{28, 10, 32, 50, 60, 60, 69, 75, 80};
     public List items = new ArrayList();

     public NDVItems() {
          for(int i = 0; i < config.length; ++i) {
               for(int j = 0; j < config[i]; ++j) {
                    this.items.add(NDVItem.findItem((byte)i));
               }
          }

     }

     public int size() {
          return this.items.size();
     }

     public NDVItem random() {
          Random rd = new Random();
          int index = rd.nextInt(this.items.size());
          return (NDVItem)this.items.get(index);
     }
}
