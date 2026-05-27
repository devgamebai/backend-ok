package bitzero.server.util;

import bitzero.server.entities.Room;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultPlayerIdGenerator implements IPlayerIdGenerator {
     private Room parentRoom;
     private volatile Boolean[] playerSlots;
     private final Logger logger = LoggerFactory.getLogger(this.getClass());

     public void init() {
          this.playerSlots = new Boolean[this.parentRoom.getMaxUsers() + 1];
          Arrays.fill(this.playerSlots, Boolean.FALSE);
     }

     public int getPlayerSlot() {
          int playerId = 0;
          synchronized(this.playerSlots) {
               int ii = 1;

               while(ii < this.playerSlots.length) {
                    if (this.playerSlots[ii]) {
                         ++ii;
                    } else {
                         playerId = ii;
                         this.playerSlots[ii] = Boolean.TRUE;
                         break;
                    }
               }
          }

          if (playerId < 1) {
               this.logger.warn("No player slot found in " + this.parentRoom);
          }

          return playerId;
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

     public void onRoomResize() {
          Boolean[] newPlayerSlots = new Boolean[this.parentRoom.getMaxUsers() + 1];
          synchronized(this.playerSlots) {
               int i = 1;

               while(true) {
                    if (i >= newPlayerSlots.length) {
                         break;
                    }

                    if (i < this.playerSlots.length) {
                         newPlayerSlots[i] = this.playerSlots[i];
                    } else {
                         newPlayerSlots[i] = Boolean.FALSE;
                    }

                    ++i;
               }
          }

          this.playerSlots = newPlayerSlots;
     }

     public Room getParentRoom() {
          return this.parentRoom;
     }

     public void setParentRoom(Room room) {
          this.parentRoom = room;
     }

     public boolean takeSlot(int playerId) {
          if (playerId == -1) {
               return false;
          } else if (playerId >= this.playerSlots.length) {
               return false;
          } else {
               synchronized(this.playerSlots) {
                    if (this.playerSlots[playerId]) {
                         return false;
                    } else {
                         this.playerSlots[playerId] = Boolean.TRUE;
                         return true;
                    }
               }
          }
     }
}
