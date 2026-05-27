package game.modules.slot.entities.slot.vqv;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VQVItems {
     private int[] config = new int[]{27, 10, 10, 38, 45, 68, 70};
     public List items = new ArrayList();

     public VQVItems() {
          for(int i = 0; i < this.config.length; ++i) {
               for(int j = 0; j < this.config[i]; ++j) {
                    this.items.add(VQVItem.findItem((byte)i));
               }
          }

     }

     public int size() {
          return this.items.size();
     }

     public VQVItem random() {
          Random rd = new Random();
          int index = rd.nextInt(this.items.size());
          return (VQVItem)this.items.get(index);
     }
}
