package game.modules.slot.entities.slot;

public class PickStarGift {
     private PickStarGiftItem item;
     private int value;
     private String name;

     public PickStarGift(PickStarGiftItem item, int value) {
          this.item = item;
          this.value = value;
          switch(item) {
          case KEY:
               this.name = "0";
               break;
          case GOLD:
               this.name = "1";
               break;
          case BOX:
               if (value == 10) {
                    this.name = "2";
               } else if (value == 15) {
                    this.name = "3";
               } else {
                    this.name = "4";
               }
               break;
          case LOSE:
               this.name = "5";
               break;
          }

     }

     public PickStarGiftItem getItem() {
          return this.item;
     }

     public void setItem(PickStarGiftItem item) {
          this.item = item;
     }

     public int getValue() {
          return this.value;
     }

     public void setValue(int value) {
          this.value = value;
     }

     public String getName() {
          return this.name;
     }

     public void setName(String name) {
          this.name = name;
     }
}
