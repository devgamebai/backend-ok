package game.modules.slot.entities.slot.avengers;

public class AvengersFreeSpinItems {
     private static int[][] config = new int[][]{{0, 0, 0, 1, 3, 5, 6, 11, 12, 13, 15}, {0, 0, 1, 1, 3, 5, 8, 12, 15, 16, 18}, {0, 0, 1, 1, 3, 4, 5, 11, 12, 13, 14}, {0, 0, 1, 1, 3, 5, 6, 11, 13, 14, 15}, {0, 0, 1, 1, 3, 5, 6, 11, 13, 14, 14}};
     private AvengersWheel[] wheels = new AvengersWheel[5];

     public AvengersFreeSpinItems() {
          for(int i = 0; i < 5; ++i) {
               this.wheels[i] = new AvengersWheel();

               for(int j = 0; j < 11; ++j) {
                    for(int k = 0; k < config[i][j]; ++k) {
                         this.wheels[i].addItem(AvengersItem.findItem((byte)j));
                    }
               }
          }

     }

     public AvengersItem random(int wheel) {
          return this.wheels[wheel].random();
     }

     public void refundItem(AvengersItem item, int wheel) {
          this.wheels[wheel].addItem(item);
     }
}
