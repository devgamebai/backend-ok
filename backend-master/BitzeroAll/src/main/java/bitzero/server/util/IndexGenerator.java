package bitzero.server.util;

import java.util.Arrays;

public class IndexGenerator {
     private volatile Boolean[] playerSlots;

     public void init(int Size) {
          this.playerSlots = new Boolean[Size + 1];
          Arrays.fill(this.playerSlots, Boolean.FALSE);
     }

     public int getPlayerSlot() {
          int playerId = 0;
          synchronized(this.playerSlots) {
               for(int ii = 1; ii < this.playerSlots.length; ++ii) {
                    if (!this.playerSlots[ii]) {
                         playerId = ii;
                         this.playerSlots[ii] = Boolean.TRUE;
                         break;
                    }
               }

               return playerId;
          }
     }

     public void freePlayerSlot(int playerId) {
          if (playerId != -1) {
               if (playerId < this.playerSlots.length) {
                    synchronized(this.playerSlots) {
                         this.playerSlots[playerId] = Boolean.FALSE;
                    }
               }
          }
     }
}
