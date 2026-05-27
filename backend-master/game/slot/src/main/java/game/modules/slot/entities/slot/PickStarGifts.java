package game.modules.slot.entities.slot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PickStarGifts {
     private static final int NUM_KEYS = 3;
     private static final int NUM_BOXS = 5;
     private static final int NUM_GOLD = 19;
     private static final int NUM_LOSE = 3;
     private List gifts = new ArrayList();

     public PickStarGifts() {
          int i;
          for(i = 0; i < NUM_KEYS; ++i) {
               this.gifts.add(PickStarGiftItem.KEY);
          }

          for(i = 0; i < NUM_BOXS; ++i) {
               this.gifts.add(PickStarGiftItem.BOX);
          }

          for(i = 0; i < NUM_GOLD; ++i) {
               this.gifts.add(PickStarGiftItem.GOLD);
          }

          for(i = 0; i < NUM_LOSE; ++i) {
               this.gifts.add(PickStarGiftItem.LOSE);
          }

     }

     public PickStarGiftItem pickRandomAndRandomGift() {
          Random rd = new Random(System.currentTimeMillis());
          int n = rd.nextInt(this.gifts.size());
          PickStarGiftItem gift = (PickStarGiftItem)this.gifts.get(n);
          this.gifts.remove(n);
          return gift;
     }
}
