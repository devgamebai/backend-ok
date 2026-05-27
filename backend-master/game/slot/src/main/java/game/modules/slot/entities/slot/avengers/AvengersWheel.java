package game.modules.slot.entities.slot.avengers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AvengersWheel {
     private List items = new ArrayList();

     public void addItem(AvengersItem item) {
          this.items.add(item);
     }

     public AvengersItem random() {
          Random rd = new Random();
          int n = rd.nextInt(this.items.size());
          return (AvengersItem)this.items.remove(n);
     }

     public void remove(int n) {
          if (n >= 0 && n < this.items.size()) {
               this.items.remove(n);
          }

     }
}
